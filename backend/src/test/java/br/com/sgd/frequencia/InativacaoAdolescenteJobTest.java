package br.com.sgd.frequencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InativacaoAdolescenteJobTest {
  @Mock InativacaoAdolescenteRepository adolescentes;

  @Test
  void usaTresMesesAtrasNoFusoDeSaoPaulo() {
    Clock clock = Clock.fixed(Instant.parse("2026-07-17T02:30:00Z"), ZoneId.of("UTC"));
    LocalDate limite = LocalDate.of(2026, 4, 16);
    Instant instanteLimite = limite.atStartOfDay(ZoneId.of("America/Sao_Paulo")).toInstant();
    when(adolescentes.inativarSemParticipacao(instanteLimite, limite)).thenReturn(4);

    assertThat(new InativacaoAdolescenteJob(adolescentes, clock).executar()).isEqualTo(4);
    verify(adolescentes).inativarSemParticipacao(instanteLimite, limite);
  }
}
