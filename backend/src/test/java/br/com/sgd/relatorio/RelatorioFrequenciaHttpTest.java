package br.com.sgd.relatorio;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.sgd.adolescente.Adolescente;
import br.com.sgd.adolescente.AdolescenteRepository;
import br.com.sgd.adolescente.VinculoAdolescenteDiscipulado;
import br.com.sgd.adolescente.VinculoAdolescenteRepository;
import br.com.sgd.frequencia.Encontro;
import br.com.sgd.frequencia.EncontroRepository;
import br.com.sgd.frequencia.Frequencia;
import br.com.sgd.frequencia.FrequenciaRepository;
import br.com.sgd.frequencia.SituacaoEncontro;
import br.com.sgd.frequencia.SituacaoFrequencia;
import br.com.sgd.frequencia.Visitante;
import br.com.sgd.frequencia.VisitanteRepository;
import br.com.sgd.organizacao.Discipulado;
import br.com.sgd.organizacao.DiscipuladoRepository;
import br.com.sgd.organizacao.Gerencia;
import br.com.sgd.organizacao.GerenciaRepository;
import br.com.sgd.organizacao.Sexo;
import br.com.sgd.user.Role;
import br.com.sgd.user.User;
import br.com.sgd.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RelatorioFrequenciaHttpTest {
    private static final String SENHA = "senha-inicial-segura";
    private static final LocalDate DATA = LocalDate.of(2026, 7, 21);
    private static final Instant AGORA = Instant.parse("2026-07-21T22:00:00Z");
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository usuarios;
    @Autowired GerenciaRepository gerencias;
    @Autowired DiscipuladoRepository discipulados;
    @Autowired AdolescenteRepository adolescentes;
    @Autowired EncontroRepository encontros;
    @Autowired FrequenciaRepository frequencias;
    @Autowired VisitanteRepository visitantes;
    @Autowired VinculoAdolescenteRepository vinculos;
    @Autowired PasswordEncoder passwords;

    private User admin;
    private User gerenteCentro;
    private User gerenteSemEscopo;
    private User liderAlpha;
    private User coLiderAlpha;
    private User perfilAcumulado;
    private Discipulado alpha;

    @BeforeEach
    void prepararDados() {
        String sufixo = UUID.randomUUID().toString();
        admin = usuario("Admin", "admin-" + sufixo, Role.ADMIN);
        gerenteCentro = usuario("Gerente Centro", "gerente-centro-" + sufixo, Role.GERENTE);
        gerenteSemEscopo = usuario("Gerente sem escopo", "sem-escopo-" + sufixo, Role.GERENTE);
        liderAlpha = usuario("Líder Alpha", "alpha-" + sufixo, Role.DISCIPULADOR);
        coLiderAlpha = usuario("Co-líder Alpha", "co-alpha-" + sufixo, Role.CO_LIDER);
        perfilAcumulado = usuario("Perfil acumulado", "acumulado-" + sufixo, Role.GERENTE, Role.CO_LIDER);
        User liderBeta = usuario("Líder Beta", "beta-" + sufixo, Role.DISCIPULADOR);
        User liderGamma = usuario("Líder Gamma", "gamma-" + sufixo, Role.DISCIPULADOR);
        Gerencia centro = gerencias.saveAndFlush(new Gerencia("Centro", gerenteCentro));
        Gerencia norte = gerencias.saveAndFlush(new Gerencia("Norte", perfilAcumulado));
        alpha = new Discipulado("Alpha", Sexo.MASCULINO, centro, liderAlpha);
        alpha.replaceCoLideres(Set.of(coLiderAlpha, perfilAcumulado));
        alpha = discipulados.saveAndFlush(alpha);
        Discipulado beta = discipulados.saveAndFlush(new Discipulado("Beta", Sexo.FEMININO, centro, liderBeta));
        Discipulado gamma = discipulados.saveAndFlush(new Discipulado("Gamma", Sexo.MASCULINO, norte, liderGamma));

        Encontro alphaPrincipal = encontro(alpha, SituacaoEncontro.REALIZADO);
        encontro(alpha, DATA.minusDays(1), SituacaoEncontro.REALIZADO);
        encontro(alpha, DATA.minusDays(2), SituacaoEncontro.NAO_REALIZADO);
        encontro(beta, SituacaoEncontro.REALIZADO);
        encontro(gamma, SituacaoEncontro.REALIZADO);
        Adolescente bia = adolescentes.saveAndFlush(new Adolescente("Bia", LocalDate.of(2010, 2, 1), "(11) 98888-2222", null));
        Adolescente ana = adolescentes.saveAndFlush(new Adolescente("Ana", LocalDate.of(2010, 1, 1), "(11) 97777-1111", null));
        frequencias.saveAndFlush(new Frequencia(alphaPrincipal, bia, SituacaoFrequencia.PRESENTE, AGORA));
        frequencias.saveAndFlush(new Frequencia(alphaPrincipal, ana, SituacaoFrequencia.AUSENTE, AGORA));
        vinculos.saveAndFlush(new VinculoAdolescenteDiscipulado(bia, alpha, DATA));
        visitantes.saveAndFlush(new Visitante(alphaPrincipal, 3, AGORA));
        ana.atualizar("Ana", LocalDate.of(2010, 1, 1), "(11) 97777-1111", null, false);
        adolescentes.saveAndFlush(ana);
    }
    @Test
    void listaDiscipuladosConformeEscopoCumulativoDoUsuario() throws Exception {
        mvc.perform(get("/api/v1/discipulados").param("size", "100")
                .header(HttpHeaders.AUTHORIZATION, bearer(token(liderAlpha))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].nome").value("Alpha"));
        mvc.perform(get("/api/v1/discipulados").param("size", "100")
                .header(HttpHeaders.AUTHORIZATION, bearer(token(coLiderAlpha))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1));
        mvc.perform(get("/api/v1/discipulados").param("size", "100")
                .header(HttpHeaders.AUTHORIZATION, bearer(token(gerenteCentro))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(2));
        mvc.perform(get("/api/v1/discipulados").param("size", "100")
                .header(HttpHeaders.AUTHORIZATION, bearer(token(perfilAcumulado))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(2));
        mvc.perform(get("/api/v1/discipulados").param("size", "100")
                .header(HttpHeaders.AUTHORIZATION, bearer(token(admin))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[?(@.nome == 'Alpha')]").isNotEmpty())
            .andExpect(jsonPath("$.content[?(@.nome == 'Beta')]").isNotEmpty())
            .andExpect(jsonPath("$.content[?(@.nome == 'Gamma')]").isNotEmpty())
            .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(3)));
    }


    @Test
    void aplicaEscopoPorPerfilEEntregaDetalhesOrdenados() throws Exception {
        String tokenLider = token(liderAlpha);
        String tokenCoLider = token(coLiderAlpha);
        String tokenGerente = token(gerenteCentro);
        String tokenAdmin = token(admin);

        consultar(tokenLider).andExpect(status().isOk()).andExpect(jsonPath("$.relatorios.length()").value(1))
            .andExpect(jsonPath("$.relatorios[0].discipulado.nome").value("Alpha"))
            .andExpect(jsonPath("$.relatorios[0].situacao").value("REALIZADO"))
            .andExpect(jsonPath("$.relatorios[0].participantes[0].nome").value("Ana"))
            .andExpect(jsonPath("$.relatorios[0].participantes[0].telefone").value("(11) 97777-1111"))
            .andExpect(jsonPath("$.relatorios[0].data").value(DATA.toString()))
            .andExpect(jsonPath("$.relatorios[0].participantes[0].situacao").value("AUSENTE"))
            .andExpect(jsonPath("$.relatorios[0].participantes[1].nome").value("Bia"))
            .andExpect(jsonPath("$.relatorios[0].visitantes").value(1))
            .andExpect(jsonPath("$.relatorios[0].resumo.presentes").value(1))
            .andExpect(jsonPath("$.relatorios[0].resumo.ausentes").value(1))
            .andExpect(jsonPath("$.relatorios[0].resumo.percentualPresenca").value(50.00))
            .andExpect(jsonPath("$.relatorios[0].coLideres[0].nome").value("Co-líder Alpha"));
        consultar(tokenCoLider).andExpect(status().isOk()).andExpect(jsonPath("$.relatorios.length()").value(1));
        consultar(tokenGerente).andExpect(status().isOk()).andExpect(jsonPath("$.relatorios.length()").value(2))
            .andExpect(jsonPath("$.relatorios[1].discipulado.nome").value("Beta"));
        consultar(token(perfilAcumulado)).andExpect(status().isOk()).andExpect(jsonPath("$.relatorios.length()").value(2))
            .andExpect(jsonPath("$.relatorios[0].discipulado.nome").value("Alpha"))
            .andExpect(jsonPath("$.relatorios[1].discipulado.nome").value("Gamma"));
        consultar(tokenAdmin).andExpect(status().isOk()).andExpect(jsonPath("$.relatorios.length()").value(3))
            .andExpect(jsonPath("$.relatorios[2].discipulado.nome").value("Gamma"));
    }

    @Test
    void incluiNaoRealizadoComJustificativaNoHistoricoDoPeriodo() throws Exception {
        String token = token(liderAlpha);
        mvc.perform(get("/api/v1/relatorios/frequencia").param("dataInicio", DATA.minusDays(2).toString())
                .param("dataFim", DATA.toString()).header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.relatorios.length()").value(3))
            .andExpect(jsonPath("$.relatorios[0].data").value(DATA.minusDays(2).toString()))
            .andExpect(jsonPath("$.relatorios[0].situacao").value("NAO_REALIZADO"))
            .andExpect(jsonPath("$.relatorios[0].justificativa").value("Imprevisto no discipulado"))
            .andExpect(jsonPath("$.relatorios[0].participantes.length()").value(0))
            .andExpect(jsonPath("$.relatorios[0].visitantes").value(0))
            .andExpect(jsonPath("$.relatorios[1].situacao").value("REALIZADO"))
            .andExpect(jsonPath("$.relatorios[2].situacao").value("REALIZADO"));
    }

    @Test
    void distingueAusenciaDeEscopoDeDataSemEncontros() throws Exception {
        consultar(token(gerenteSemEscopo)).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value("O usuário não possui escopo organizacional para relatórios de frequência."));
        mvc.perform(get("/api/v1/relatorios/frequencia-diaria").param("data", "2026-07-22")
                .header(HttpHeaders.AUTHORIZATION, bearer(token(admin))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.relatorios.length()").value(0));
    }

    @Test
    void consultaPeriodoDeAteDozeMesesEValidaLimites() throws Exception {
        String token = token(liderAlpha);
        mvc.perform(get("/api/v1/relatorios/frequencia").param("dataInicio", DATA.minusDays(2).toString())
                .param("dataFim", DATA.toString()).header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.dataInicio").value(DATA.minusDays(2).toString()))
            .andExpect(jsonPath("$.dataFim").value(DATA.toString()))
            .andExpect(jsonPath("$.relatorios.length()").value(3))
            .andExpect(jsonPath("$.relatorios[0].data").value(DATA.minusDays(2).toString()))
            .andExpect(jsonPath("$.relatorios[1].data").value(DATA.minusDays(1).toString()))
            .andExpect(jsonPath("$.relatorios[2].data").value(DATA.toString()));
        mvc.perform(get("/api/v1/relatorios/frequencia").param("dataInicio", DATA.toString())
                .param("dataFim", DATA.minusDays(1).toString()).header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/relatorios/frequencia").param("dataInicio", DATA.toString())
                .param("dataFim", DATA.plusMonths(12).toString()).header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk());
        mvc.perform(get("/api/v1/relatorios/frequencia").param("dataInicio", DATA.toString())
                .param("dataFim", DATA.plusMonths(12).plusDays(1).toString()).header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isBadRequest());
    }

    private org.springframework.test.web.servlet.ResultActions consultar(String token) throws Exception {
        return mvc.perform(get("/api/v1/relatorios/frequencia-diaria").param("data", DATA.toString())
                .header(HttpHeaders.AUTHORIZATION, bearer(token)));
    }

    private Encontro encontro(Discipulado discipulado, SituacaoEncontro situacao) {
        return encontro(discipulado, DATA, situacao);
    }

    private Encontro encontro(Discipulado discipulado, LocalDate data, SituacaoEncontro situacao) {
        String justificativa = situacao == SituacaoEncontro.NAO_REALIZADO ? "Imprevisto no discipulado" : null;
        return encontros.saveAndFlush(new Encontro(discipulado, data, situacao, justificativa, AGORA));
    }

    private User usuario(String nome, String prefixo, Role... perfis) {
        return usuarios.saveAndFlush(new User(nome, prefixo + "@sgd.local", passwords.encode(SENHA), Set.of(perfis)));
    }

    private String token(User usuario) throws Exception {
        String response = mvc.perform(post("/api/v1/autenticacao/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + usuario.getEmail() + "\",\"senha\":\"" + SENHA + "\"}"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("accessToken").asText();
    }

    private static String bearer(String token) { return "Bearer " + token; }
}
