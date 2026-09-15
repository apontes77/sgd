package br.com.sgd.painel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import br.com.sgd.organizacao.Gerencia;
import br.com.sgd.organizacao.GerenciaRepository;
import br.com.sgd.user.Role;
import br.com.sgd.user.User;

class PainelDesempenhoServiceTest {
  private PainelDesempenhoRepository repository;
  private GerenciaRepository gerencias;
  private PainelDesempenhoService service;
  private User admin;
  private final LocalDate inicio = LocalDate.of(2026, 1, 1);
  private final LocalDate fim = LocalDate.of(2026, 3, 31);

  @BeforeEach
  void setup() {
    repository = mock(PainelDesempenhoRepository.class);
    gerencias = mock(GerenciaRepository.class);
    service = new PainelDesempenhoService(repository, gerencias);
    admin = mock(User.class);
    when(admin.getPerfis()).thenReturn(Set.of(Role.ADMIN));
    when(repository.listarDiscipulados()).thenReturn(List.of());
    when(repository.frequenciasMensais(inicio, fim)).thenReturn(List.of());
    when(repository.vinculosNoPeriodo(inicio, fim)).thenReturn(List.of());
  }

  @Test
  void retornaListaVaziaQuandoNaoHaDiscipulados() {
    var resposta = service.consultar(admin, inicio, fim);
    assertThat(resposta.dataInicio()).isEqualTo(inicio);
    assertThat(resposta.dataFim()).isEqualTo(fim);
    assertThat(resposta.discipulados()).isEmpty();
  }

  @Test
  void montaSeriesDeFrequenciaComCategoriasEQuantidade() {
    var alpha = meta(1L, "Alpha", "MASCULINO", "DE_09_A_11", true, 10L, "Norte", "Líder A");
    var frequencia = mock(PainelDesempenhoRepository.FrequenciaMensal.class);
    when(frequencia.getDiscipuladoId()).thenReturn(1L);
    when(frequencia.getReferencia()).thenReturn("2026-02");
    when(frequencia.getPresentes()).thenReturn(6L);
    when(frequencia.getPresentesDiscipulos()).thenReturn(3L);
    when(frequencia.getPresentesVisitantes()).thenReturn(2L);
    when(frequencia.getPresentesGoe()).thenReturn(1L);
    when(frequencia.getAusentes()).thenReturn(1L);
    var vinculo1 = vinculo(1L, 100L, LocalDate.of(2026, 1, 1), null);
    var vinculo2 = vinculo(1L, 101L, LocalDate.of(2026, 1, 15), LocalDate.of(2026, 2, 28));
    var vinculo3 = vinculo(1L, 102L, LocalDate.of(2026, 3, 1), null);
    when(repository.listarDiscipulados()).thenReturn(List.of(alpha));
    when(repository.frequenciasMensais(inicio, fim)).thenReturn(List.of(frequencia));
    when(repository.vinculosNoPeriodo(inicio, fim))
        .thenReturn(List.of(vinculo1, vinculo2, vinculo3));

    var item = service.consultar(admin, inicio, fim).discipulados().getFirst();
    assertThat(item.nome()).isEqualTo("Alpha");
    assertThat(item.discipuladorNome()).isEqualTo("Líder A");
    assertThat(item.frequencia()).hasSize(1);
    assertThat(item.frequencia().getFirst().presentes()).isEqualTo(6);
    assertThat(item.frequencia().getFirst().presentesDiscipulos()).isEqualTo(3);
    assertThat(item.frequencia().getFirst().presentesVisitantes()).isEqualTo(2);
    assertThat(item.frequencia().getFirst().presentesGoe()).isEqualTo(1);
    assertThat(item.frequencia().getFirst().ausentes()).isEqualTo(1);
    assertThat(item.discipulos())
        .extracting(PainelDesempenhoService.QuantidadeMensal::quantidade)
        .containsExactly(2L, 2L, 2L);
  }

  @Test
  void omiteInativoSemHistoricoEMantemAtivoSemDados() {
    var ativo = meta(1L, "Ativo", "FEMININO", "DE_15_MAIS", true, 10L, "Sul", "Líder");
    var inativo = meta(2L, "Inativo", "MASCULINO", "DE_11_A_13", false, 10L, "Sul", "Líder");
    when(repository.listarDiscipulados()).thenReturn(List.of(ativo, inativo));

    var resposta = service.consultar(admin, inicio, fim);
    assertThat(resposta.discipulados())
        .extracting(PainelDesempenhoService.DiscipuladoDesempenho::nome)
        .containsExactly("Ativo");
  }

