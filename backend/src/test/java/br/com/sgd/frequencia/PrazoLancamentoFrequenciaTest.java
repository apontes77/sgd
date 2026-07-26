package br.com.sgd.frequencia;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.*;

import org.junit.jupiter.api.Test;

class PrazoLancamentoFrequenciaTest {
  @Test
  void reconheceSextaEIgnoraOutrosDias() {
    assertThat(PrazoLancamentoFrequencia.ehSexta(LocalDate.of(2026, 7, 17))).isTrue();
    assertThat(PrazoLancamentoFrequencia.ehSexta(LocalDate.of(2026, 7, 18))).isFalse();
  }

  @Test
  void prazoIncluiDomingoEExcluiSegunda() {
    LocalDate sexta = LocalDate.of(2026, 7, 17);
    Instant domingo235959 =
        LocalDateTime.of(2026, 7, 19, 23, 59, 59)
            .atZone(PrazoLancamentoFrequencia.ZONE)
            .toInstant();
    Instant segunda000000 =
        LocalDateTime.of(2026, 7, 20, 0, 0, 0).atZone(PrazoLancamentoFrequencia.ZONE).toInstant();

    assertThat(PrazoLancamentoFrequencia.estaDentroDoPrazo(sexta, domingo235959)).isTrue();
    assertThat(PrazoLancamentoFrequencia.estaDentroDoPrazo(sexta, segunda000000)).isFalse();
  }

  @Test
  void diasQueNaoSaoSextaNaoTemPrazoEspecial() {
    Instant segunda =
        LocalDateTime.of(2026, 7, 27, 12, 0).atZone(PrazoLancamentoFrequencia.ZONE).toInstant();
    assertThat(PrazoLancamentoFrequencia.estaDentroDoPrazo(LocalDate.of(2026, 7, 16), segunda))
        .isTrue();
  }

  @Test
  void listaSextasVencidasNoLookback() {
    LocalDate segunda = LocalDate.of(2026, 7, 20);
    Instant agora =
        LocalDateTime.of(2026, 7, 20, 3, 20).atZone(PrazoLancamentoFrequencia.ZONE).toInstant();
    assertThat(PrazoLancamentoFrequencia.sextasComPrazoEncerrado(segunda, agora))
        .containsExactly(
            LocalDate.of(2026, 7, 17),
            LocalDate.of(2026, 7, 10),
            LocalDate.of(2026, 7, 3),
            LocalDate.of(2026, 6, 26));
  }
}
