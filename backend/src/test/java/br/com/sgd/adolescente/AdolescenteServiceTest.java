package br.com.sgd.adolescente;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import br.com.sgd.audit.AuditLogRepository;
import br.com.sgd.frequencia.FrequenciaRepository;
import br.com.sgd.organizacao.Discipulado;
import br.com.sgd.organizacao.DiscipuladoRepository;
import br.com.sgd.organizacao.Gerencia;
import br.com.sgd.user.User;

@ExtendWith(MockitoExtension.class)
class AdolescenteServiceTest {
  private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");
  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-07-26T15:00:00Z"), ZoneId.of("UTC"));

  @Mock AdolescenteRepository adolescentes;
  @Mock VinculoAdolescenteRepository vinculos;
  @Mock DiscipuladoRepository discipulados;
  @Mock EscopoOrganizacionalService escopo;
  @Mock AuditLogRepository auditoria;
  @Mock FrequenciaRepository frequencias;
  @Mock User usuario;
  @Mock Discipulado origem;
  @Mock Discipulado destino;
  @Mock Gerencia gerencia;
  private AdolescenteService service;

  @BeforeEach
  void setup() {
    service =
        new AdolescenteService(
            adolescentes, vinculos, discipulados, escopo, auditoria, frequencias, CLOCK);
  }

  @Test
  void criaAdolescenteEVinculoInicialNoMesmoFluxo() {
    configurarAtivo(origem);
    when(discipulados.findById(10L)).thenReturn(Optional.of(origem));
    when(adolescentes.save(any())).thenAnswer(i -> i.getArgument(0));
    when(vinculos.save(any())).thenAnswer(i -> i.getArgument(0));
    var dados = dadosBasicos(10L, CategoriaAdolescente.DISCIPULO, null);

    Adolescente criado = service.criar(usuario, dados);

    assertThat(criado.getNome()).isEqualTo("Ana");
    assertThat(criado.getCategoria()).isEqualTo(CategoriaAdolescente.DISCIPULO);
    var captor = ArgumentCaptor.forClass(VinculoAdolescenteDiscipulado.class);
    verify(vinculos).save(captor.capture());
    assertThat(captor.getValue().getAdolescente()).isSameAs(criado);
    assertThat(captor.getValue().getDiscipulado()).isSameAs(origem);
    verify(escopo).exigirAlteracao(usuario, origem);
  }

  @Test
  void criaVisitanteComCategoriaSelecionada() {
    configurarAtivo(origem);
    when(discipulados.findById(10L)).thenReturn(Optional.of(origem));
    when(adolescentes.save(any())).thenAnswer(i -> i.getArgument(0));
    when(vinculos.save(any())).thenAnswer(i -> i.getArgument(0));

    Adolescente criado =
        service.criar(usuario, dadosBasicos(10L, CategoriaAdolescente.VISITANTE, null));

    assertThat(criado.getCategoria()).isEqualTo(CategoriaAdolescente.VISITANTE);
    assertThat(criado.getMotivoAfastamento()).isNull();
  }

