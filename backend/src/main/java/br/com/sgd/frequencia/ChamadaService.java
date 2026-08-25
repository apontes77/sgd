package br.com.sgd.frequencia;

import java.time.Clock;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sgd.adolescente.Adolescente;
import br.com.sgd.adolescente.AdolescenteService;
import br.com.sgd.adolescente.CategoriaAdolescente;
import br.com.sgd.adolescente.EscopoOrganizacionalService;
import br.com.sgd.adolescente.VinculoAdolescenteDiscipulado;
import br.com.sgd.user.User;

@Service
@Transactional
public class ChamadaService {
  private final EncontroService encontros;
  private final FrequenciaRepository frequencias;
  private final VisitanteRepository visitantes;
  private final AdolescenteService adolescentes;
  private final EscopoOrganizacionalService escopo;
  private final Clock clock;

  public ChamadaService(
      EncontroService e,
      FrequenciaRepository f,
      VisitanteRepository v,
      AdolescenteService a,
      EscopoOrganizacionalService es,
      Clock c) {
    encontros = e;
    frequencias = f;
    visitantes = v;
    adolescentes = a;
    escopo = es;
    clock = c;
  }

  @Transactional(readOnly = true)
  public List<Frequencia> listar(User ator, long encontroId) {
    var e = encontros.encontro(encontroId);
    escopo.exigirLeitura(ator, e.getDiscipulado());
    return frequencias.findAllByEncontroIdOrderByAdolescenteNome(encontroId);
  }

  public List<Frequencia> salvar(User ator, long encontroId, List<ItemChamada> itens) {
    var e = encontros.encontro(encontroId);
    encontros.exigirEditavel(ator, e);
    if (itens == null) throw new IllegalArgumentException("A chamada é obrigatória.");
    var atuais = encontros.participantesAtuais(e);
    var existentes = frequencias.findAllByEncontroIdOrderByAdolescenteNome(encontroId);
    Map<Long, Adolescente> conhecidos = conhecidos(atuais, existentes);
    validarItens(itens, conhecidos);
    var existentesPorId =
        existentes.stream()
            .collect(Collectors.toMap(f -> f.getAdolescente().getId(), Function.identity()));
    var agora = clock.instant();
    var resultado = new ArrayList<Frequencia>();
    var mudancas = new ArrayList<Map<String, Object>>();
    for (var item : itens) {
      var existente = Optional.ofNullable(existentesPorId.get(item.adolescenteId()));
      var anterior = existente.map(Frequencia::getSituacao).orElse(null);
      var f =
          existente.orElseGet(
              () ->
                  new Frequencia(e, conhecidos.get(item.adolescenteId()), item.situacao(), agora));
      if (existente.isPresent()) f.atualizar(item.situacao(), agora);
      resultado.add(frequencias.save(f));
      registrarMudanca(mudancas, item.adolescenteId(), anterior, item.situacao());
    }
    Set<Long> idsRecebidos =
        itens.stream().map(ItemChamada::adolescenteId).collect(Collectors.toSet());
    for (var existente : existentes) {
      long id = existente.getAdolescente().getId();
      if (!presencaOpcional(existente.getAdolescente()) || idsRecebidos.contains(id)) continue;
      frequencias.delete(existente);
      registrarMudanca(mudancas, id, existente.getSituacao(), null);
    }
    e.marcarChamadaSalva(agora);
    encontros.salvar(e);
    if (!mudancas.isEmpty())
      encontros.auditar(
          ator,
          "FREQUENCIA",
          "SUBSTITUIR_CHAMADA",
          Map.of("encontroId", encontroId, "alteracoes", mudancas));
    resultado.stream()
        .filter(f -> f.getSituacao() == SituacaoFrequencia.PRESENTE)
        .map(f -> f.getAdolescente().getId())
        .distinct()
        .forEach(id -> adolescentes.promoverVisitanteSeElegivel(ator, id));
    return resultado;
  }

  @Transactional(readOnly = true)
  public int listarVisitantes(User ator, long encontroId) {
    var e = encontros.encontro(encontroId);
    escopo.exigirLeitura(ator, e.getDiscipulado());
    return visitantes.findByEncontroId(encontroId).map(Visitante::getQuantidade).orElse(0);
  }

  public int salvarVisitantes(User ator, long encontroId, int quantidade) {
    var e = encontros.encontro(encontroId);
    encontros.exigirEditavel(ator, e);
    if (quantidade < 0) throw new IllegalArgumentException("A quantidade não pode ser negativa.");
    var agora = clock.instant();
    var existente = visitantes.findByEncontroId(encontroId);
    int anterior = existente.map(Visitante::getQuantidade).orElse(0);
    var v = existente.orElseGet(() -> new Visitante(e, quantidade, agora));
    if (existente.isPresent()) v.atualizar(quantidade, agora);
    visitantes.save(v);
    if (anterior != quantidade) {
      e.registrarAlteracao(agora);
      encontros.salvar(e);
      encontros.auditar(
          ator,
          "VISITANTE",
          "ALTERAR",
          Map.of("encontroId", encontroId, "anterior", anterior, "novo", quantidade));
    }
    return quantidade;
  }

  public record ItemChamada(Long adolescenteId, SituacaoFrequencia situacao) {}

  static boolean presencaOpcional(Adolescente adolescente) {
    CategoriaAdolescente categoria = adolescente.getCategoria();
    return categoria == CategoriaAdolescente.VISITANTE
        || categoria == CategoriaAdolescente.DISCIPULO_GOE;
  }

  private static Map<Long, Adolescente> conhecidos(
      List<VinculoAdolescenteDiscipulado> atuais, List<Frequencia> existentes) {
    Map<Long, Adolescente> porId =
        atuais.stream()
            .map(VinculoAdolescenteDiscipulado::getAdolescente)
            .collect(
                Collectors.toMap(
                    Adolescente::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    existentes.forEach(f -> porId.putIfAbsent(f.getAdolescente().getId(), f.getAdolescente()));
    return porId;
  }

  private static void validarItens(List<ItemChamada> itens, Map<Long, Adolescente> conhecidos) {
    var ids = itens.stream().map(ItemChamada::adolescenteId).toList();
    if (ids.contains(null) || new HashSet<>(ids).size() != ids.size())
      EncontroService.conflito(
          "A chamada deve conter os adolescentes ativos do discipulado e os registros anteriores deste encontro.");
    Set<Long> recebidos = new HashSet<>(ids);
    Set<Long> obrigatorios =
        conhecidos.entrySet().stream()
            .filter(entry -> !presencaOpcional(entry.getValue()))
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
    if (!recebidos.containsAll(obrigatorios) || !conhecidos.keySet().containsAll(recebidos))
      EncontroService.conflito(
          "A chamada deve conter os adolescentes ativos do discipulado e os registros anteriores deste encontro.");
    for (var item : itens) {
      if (item.situacao() == null)
        throw new IllegalArgumentException("A situação da frequência é obrigatória.");
      Adolescente adolescente = conhecidos.get(item.adolescenteId());
      if (presencaOpcional(adolescente) && item.situacao() != SituacaoFrequencia.PRESENTE)
        throw new IllegalArgumentException("GOE e visitantes só entram na chamada como presentes.");
    }
  }

  private static void registrarMudanca(
      List<Map<String, Object>> mudancas,
      long adolescenteId,
      SituacaoFrequencia anterior,
      SituacaoFrequencia novo) {
    if (anterior == novo) return;
    var m = new LinkedHashMap<String, Object>();
    m.put("adolescenteId", adolescenteId);
    m.put("anterior", anterior);
    m.put("novo", novo);
    mudancas.add(m);
  }
}
