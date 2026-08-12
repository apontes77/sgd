package br.com.sgd.frequencia;

import java.time.*;
import java.util.*;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.sgd.adolescente.EscopoOrganizacionalService;
import br.com.sgd.audit.*;
import br.com.sgd.organizacao.*;
import br.com.sgd.user.Role;
import br.com.sgd.user.User;

@Service
@Transactional
public class EncontroService {
  private final EncontroRepository encontros;
  private final FrequenciaRepository frequencias;
  private final VisitanteRepository visitantes;
  private final VinculoHistoricoRepository vinculos;
  private final DiscipuladoRepository discipulados;
  private final EscopoOrganizacionalService escopo;
  private final AuditLogRepository auditoria;
  private final ObjectMapper json;
  private final Clock clock;

  public EncontroService(
      EncontroRepository e,
      FrequenciaRepository f,
      VisitanteRepository vi,
      VinculoHistoricoRepository vh,
      DiscipuladoRepository d,
      EscopoOrganizacionalService es,
      AuditLogRepository a,
      ObjectMapper j,
      Clock c) {
    encontros = e;
    frequencias = f;
    visitantes = vi;
    vinculos = vh;
    discipulados = d;
    escopo = es;
    auditoria = a;
    json = j;
    clock = c;
  }

  public Encontro criar(User ator, long discipuladoId, LocalDate data, SituacaoEncontro situacao) {
    return criar(ator, discipuladoId, data, situacao, null);
  }

  public Encontro criar(
      User ator,
      long discipuladoId,
      LocalDate data,
      SituacaoEncontro situacao,
      String justificativa) {
    var d = discipulado(discipuladoId);
    escopo.exigirAlteracao(ator, d);
    if (!d.isAtivo()) conflito("O discipulado está inativo.");
    exigirPrazoLancamento(ator, data);
    if (encontros.existsByDiscipuladoIdAndData(discipuladoId, data))
      conflito("Já existe um encontro registrado para este discipulado nesta data.");
    if (situacao == SituacaoEncontro.NAO_REALIZADO) exigirRegistroNaoRealizado(ator);
    String motivo = validarJustificativa(situacao, justificativa);
    var e = encontros.save(new Encontro(d, data, situacao, motivo, clock.instant()));
    var detalhes = new LinkedHashMap<String, Object>();
    detalhes.put("id", e.getId());
    detalhes.put("situacao", situacao);
    detalhes.put("justificativa", motivo);
    auditar(ator, "ENCONTRO", "CRIAR", detalhes);
    return e;
  }

  public Encontro atualizar(User ator, long id, LocalDate data, SituacaoEncontro situacao) {
    return atualizar(ator, id, data, situacao, null, null);
  }

  public Encontro atualizar(
      User ator, long id, LocalDate data, SituacaoEncontro situacao, String justificativa) {
    return atualizar(ator, id, data, situacao, justificativa, null);
  }

  public Encontro atualizar(
      User ator,
      long id,
      LocalDate data,
      SituacaoEncontro situacao,
      String justificativa,
      String observacao) {
    var e = encontro(id);
    escopo.exigirAlteracao(ator, e.getDiscipulado());
    var novaData = data == null ? e.getData() : data;
    exigirPrazoLancamento(ator, novaData);
    if (!novaData.equals(e.getData())) exigirPrazoLancamento(ator, e.getData());
    if (encontros.existsByDiscipuladoIdAndDataAndIdNot(e.getDiscipulado().getId(), novaData, id))
      conflito("Já existe um encontro registrado para este discipulado nesta data.");
    var novaSituacao = situacao == null ? e.getSituacao() : situacao;
    exigirPermissaoSituacao(ator, e.getSituacao(), novaSituacao);
    exigirSituacaoSemChamada(e.getSituacao(), novaSituacao, id);
    String motivo = resolverJustificativa(e, novaSituacao, justificativa);
    String novaObservacao = observacao == null ? e.getObservacao() : validarObservacao(observacao);
    var antes = estado(e);
    e.atualizar(data, novaSituacao, motivo, novaObservacao, clock.instant());
    auditar(ator, "ENCONTRO", "ALTERAR", Map.of("id", id, "anterior", antes, "novo", estado(e)));
    return e;
  }

  private void exigirPermissaoSituacao(
      User ator, SituacaoEncontro atual, SituacaoEncontro novaSituacao) {
    if (atual == SituacaoEncontro.NAO_REALIZADO && novaSituacao == SituacaoEncontro.REALIZADO)
      exigirReversaoAdministrativa(ator);
    else if (atual == SituacaoEncontro.NAO_REALIZADO
        || novaSituacao == SituacaoEncontro.NAO_REALIZADO) exigirRegistroNaoRealizado(ator);
  }

  private void exigirSituacaoSemChamada(
      SituacaoEncontro atual, SituacaoEncontro novaSituacao, long id) {
    if (atual == novaSituacao) return;
    if (frequencias.existsByEncontroId(id)
        || visitantes.findByEncontroId(id).map(v -> v.getQuantidade() > 0).orElse(false))
      conflito(
          "Um encontro com chamada ou visitantes registrados não pode ter sua situação alterada.");
  }