  @Test
  void criaDiscipuloGoeExigeMotivo() {
    configurarAtivo(origem);
    when(discipulados.findById(10L)).thenReturn(Optional.of(origem));

    assertThatThrownBy(
            () ->
                service.criar(usuario, dadosBasicos(10L, CategoriaAdolescente.DISCIPULO_GOE, null)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("motivo do afastamento");
  }

  @Test
  void criaDiscipuloGoeComMotivo() {
    configurarAtivo(origem);
    when(discipulados.findById(10L)).thenReturn(Optional.of(origem));
    when(adolescentes.save(any())).thenAnswer(i -> i.getArgument(0));
    when(vinculos.save(any())).thenAnswer(i -> i.getArgument(0));

    Adolescente criado =
        service.criar(
            usuario, dadosBasicos(10L, CategoriaAdolescente.DISCIPULO_GOE, "Mudou de cidade"));

    assertThat(criado.getCategoria()).isEqualTo(CategoriaAdolescente.DISCIPULO_GOE);
    assertThat(criado.getMotivoAfastamento()).isEqualTo("Mudou de cidade");
  }

  @Test
  void usaDataInicioInformadaAoRegistrarVisitanteEmDataAnterior() {
    configurarAtivo(origem);
    when(discipulados.findById(10L)).thenReturn(Optional.of(origem));
    when(adolescentes.save(any())).thenAnswer(i -> i.getArgument(0));
    when(vinculos.save(any())).thenAnswer(i -> i.getArgument(0));
    var dados =
        new AdolescenteService.DadosAdolescente(
            "Bia",
            LocalDate.of(2011, 5, 4),
            null,
            null,
            null,
            null,
            LocalDate.of(2026, 1, 1),
            CategoriaAdolescente.VISITANTE,
            null,
            null,
            "Pai da Bia",
            "(11) 97777-0000",
            null,
            null,
            10L,
            true,
            LocalDate.of(2026, 3, 1));

    service.criar(usuario, dados);

    var captor = ArgumentCaptor.forClass(VinculoAdolescenteDiscipulado.class);
    verify(vinculos).save(captor.capture());
    assertThat(captor.getValue().getDataInicio()).isEqualTo(LocalDate.of(2026, 3, 1));
  }

  @Test
  void transfereEncerrandoAnteriorSemApagarHistorico() {
    Adolescente adolescente = adolescenteComResponsavel();
    var anterior = new VinculoAdolescenteDiscipulado(adolescente, origem, LocalDate.of(2026, 1, 1));
    configurarAtivo(destino);
    when(adolescentes.findById(1L)).thenReturn(Optional.of(adolescente));
    when(vinculos.findByAdolescenteIdAndAtivoTrue(1L)).thenReturn(Optional.of(anterior));
    when(discipulados.findById(20L)).thenReturn(Optional.of(destino));
    when(origem.getId()).thenReturn(10L);
    when(vinculos.save(any())).thenAnswer(i -> i.getArgument(0));

    var novo = service.transferir(usuario, 1L, 20L, LocalDate.of(2026, 2, 1));

    assertThat(anterior.isAtivo()).isFalse();
    assertThat(anterior.getDataFim()).isEqualTo(LocalDate.of(2026, 1, 31));
    assertThat(novo.getDataInicio()).isEqualTo(LocalDate.of(2026, 2, 1));
    assertThat(novo.getDiscipulado()).isSameAs(destino);
    verify(escopo).exigirAlteracao(usuario, origem);
    verify(escopo).exigirAlteracao(usuario, destino);
  }

  @Test
  void rejeitaTransferenciaNaDataInicialOuParaOMesmoDiscipulado() {
    Adolescente adolescente = adolescenteComResponsavel();
    var anterior = new VinculoAdolescenteDiscipulado(adolescente, origem, LocalDate.of(2026, 1, 1));
    configurarAtivo(destino);
    when(adolescentes.findById(1L)).thenReturn(Optional.of(adolescente));
    when(vinculos.findByAdolescenteIdAndAtivoTrue(1L)).thenReturn(Optional.of(anterior));
    when(discipulados.findById(20L)).thenReturn(Optional.of(destino));
    when(origem.getId()).thenReturn(10L);

    assertThatThrownBy(() -> service.transferir(usuario, 1L, 20L, LocalDate.of(2026, 1, 1)))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("após o início");
    assertThat(anterior.isAtivo()).isTrue();
  }

  @Test
  void atualizarNaoPermiteTrocarDiscipuladoSemEndpointDeTransferencia() {
    Adolescente adolescente = adolescenteComResponsavel();
    var atual = new VinculoAdolescenteDiscipulado(adolescente, origem, LocalDate.of(2026, 1, 1));
    when(adolescentes.findById(1L)).thenReturn(Optional.of(adolescente));
    when(vinculos.findByAdolescenteIdAndAtivoTrue(1L)).thenReturn(Optional.of(atual));
    when(origem.getId()).thenReturn(10L);
    var dados = dadosBasicos(20L, CategoriaAdolescente.DISCIPULO, null);

    assertThatThrownBy(() -> service.atualizar(usuario, 1L, dados))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("endpoint de vínculos");
  }

  @Test
  void listarAlertasGoeConsultaJanelaDeSeisSemanas() {
    when(discipulados.findById(10L)).thenReturn(Optional.of(origem));
    when(frequencias.encontrarPotenciaisGoe(
            eq(10L), eq(LocalDate.of(2026, 6, 15)), eq(LocalDate.of(2026, 7, 26)), eq(4L)))
        .thenReturn(List.of(row(1L, "Ana", 4L)));

    var alertas = service.listarAlertasGoe(usuario, 10L);

    assertThat(alertas).containsExactly(new AdolescenteService.AlertaGoe(1L, "Ana", 4L));
    verify(escopo).exigirLeitura(usuario, origem);
    LocalDate hoje = LocalDate.now(CLOCK.withZone(ZONA));
    assertThat(hoje).isEqualTo(LocalDate.of(2026, 7, 26));
  }

  @Test
  void promoveVisitanteComTresPresencasNaJanelaDoPrimeiroVinculo() {
    Adolescente visitante = adolescenteNaCategoria(CategoriaAdolescente.VISITANTE);
    var primeiro = new VinculoAdolescenteDiscipulado(visitante, origem, LocalDate.of(2026, 6, 1));
    when(adolescentes.findById(1L)).thenReturn(Optional.of(visitante));
    when(vinculos.findAllByAdolescenteIdOrderByDataInicioAsc(1L)).thenReturn(List.of(primeiro));
    when(frequencias.contarPresencasRealizadas(
            1L, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 7, 5)))
        .thenReturn(3L);

    service.promoverVisitanteSeElegivel(usuario, 1L);

    assertThat(visitante.getCategoria()).isEqualTo(CategoriaAdolescente.DISCIPULO);
    verify(auditoria).save(any());
  }

  @Test
  void naoPromoveVisitanteComMenosDeTresPresencas() {
    Adolescente visitante = adolescenteNaCategoria(CategoriaAdolescente.VISITANTE);
    var vinculo = new VinculoAdolescenteDiscipulado(visitante, origem, LocalDate.of(2026, 6, 1));
    when(adolescentes.findById(1L)).thenReturn(Optional.of(visitante));
    when(vinculos.findAllByAdolescenteIdOrderByDataInicioAsc(1L)).thenReturn(List.of(vinculo));
    when(frequencias.contarPresencasRealizadas(
            1L, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 7, 5)))
        .thenReturn(2L);

    service.promoverVisitanteSeElegivel(usuario, 1L);

    assertThat(visitante.getCategoria()).isEqualTo(CategoriaAdolescente.VISITANTE);
    verify(auditoria, never()).save(any());
  }

