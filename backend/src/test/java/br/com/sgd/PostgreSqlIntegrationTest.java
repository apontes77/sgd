package br.com.sgd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import br.com.sgd.frequencia.AuditoriaConsultaRepository;
import br.com.sgd.frequencia.InativacaoAdolescenteRepository;
import br.com.sgd.painel.PainelAdminRepository;
import br.com.sgd.painel.PainelGerenciaRepository;
import br.com.sgd.painel.PainelLiderRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = {
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.flyway.enabled=true"
})
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
@Transactional
class PostgreSqlIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("sgd_test")
        .withUsername("sgd")
        .withPassword("sgd");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired Flyway flyway;
    @Autowired PainelAdminRepository painelAdmin;
    @Autowired PainelGerenciaRepository painelGerencia;
    @Autowired PainelLiderRepository painelLider;
    @Autowired AuditoriaConsultaRepository auditoria;
    @Autowired InativacaoAdolescenteRepository inativacao;

    @BeforeEach
    void limparDados() {
        jdbc.execute("truncate table auditoria, visitantes, frequencias, encontros, "
            + "vinculos_adolescente_discipulado, adolescentes, discipulado_co_lideres, "
            + "discipulados, gerencias, identidades_oauth, tokens_redefinicao_senha, "
            + "refresh_tokens, usuario_perfis, usuarios restart identity cascade");
    }

    @Test
    void aplicaTodasAsMigrationsEValidaSchemaComHibernate() {
        List<String> versoes = Arrays.stream(flyway.info().applied())
            .map(info -> info.getVersion().getVersion())
            .toList();

        assertThat(versoes).containsExactly("1", "2", "3", "4", "5", "6", "7", "8", "9");
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("9");
        assertThat(jdbc.queryForList(
            "select table_name from information_schema.tables where table_schema='public'",
            String.class
        )).contains("usuarios", "identidades_oauth", "gerencias", "discipulados",
            "adolescentes", "encontros", "frequencias", "visitantes", "auditoria");
    }

    @Test
    void executaConsultasNativasDosTresPaineis() {
        Cenario c = criarCenarioPainel();
        LocalDate inicio = LocalDate.of(2025, 1, 1);
        LocalDate fim = LocalDate.of(2025, 2, 28);

        assertThat(painelAdmin.frequenciasMensais(inicio, fim)).extracting(
            PainelAdminRepository.ContagemMensal::getReferencia,
            PainelAdminRepository.ContagemMensal::getPresentes,
            PainelAdminRepository.ContagemMensal::getAusentes
        ).containsExactly(tuple("2025-01", 1L, 1L), tuple("2025-02", 1L, 0L));
        assertThat(painelAdmin.visitantesMensais(inicio, fim))
            .extracting(PainelAdminRepository.VisitantesMensais::getVisitantes)
            .containsExactly(3L, 2L);
        assertThat(painelAdmin.encontrosRealizados(inicio, fim)).isEqualTo(2);
        assertThat(painelAdmin.porGerencia(inicio, fim)).hasSize(2);
        assertThat(painelAdmin.porSexo(inicio, fim)).hasSize(2);

        assertThat(painelGerencia.frequenciasMensais(c.gerencia(), inicio, fim))
            .singleElement().satisfies(item -> {
                assertThat(item.getReferencia()).isEqualTo("2025-01");
                assertThat(item.getPresentes()).isEqualTo(1);
                assertThat(item.getAusentes()).isEqualTo(1);
            });
        assertThat(painelGerencia.visitantesPorDiscipulado(c.gerencia(), inicio, fim))
            .singleElement().satisfies(item -> {
                assertThat(item.getDiscipuladoId()).isEqualTo(c.discipulado());
                assertThat(item.getVisitantes()).isEqualTo(3);
            });
        assertThat(painelGerencia.encontrosRealizados(c.gerencia(), inicio, fim)).isEqualTo(1);

        assertThat(painelLider.frequenciasMensais(c.discipulado(), inicio, fim))
            .singleElement().satisfies(item -> {
                assertThat(item.getPresentes()).isEqualTo(1);
                assertThat(item.getAusentes()).isEqualTo(1);
            });
        assertThat(painelLider.visitantesMensais(c.discipulado(), inicio, fim))
            .singleElement().satisfies(item -> assertThat(item.getVisitantes()).isEqualTo(3));
        assertThat(painelLider.encontrosRealizados(c.discipulado(), inicio, fim)).isEqualTo(1);
    }

    @Test
    void filtraEPaginaAuditoriaNativa() {
        long usuarioUm = usuario("Gerente", "gerente@teste.local");
        long usuarioDois = usuario("Lider", "lider@teste.local");
        auditoria(usuarioUm, "ENCONTRO", "CRIAR", "{\"id\":1}", "2025-01-01T10:00:00Z");
        auditoria(usuarioUm, "ENCONTRO", "ATUALIZAR", "{\"id\":1}", "2025-01-02T10:00:00Z");
        auditoria(usuarioDois, "ADOLESCENTE", "CRIAR", "{\"id\":2}", "2025-01-03T10:00:00Z");

        var pagina = auditoria.consultar(
            "ENCONTRO", usuarioUm, PageRequest.of(0, 1, Sort.by("id"))
        );

        assertThat(pagina.getTotalElements()).isEqualTo(2);
        assertThat(pagina.getTotalPages()).isEqualTo(2);
        assertThat(pagina.getContent()).singleElement().satisfies(linha -> {
            assertThat(linha.getUsuarioId()).isEqualTo(usuarioUm);
            assertThat(linha.getEntidade()).isEqualTo("ENCONTRO");
            assertThat(linha.getAcao()).isEqualTo("CRIAR");
            assertThat(linha.getDetalhes()).isEqualTo("{\"id\":1}");
            assertThat(linha.getDataHora()).isEqualTo(Instant.parse("2025-01-01T10:00:00Z"));
        });
        assertThat(auditoria.consultar(null, usuarioDois, PageRequest.of(0, 10)).getTotalElements())
            .isEqualTo(1);
    }

    @Test
    void inativaSomenteAdolescenteAntigoSemPresencaRecente() {
        long responsavel = usuario("Gerente", "inativacao@teste.local");
        long gerencia = gerencia("Gerencia", responsavel);
        long discipulado = discipulado("Discipulado", "MASCULINO", gerencia, responsavel);
        long semPresenca = adolescente("Sem presenca", "2008-01-01", "2023-01-01T00:00:00Z");
        long comPresenca = adolescente("Com presenca", "2008-02-01", "2023-01-01T00:00:00Z");
        long recente = adolescente("Cadastro recente", "2008-03-01", "2025-06-01T00:00:00Z");
        long encontro = encontro(discipulado, "2025-06-15", "REALIZADO");
        frequencia(encontro, comPresenca, "PRESENTE");

        int alterados = inativacao.inativarSemParticipacao(
            Instant.parse("2025-01-01T00:00:00Z"), LocalDate.of(2025, 1, 1)
        );

        assertThat(alterados).isEqualTo(1);
        assertThat(ativo(semPresenca)).isFalse();
        assertThat(ativo(comPresenca)).isTrue();
        assertThat(ativo(recente)).isTrue();
    }

    private Cenario criarCenarioPainel() {
        long responsavel = usuario("Responsavel", "responsavel@teste.local");
        long gerenciaUm = gerencia("Gerencia Norte", responsavel);
        long gerenciaDois = gerencia("Gerencia Sul", responsavel);
        long discipuladoUm = discipulado("Aguias", "MASCULINO", gerenciaUm, responsavel);
        long discipuladoDois = discipulado("Flores", "FEMININO", gerenciaDois, responsavel);
        long adolescenteUm = adolescente("Adolescente 1", "2008-01-01", "2024-01-01T00:00:00Z");
        long adolescenteDois = adolescente("Adolescente 2", "2008-02-01", "2024-01-01T00:00:00Z");
        long janeiro = encontro(discipuladoUm, "2025-01-12", "REALIZADO");
        frequencia(janeiro, adolescenteUm, "PRESENTE");
        frequencia(janeiro, adolescenteDois, "AUSENTE");
        visitantes(janeiro, 3);
        long fevereiro = encontro(discipuladoDois, "2025-02-09", "REALIZADO");
        frequencia(fevereiro, adolescenteUm, "PRESENTE");
        visitantes(fevereiro, 2);
        long cancelado = encontro(discipuladoUm, "2025-01-19", "CANCELADO");
        frequencia(cancelado, adolescenteUm, "PRESENTE");
        visitantes(cancelado, 9);
        return new Cenario(gerenciaUm, discipuladoUm);
    }

    private long usuario(String nome, String email) {
        return jdbc.queryForObject(
            "insert into usuarios(nome,email,senha_hash) values (?,?,?) returning id",
            Long.class, nome, email, "hash");
    }

    private long gerencia(String nome, long usuario) {
        return jdbc.queryForObject(
            "insert into gerencias(nome,gerente_id) values (?,?) returning id",
            Long.class, nome, usuario);
    }

    private long discipulado(String nome, String sexo, long gerencia, long usuario) {
        return jdbc.queryForObject(
            "insert into discipulados(nome,sexo,gerencia_id,discipulador_id) values (?,?,?,?) returning id",
            Long.class, nome, sexo, gerencia, usuario);
    }

    private long adolescente(String nome, String nascimento, String criadoEm) {
        return jdbc.queryForObject(
            "insert into adolescentes(nome,data_nascimento,criado_em,atualizado_em) "
                + "values (?,cast(? as date),cast(? as timestamptz),cast(? as timestamptz)) returning id",
            Long.class, nome, nascimento, criadoEm, criadoEm);
    }

    private long encontro(long discipulado, String data, String situacao) {
        String justificativa = "CANCELADO".equals(situacao) ? "Encontro nao realizado" : null;
        return jdbc.queryForObject(
            "insert into encontros(discipulado_id,data,situacao,justificativa) "
                + "values (?,cast(? as date),?,?) returning id",
            Long.class, discipulado, data, situacao, justificativa);
    }

    private void frequencia(long encontro, long adolescente, String situacao) {
        jdbc.update("insert into frequencias(encontro_id,adolescente_id,situacao) values (?,?,?)",
            encontro, adolescente, situacao);
    }

    private void visitantes(long encontro, int quantidade) {
        jdbc.update("insert into visitantes(encontro_id,quantidade) values (?,?)",
            encontro, quantidade);
    }

    private void auditoria(long usuario, String entidade, String acao, String detalhes, String instante) {
        jdbc.update("insert into auditoria(usuario_id,data_hora,entidade,acao,detalhes) "
                + "values (?,cast(? as timestamptz),?,?,?)",
            usuario, instante, entidade, acao, detalhes);
    }

    private boolean ativo(long adolescente) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
            "select ativo from adolescentes where id=?", Boolean.class, adolescente));
    }

    private record Cenario(long gerencia, long discipulado) { }
}
