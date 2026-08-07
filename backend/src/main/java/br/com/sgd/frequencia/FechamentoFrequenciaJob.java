package br.com.sgd.frequencia;

import java.time.*;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.sgd.audit.AuditLog;
import br.com.sgd.audit.AuditLogRepository;
import br.com.sgd.organizacao.Discipulado;

@Component
public class FechamentoFrequenciaJob {
  private final EncontroRepository encontros;
  private final FrequenciaRepository frequencias;
  private final AuditLogRepository auditoria;
  private final ObjectMapper json;
  private final Clock clock;

  public FechamentoFrequenciaJob(
      EncontroRepository e, FrequenciaRepository f, AuditLogRepository a, ObjectMapper j, Clock c) {
    encontros = e;
    frequencias = f;
    auditoria = a;
    json = j;
    clock = c;
  }

  @Scheduled(cron = "0 20 3 * * *", zone = "America/Sao_Paulo")
  @Transactional
  public int executar() {
    var zona = PrazoLancamentoFrequencia.ZONE;
    var agora = clock.instant();
    var hoje = LocalDate.now(clock.withZone(zona));
    int fechados = 0;
    for (LocalDate sexta : PrazoLancamentoFrequencia.sextasComPrazoEncerrado(hoje, agora)) {
      for (Discipulado d :
          encontros.findAtivosPendentesDeLancamento(sexta, SituacaoEncontro.NAO_REALIZADO)) {
        var existente = encontros.findByDiscipuladoIdAndData(d.getId(), sexta);
        if (existente.isEmpty()) {
          var criado =
              encontros.save(
                  new Encontro(
                      d,
                      sexta,
                      SituacaoEncontro.NAO_REALIZADO,
                      PrazoLancamentoFrequencia.JUSTIFICATIVA_AUTOMATICA,
                      agora));
          auditar(criado, "CRIAR");
          fechados++;
          continue;
        }
        var encontro = existente.get();
        if (encontro.getSituacao() != SituacaoEncontro.REALIZADO
            || encontro.getChamadaSalvaEm() != null) continue;
        if (frequencias.existsByEncontroId(encontro.getId())) {
          encontro.marcarChamadaSalva(
              frequencias.findAllByEncontroIdOrderByAdolescenteNome(encontro.getId()).stream()
                  .map(Frequencia::getRegistradaEm)
                  .min(Instant::compareTo)
                  .orElse(agora));
          continue;
        }
        encontro.atualizar(
            null,
            SituacaoEncontro.NAO_REALIZADO,
            PrazoLancamentoFrequencia.JUSTIFICATIVA_AUTOMATICA,
            encontro.getObservacao(),
            agora);
        auditar(encontro, "CONVERTER");
        fechados++;
      }
    }
    return fechados;
  }

  private void auditar(Encontro encontro, String operacao) {
    try {
      Map<String, Object> detalhes = new LinkedHashMap<>();
      detalhes.put("operacao", operacao);
      detalhes.put("encontroId", encontro.getId());
      detalhes.put("discipuladoId", encontro.getDiscipulado().getId());
      detalhes.put("data", encontro.getData().toString());
      detalhes.put("justificativa", PrazoLancamentoFrequencia.JUSTIFICATIVA_AUTOMATICA);
      auditoria.save(
          new AuditLog(
              null, "ENCONTRO", "FECHAMENTO_AUTOMATICO", json.writeValueAsString(detalhes)));
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Falha ao registrar auditoria do fechamento automático.", ex);
    }
  }
}