  @Test
  void incluiInativoComFrequenciaNoPeriodo() {
    var antigo = meta(2L, "Antigo", "MASCULINO", "DE_13_A_15", false, 8L, "Leste", "Líder");
    var frequencia = mock(PainelDesempenhoRepository.FrequenciaMensal.class);
    when(frequencia.getDiscipuladoId()).thenReturn(2L);
    when(frequencia.getReferencia()).thenReturn("2026-01");
    when(frequencia.getPresentes()).thenReturn(1L);
    when(frequencia.getPresentesDiscipulos()).thenReturn(1L);
    when(frequencia.getPresentesVisitantes()).thenReturn(0L);
    when(frequencia.getPresentesGoe()).thenReturn(0L);
    when(frequencia.getAusentes()).thenReturn(0L);
    when(repository.listarDiscipulados()).thenReturn(List.of(antigo));
    when(repository.frequenciasMensais(inicio, fim)).thenReturn(List.of(frequencia));

    assertThat(service.consultar(admin, inicio, fim).discipulados())
        .extracting(PainelDesempenhoService.DiscipuladoDesempenho::nome)
        .containsExactly("Antigo");
  }

  @Test
  void gerenteConsultaApenasSuaGerencia() {
    User gerente = mock(User.class);
    when(gerente.getId()).thenReturn(7L);
    when(gerente.getPerfis()).thenReturn(Set.of(Role.GERENTE));
    Gerencia gerencia = mock(Gerencia.class);
    when(gerencia.getId()).thenReturn(10L);
    when(gerencias.findAllByGerenteIdAndAtivoTrue(7L)).thenReturn(List.of(gerencia));
    var daGerencia = meta(1L, "Alpha", "MASCULINO", "DE_09_A_11", true, 10L, "Norte", "Líder");
    when(repository.listarDiscipuladosDaGerencia(10L)).thenReturn(List.of(daGerencia));
    when(repository.frequenciasMensaisDaGerencia(10L, inicio, fim)).thenReturn(List.of());
    when(repository.vinculosNoPeriodoDaGerencia(10L, inicio, fim)).thenReturn(List.of());

    var resposta = service.consultar(gerente, inicio, fim);
    assertThat(resposta.discipulados())
        .extracting(PainelDesempenhoService.DiscipuladoDesempenho::nome)
        .containsExactly("Alpha");
  }

  @Test
  void gerenteSemGerenciaRetorna404() {
    User gerente = mock(User.class);
    when(gerente.getId()).thenReturn(7L);
    when(gerente.getPerfis()).thenReturn(Set.of(Role.GERENTE));
    when(gerencias.findAllByGerenteIdAndAtivoTrue(7L)).thenReturn(List.of());
    assertThatThrownBy(() -> service.consultar(gerente, inicio, fim))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(404));
  }

  @Test
  void rejeitaPeriodoInvalido() {
    assertThatThrownBy(() -> service.consultar(admin, fim, inicio))
        .isInstanceOf(ResponseStatusException.class);
    assertThatThrownBy(() -> service.consultar(admin, inicio, inicio.plusMonths(24).plusDays(1)))
        .isInstanceOf(ResponseStatusException.class);
  }

  private static PainelDesempenhoRepository.DiscipuladoMeta meta(
      Long id,
      String nome,
      String sexo,
      String faixa,
      boolean ativo,
      Long gerenciaId,
      String gerenciaNome,
      String discipuladorNome) {
    var item = mock(PainelDesempenhoRepository.DiscipuladoMeta.class);
    when(item.getId()).thenReturn(id);
    when(item.getNome()).thenReturn(nome);
    when(item.getSexo()).thenReturn(sexo);
    when(item.getFaixaEtaria()).thenReturn(faixa);
    when(item.getAtivo()).thenReturn(ativo);
    when(item.getGerenciaId()).thenReturn(gerenciaId);
    when(item.getGerenciaNome()).thenReturn(gerenciaNome);
    when(item.getDiscipuladorNome()).thenReturn(discipuladorNome);
    return item;
  }

  private static PainelDesempenhoRepository.VinculoPeriodo vinculo(
      Long discipuladoId, Long adolescenteId, LocalDate dataInicio, LocalDate dataFim) {
    var item = mock(PainelDesempenhoRepository.VinculoPeriodo.class);
    when(item.getDiscipuladoId()).thenReturn(discipuladoId);
    when(item.getAdolescenteId()).thenReturn(adolescenteId);
    when(item.getDataInicio()).thenReturn(dataInicio);
    when(item.getDataFim()).thenReturn(dataFim);
    return item;
  }
}