  @Test
  void naoPromoveQuandoJaEDiscipuloOuInativo() {
    Adolescente discipulo = adolescenteNaCategoria(CategoriaAdolescente.DISCIPULO);
    when(adolescentes.findById(1L)).thenReturn(Optional.of(discipulo));
    service.promoverVisitanteSeElegivel(usuario, 1L);
    verify(frequencias, never()).contarPresencasRealizadas(eq(1L), any(), any());

    Adolescente inativo = adolescenteNaCategoria(CategoriaAdolescente.VISITANTE);
    inativo.atualizar(
        new DadosCadastroAdolescente(
            "Ana",
            LocalDate.of(2010, 3, 2),
            null,
            null,
            LocalDate.of(2026, 1, 1),
            CategoriaAdolescente.VISITANTE,
            null,
            null,
            ContatosAdolescente.de(null, null, null, null, "Mãe da Ana", "(11) 98888-0000")),
        false);
    when(adolescentes.findById(2L)).thenReturn(Optional.of(inativo));
    service.promoverVisitanteSeElegivel(usuario, 2L);
    verify(vinculos, never()).findAllByAdolescenteIdOrderByDataInicioAsc(2L);
  }

  @Test
  void usaDataInicioDoPrimeiroVinculoComoAncoraDaJanela() {
    Adolescente visitante = adolescenteNaCategoria(CategoriaAdolescente.VISITANTE);
    var antigo = new VinculoAdolescenteDiscipulado(visitante, origem, LocalDate.of(2026, 5, 1));
    var atual = new VinculoAdolescenteDiscipulado(visitante, destino, LocalDate.of(2026, 6, 15));
    when(adolescentes.findById(1L)).thenReturn(Optional.of(visitante));
    when(vinculos.findAllByAdolescenteIdOrderByDataInicioAsc(1L))
        .thenReturn(List.of(antigo, atual));
    when(frequencias.contarPresencasRealizadas(
            1L, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 6, 4)))
        .thenReturn(3L);

