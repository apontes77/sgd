package br.com.sgd.frequencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.*;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.sgd.audit.AuditLogRepository;
import br.com.sgd.organizacao.Discipulado;

@ExtendWith(MockitoExtension.class)
class FechamentoFrequenciaJobTest {
  private static final LocalDate SEXTA = LocalDate.of(2026, 7, 17);

  @Mock EncontroRepository encontros;
  @Mock FrequenciaRepository frequencias;
  @Mock AuditLogRepository auditoria;
  @Mock Discipulado discipulado;
  private FechamentoFrequenciaJob job;

  @BeforeEach
  void setup() {
    Clock clock =
        Clock.fixed(
            LocalDateTime.of(2026, 7, 20, 3, 20).atZone(PrazoLancamentoFrequencia.ZONE).toInstant(),
            ZoneOffset.UTC);
    job = new FechamentoFrequenciaJob(encontros, frequencias, auditoria, new ObjectMapper(), clock);
    when(discipulado.getId()).thenReturn(10L);
    when(encontros.findAtivosPendentesDeLancamento(any(), eq(SituacaoEncontro.NAO_REALIZADO)))
        .thenAnswer(
            inv ->
                SEXTA.equals(inv.getArgument(0)) ? List.of(discipulado) : List.<Discipulado>of());
  }

  @Test
  void criaNaoRealizadoQuandoNaoHaEncontro() {
    when(encontros.findByDiscipuladoIdAndData(10L, SEXTA)).thenReturn(Optional.empty());
    when(encontros.save(any())).thenAnswer(i -> withId(i.getArgument(0), 99L));

    assertThat(job.executar()).isEqualTo(1);

    ArgumentCaptor<Encontro> captor = ArgumentCaptor.forClass(Encontro.class);
    verify(encontros).save(captor.capture());
    assertThat(captor.getValue().getSituacao()).isEqualTo(SituacaoEncontro.NAO_REALIZADO);
    assertThat(captor.getValue().getJustificativa())
        .isEqualTo(PrazoLancamentoFrequencia.JUSTIFICATIVA_AUTOMATICA);
    assertThat(captor.getValue().isFechamentoAutomatico()).isTrue();
    verify(auditoria).save(any());
  }

  @Test
  void converteRealizadoSemChamada() {
    Encontro orfao =
        new Encontro(
            discipulado, SEXTA, SituacaoEncontro.REALIZADO, Instant.parse("2026-07-17T20:00:00Z"));
    withId(orfao, 5L);
    when(encontros.findByDiscipuladoIdAndData(10L, SEXTA)).thenReturn(Optional.of(orfao));
    when(frequencias.existsByEncontroId(5L)).thenReturn(false);

    assertThat(job.executar()).isEqualTo(1);
    assertThat(orfao.getSituacao()).isEqualTo(SituacaoEncontro.NAO_REALIZADO);
    assertThat(orfao.getJustificativa())
        .isEqualTo(PrazoLancamentoFrequencia.JUSTIFICATIVA_AUTOMATICA);
    assertThat(orfao.isFechamentoAutomatico()).isTrue();
    verify(encontros, never()).save(any());
  }

  private static Encontro withId(Encontro encontro, long id) {
    try {
      var field = Encontro.class.getDeclaredField("id");
      field.setAccessible(true);
      field.set(encontro, id);
      return encontro;
    } catch (ReflectiveOperationException e) {
      throw new AssertionError(e);
    }
  }
}
