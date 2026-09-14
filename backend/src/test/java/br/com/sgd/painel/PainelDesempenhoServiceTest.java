package br.com.sgd.painel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class PainelDesempenhoServiceTest {
  private PainelDesempenhoRepository repository;
  private PainelDesempenhoService service;
  private final LocalDate inicio = LocalDate.of(2026, 1, 1);
  private final LocalDate fim = LocalDate.of(2026, 3, 31);

  @BeforeEach
  void setup() {
    repository = mock(PainelDesempenhoRepository.class);
    service = new PainelDesempenhoService(repository);
    when(repository.listarDiscipulados()).thenReturn(List.of());
    when(repository.frequenciasMensais(inicio, fim)).thenReturn(List.of());
    when(repository.vinculosNoPeriodo(inicio, fim)).thenReturn(List.of());
  }

  @Test
  void retornaListaVaziaQuandoNaoHaDiscipulados() {
    var resposta = service.consultar(inicio, fim);
    assertThat(resposta.dataInicio()).isEqualTo(inicio);
    assertThat(resposta.dataFim()).isEqualTo(fim);
    assertThat(resposta.discipulados()).isEmpty();
  }

  @Test
  void montaSeriesDeFrequenciaEQuantidadePorDiscipulado() {
    var alpha = meta(1L, "Alpha", "MASCULINO", "DE_09_A_11", true, 10L, "Norte");
    var frequencia = mock(PainelDesempenhoRepository.FrequenciaMensal.class);
    when(frequencia.getDiscipuladoId()).thenReturn(1L);
    when(frequencia.getReferencia()).thenReturn("2026-02");
    when(frequencia.getPresentes()).thenReturn(4L);
    when(frequencia.getAusentes()).thenReturn(1L);
    var vinculo1 = vinculo(1L, 100L, LocalDate.of(2026, 1, 1), null);
    var vinculo2 = vinculo(1L, 101L, LocalDate.of(2026, 1, 15), LocalDate.of(2026, 2, 28));
    var vinculo3 = vinculo(1L, 102L, LocalDate.of(2026, 3, 1), null);
    when(repository.listarDiscipulados()).thenReturn(List.of(alpha));
    when(repository.frequenciasMensais(inicio, fim)).thenReturn(List.of(frequencia));
    when(repository.vinculosNoPeriodo(inicio, fim))
        .thenReturn(List.of(vinculo1, vinculo2, vinculo3));

    var item = service.consultar(inicio, fim).discipulados().getFirst();
    assertThat(item.nome()).isEqualTo("Alpha");
    assertThat(item.frequencia()).hasSize(1);
    assertThat(item.frequencia().getFirst().presentes()).isEqualTo(4);
    assertThat(item.frequencia().getFirst().ausentes()).isEqualTo(1);
    assertThat(item.discipulos())
        .extracting(PainelDesempenhoService.QuantidadeMensal::referencia)
        .containsExactly("2026-01", "2026-02", "2026-03");
    assertThat(item.discipulos())
        .extracting(PainelDesempenhoService.QuantidadeMensal::quantidade)
        .containsExactly(2L, 2L, 2L);
  }

  @Test
  void omiteInativoSemHistoricoEMantemAtivoSemDados() {
    var ativo = meta(1L, "Ativo", "FEMININO", "DE_15_MAIS", true, 10L, "Sul");
    var inativo = meta(2L, "Inativo", "MASCULINO", "DE_11_A_13", false, 10L, "Sul");
    when(repository.listarDiscipulados()).thenReturn(List.of(ativo, inativo));

    var resposta = service.consultar(inicio, fim);
    assertThat(resposta.discipulados())
        .extracting(PainelDesempenhoService.DiscipuladoDesempenho::nome)
        .containsExactly("Ativo");
  }

  @Test
  void incluiInativoComFrequenciaNoPeriodo() {
    var antigo = meta(2L, "Antigo", "MASCULINO", "DE_13_A_15", false, 8L, "Leste");
    var frequencia = mock(PainelDesempenhoRepository.FrequenciaMensal.class);
    when(frequencia.getDiscipuladoId()).thenReturn(2L);
    when(frequencia.getReferencia()).thenReturn("2026-01");
    when(frequencia.getPresentes()).thenReturn(1L);
    when(frequencia.getAusentes()).thenReturn(0L);
    when(repository.listarDiscipulados()).thenReturn(List.of(antigo));
    when(repository.frequenciasMensais(inicio, fim)).thenReturn(List.of(frequencia));

    assertThat(service.consultar(inicio, fim).discipulados())
        .extracting(PainelDesempenhoService.DiscipuladoDesempenho::nome)
        .containsExactly("Antigo");
  }

  @Test
  void rejeitaPeriodoInvalido() {
    assertThatThrownBy(() -> service.consultar(fim, inicio))
        .isInstanceOf(ResponseStatusException.class);
    assertThatThrownBy(() -> service.consultar(inicio, inicio.plusMonths(24).plusDays(1)))
        .isInstanceOf(ResponseStatusException.class);
  }

  private static PainelDesempenhoRepository.DiscipuladoMeta meta(
      Long id,
      String nome,
      String sexo,
      String faixa,
      boolean ativo,
      Long gerenciaId,
      String gerenciaNome) {
    var item = mock(PainelDesempenhoRepository.DiscipuladoMeta.class);
    when(item.getId()).thenReturn(id);
    when(item.getNome()).thenReturn(nome);
    when(item.getSexo()).thenReturn(sexo);
    when(item.getFaixaEtaria()).thenReturn(faixa);
    when(item.getAtivo()).thenReturn(ativo);
    when(item.getGerenciaId()).thenReturn(gerenciaId);
    when(item.getGerenciaNome()).thenReturn(gerenciaNome);
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