  private static String resolverJustificativa(
      Encontro e, SituacaoEncontro novaSituacao, String justificativa) {
    if (novaSituacao != SituacaoEncontro.NAO_REALIZADO)
      return validarJustificativa(novaSituacao, justificativa);
    return validarJustificativa(
        novaSituacao, justificativa == null ? e.getJustificativa() : justificativa);
  }

  @Transactional(readOnly = true)
  public List<Encontro> listar(User ator, long discipuladoId, LocalDate inicio, LocalDate fim) {
    var d = discipulado(discipuladoId);
    escopo.exigirLeitura(ator, d);
    return encontros.findAllByDiscipuladoIdAndDataBetweenOrderByDataDesc(
        discipuladoId,
        inicio == null ? LocalDate.of(1900, 1, 1) : inicio,
        fim == null ? LocalDate.of(2999, 12, 31) : fim);
  }

  @Transactional(readOnly = true)
  public Encontro encontro(long id) {
    return encontros
        .findById(id)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Encontro não encontrado."));
  }

  private Discipulado discipulado(long id) {
    return discipulados
        .findById(id)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Discipulado não encontrado."));
  }

  private static String validarJustificativa(SituacaoEncontro situacao, String justificativa) {
    String valor = justificativa == null ? null : justificativa.trim();
    if (situacao == SituacaoEncontro.NAO_REALIZADO && (valor == null || valor.isEmpty()))
      throw new IllegalArgumentException(
          "A justificativa é obrigatória para encontro não realizado.");
    if (valor != null && valor.length() > 500)
      throw new IllegalArgumentException("A justificativa deve ter no máximo 500 caracteres.");
    if (situacao == SituacaoEncontro.REALIZADO && valor != null && !valor.isEmpty())
      throw new IllegalArgumentException(
          "A justificativa só pode ser informada para encontro não realizado.");
    return situacao == SituacaoEncontro.NAO_REALIZADO ? valor : null;
  }

  private static String validarObservacao(String observacao) {
    String valor = observacao == null ? null : observacao.trim();
    if (valor != null && valor.isEmpty()) return null;
    if (valor != null && valor.length() > 500)
      throw new IllegalArgumentException("A observação deve ter no máximo 500 caracteres.");
    return valor;
  }

  private static void exigirRegistroNaoRealizado(User ator) {
    if (!ator.getPerfis().contains(Role.ADMIN) && !ator.getPerfis().contains(Role.DISCIPULADOR))
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN,
          "Somente administradores e discipuladores podem marcar um encontro como não realizado.");
  }

  private static void exigirReversaoAdministrativa(User ator) {
    if (!ator.getPerfis().contains(Role.ADMIN))
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN,
          "Somente administradores podem corrigir um encontro não realizado para realizado.");
  }

  private void exigirPrazoLancamento(User ator, LocalDate data) {
    if (ator.getPerfis().contains(Role.ADMIN)) return;
    if (!PrazoLancamentoFrequencia.estaDentroDoPrazo(data, clock.instant()))
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN,
          "O prazo para lançar a frequência desta sexta encerrou no domingo subsequente.");
  }

  private static Map<String, Object> estado(Encontro e) {
    var estado = new LinkedHashMap<String, Object>();
    estado.put("data", e.getData());
    estado.put("situacao", e.getSituacao());
    estado.put("justificativa", e.getJustificativa());
    estado.put("observacao", e.getObservacao());
    estado.put("chamadaSalvaEm", e.getChamadaSalvaEm());
    return estado;
  }

  void exigirEditavel(User ator, Encontro e) {
    escopo.exigirAlteracao(ator, e.getDiscipulado());
    if (e.getSituacao() == SituacaoEncontro.NAO_REALIZADO)
      conflito("Não é possível registrar dados em um discipulado marcado como não realizado.");
    if (ator.getPerfis().contains(Role.ADMIN)) return;
    var agora = clock.instant();
    if (e.getChamadaSalvaEm() != null) {
      if (agora.isAfter(e.getChamadaSalvaEm().plus(Duration.ofHours(3))))
        throw new ResponseStatusException(
            HttpStatus.FORBIDDEN, "A janela de três horas para alteração terminou.");
      return;
    }
    if (!PrazoLancamentoFrequencia.estaDentroDoPrazo(e.getData(), agora))
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN,
          "O prazo para lançar a frequência desta sexta encerrou no domingo subsequente.");
  }

  List<br.com.sgd.adolescente.VinculoAdolescenteDiscipulado> participantesAtuais(Encontro e) {
    return vinculos.atuais(e.getDiscipulado().getId());
  }

  void salvar(Encontro e) {
    encontros.save(e);
  }

  void auditar(User ator, String entidade, String acao, Object detalhes) {
    try {
      auditoria.save(new AuditLog(ator, entidade, acao, json.writeValueAsString(detalhes)));
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Falha ao registrar auditoria.", ex);
    }
  }

  static void conflito(String msg) {
    throw new ResponseStatusException(HttpStatus.CONFLICT, msg);
  }
}
