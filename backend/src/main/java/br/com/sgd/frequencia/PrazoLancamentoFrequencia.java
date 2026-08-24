package br.com.sgd.frequencia;

import java.time.*;
import java.util.ArrayList;
import java.util.List;

public final class PrazoLancamentoFrequencia {
  public static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
  public static final String JUSTIFICATIVA_AUTOMATICA =
      "discipulador ou colider não registraram a frequência";
  public static final String AVISO_LANCAMENTO_PENDENTE =
      "O discipulador/co-líder não lançou a frequência no prazo.";
  private static final int LOOKBACK_SEMANAS_PADRAO = 4;

  private PrazoLancamentoFrequencia() {}

  public static boolean ehSexta(LocalDate data) {
    return data != null && data.getDayOfWeek() == DayOfWeek.FRIDAY;
  }

  /** Instant exclusivo: segunda 00:00 America/Sao_Paulo após o domingo subsequente. */
  public static Instant limiteExclusivo(LocalDate dataEncontro) {
    if (!ehSexta(dataEncontro)) return null;
    LocalDate segunda = dataEncontro.plusDays(3);
    return segunda.atStartOfDay(ZONE).toInstant();
  }

  public static boolean estaDentroDoPrazo(LocalDate dataEncontro, Instant agora) {
    Instant limite = limiteExclusivo(dataEncontro);
    if (limite == null) return true;
    return agora.isBefore(limite);
  }

  public static List<LocalDate> sextasComPrazoEncerrado(LocalDate hoje, Instant agora) {
    return sextasComPrazoEncerrado(hoje, agora, LOOKBACK_SEMANAS_PADRAO);
  }

  public static List<LocalDate> sextasComPrazoEncerrado(
      LocalDate hoje, Instant agora, int lookbackSemanas) {
    List<LocalDate> vencidas = new ArrayList<>();
    LocalDate sexta = hoje;
    while (sexta.getDayOfWeek() != DayOfWeek.FRIDAY) sexta = sexta.minusDays(1);
    for (int i = 0; i < lookbackSemanas; i++) {
      LocalDate candidata = sexta.minusWeeks(i);
      if (!estaDentroDoPrazo(candidata, agora)) vencidas.add(candidata);
    }
    return vencidas;
  }
}