    service.promoverVisitanteSeElegivel(usuario, 1L);

    verify(frequencias)
        .contarPresencasRealizadas(1L, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 6, 4));
    assertThat(visitante.getCategoria()).isEqualTo(CategoriaAdolescente.DISCIPULO);
  }

  @Test
  void anonimizaRemovendoDadosPessoaisERegistraAuditoria() {
    Adolescente adolescente =
        new Adolescente(
            new DadosCadastroAdolescente(
                "Ana",
                LocalDate.of(2010, 3, 2),
                "(11) 99999-0000",
                "@ana",
                LocalDate.of(2026, 1, 1),
                CategoriaAdolescente.DISCIPULO_GOE,
                "Núcleo A",
                "Mudou de cidade",
                ContatosAdolescente.de(
                    "Mãe da Ana", "(11) 98888-0000", "Pai da Ana", "(11) 97777-0000", null, null)),
            true);
    when(adolescentes.findById(1L)).thenReturn(Optional.of(adolescente));

    service.anonimizar(usuario, 1L);

    assertThat(adolescente.getNome()).isEqualTo("Adolescente anonimizado");
    assertThat(adolescente.getTelefone()).isNull();
    assertThat(adolescente.getInstagram()).isNull();
    assertThat(adolescente.getResponsavelNome()).isNull();
    assertThat(adolescente.getResponsavelTelefone()).isNull();
    assertThat(adolescente.getTelefoneMae()).isNull();
    assertThat(adolescente.getTelefonePai()).isNull();
    assertThat(adolescente.getEstrutura()).isNull();
    assertThat(adolescente.getMotivoAfastamento()).isNull();
    assertThat(adolescente.getCategoria()).isEqualTo(CategoriaAdolescente.DISCIPULO);
    assertThat(adolescente.isAtivo()).isFalse();
    assertThat(adolescente.isAnonimizado()).isTrue();
    verify(auditoria).save(any());
  }

  private static Adolescente adolescenteComResponsavel() {
    return adolescenteNaCategoria(CategoriaAdolescente.DISCIPULO);
  }

  private static Adolescente adolescenteNaCategoria(CategoriaAdolescente categoria) {
    return new Adolescente(
        new DadosCadastroAdolescente(
            "Ana",
            LocalDate.of(2010, 3, 2),
            null,
            null,
            LocalDate.of(2026, 1, 1),
            categoria,
            null,
            null,
            ContatosAdolescente.de(null, null, null, null, "Mãe da Ana", "(11) 98888-0000")),
        true);
  }

  private AdolescenteService.DadosAdolescente dadosBasicos(
      long discipuladoId, CategoriaAdolescente categoria, String motivo) {
    return new AdolescenteService.DadosAdolescente(
        " Ana ",
        LocalDate.of(2010, 3, 2),
        null,
        "@ana",
        null,
        null,
        LocalDate.of(2026, 1, 1),
        categoria,
        "Mãe da Ana",
        "(11) 91111-0000",
        null,
        null,
        "Núcleo A",
        motivo,
        discipuladoId,
        true,
        null);
  }

  private static FrequenciaRepository.AlertaGoeRow row(long id, String nome, long faltas) {
    return new FrequenciaRepository.AlertaGoeRow() {
      @Override
      public Long getAdolescenteId() {
        return id;
      }

      @Override
      public String getNome() {
        return nome;
      }

      @Override
      public Long getFaltas() {
        return faltas;
      }
    };
  }

  private void configurarAtivo(Discipulado d) {
    when(d.isAtivo()).thenReturn(true);
    when(d.getGerencia()).thenReturn(gerencia);
    when(gerencia.isAtivo()).thenReturn(true);
  }
}
