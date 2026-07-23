package br.com.sgd.frequencia;

import java.time.*;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InativacaoAdolescenteJob {
  private final InativacaoAdolescenteRepository adolescentes;
  private final Clock clock;

  public InativacaoAdolescenteJob(InativacaoAdolescenteRepository a, Clock c) {
    adolescentes = a;
    clock = c;
  }

  @Scheduled(cron = "0 15 3 * * *", zone = "America/Sao_Paulo")
  @Transactional
  public int executar() {
    var hoje = LocalDate.now(clock.withZone(ZoneId.of("America/Sao_Paulo")));
    var limite = hoje.minusMonths(3);
    return adolescentes.inativarSemParticipacao(
        limite.atStartOfDay(ZoneId.of("America/Sao_Paulo")).toInstant(), limite);
  }
}
