package br.com.sgd.painel;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class PainelDesempenhoService {
  private final PainelDesempenhoRepository repository;

  public PainelDesempenhoService(PainelDesempenhoRepository repository) {
    this.repository = repository;
  }

  public PainelDesempenhoResponse consultar(LocalDate inicio, LocalDate fim) {
    validar(inicio, fim);
    Map<Long, Map<String, FrequenciaMensal>> frequencias = carregarFrequencias(inicio, fim);
    Map<Long, Map<String, Long>> tamanhos = carregarTamanhos(inicio, fim);
    List<DiscipuladoDesempenho> discipulados = new ArrayList<>();
    for (PainelDesempenhoRepository.DiscipuladoMeta meta : repository.listarDiscipulados()) {
      long id = meta.getId();
      List<FrequenciaMensal> serieFrequencia =
          frequencias.getOrDefault(id, Map.of()).values().stream()
              .sorted(Comparator.comparing(FrequenciaMensal::referencia))
              .toList();
      List<QuantidadeMensal> serieDiscipulos =
          tamanhos.getOrDefault(id, Map.of()).entrySet().stream()
              .map(entry -> new QuantidadeMensal(entry.getKey(), entry.getValue()))
              .sorted(Comparator.comparing(QuantidadeMensal::referencia))
              .toList();
      boolean ativo = Boolean.TRUE.equals(meta.getAtivo());
      if (!ativo
          && serieFrequencia.isEmpty()
          && serieDiscipulos.stream().allMatch(q -> q.quantidade() == 0)) {
        continue;
      }
      discipulados.add(
          new DiscipuladoDesempenho(
              id,
              meta.getNome(),
              meta.getSexo(),
              meta.getFaixaEtaria(),
              meta.getGerenciaId(),
              meta.getGerenciaNome(),
              ativo,
              serieFrequencia,
              serieDiscipulos));
    }
    discipulados.sort(
        Comparator.comparing(DiscipuladoDesempenho::ativo)
            .reversed()
            .thenComparing(DiscipuladoDesempenho::nome, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(DiscipuladoDesempenho::id));
    return new PainelDesempenhoResponse(inicio, fim, discipulados);
  }

  private Map<Long, Map<String, FrequenciaMensal>> carregarFrequencias(
      LocalDate inicio, LocalDate fim) {
    Map<Long, Map<String, FrequenciaMensal>> porDiscipulado = new LinkedHashMap<>();
    repository
        .frequenciasMensais(inicio, fim)
        .forEach(
            item ->
                porDiscipulado
                    .computeIfAbsent(item.getDiscipuladoId(), id -> new LinkedHashMap<>())
                    .put(
                        item.getReferencia(),
                        new FrequenciaMensal(
                            item.getReferencia(),
                            valor(item.getPresentes()),
                            valor(item.getAusentes()))));
    return porDiscipulado;
  }

  private Map<Long, Map<String, Long>> carregarTamanhos(LocalDate inicio, LocalDate fim) {
    List<PainelDesempenhoRepository.VinculoPeriodo> vinculos =
        repository.vinculosNoPeriodo(inicio, fim);
    List<LocalDate> finsDeMes = finsDeMes(inicio, fim);
    Map<Long, Map<String, Long>> porDiscipulado = new LinkedHashMap<>();
    for (LocalDate fimMes : finsDeMes) {
      String referencia = YearMonth.from(fimMes).toString();
      Map<Long, Set<Long>> adolescentesPorDiscipulado = new LinkedHashMap<>();
      for (PainelDesempenhoRepository.VinculoPeriodo vinculo : vinculos) {
        if (!vigenteEm(vinculo, fimMes)) continue;
        adolescentesPorDiscipulado
            .computeIfAbsent(vinculo.getDiscipuladoId(), id -> new HashSet<>())
            .add(vinculo.getAdolescenteId());
      }
      for (Map.Entry<Long, Set<Long>> entry : adolescentesPorDiscipulado.entrySet()) {
        porDiscipulado
            .computeIfAbsent(entry.getKey(), id -> new LinkedHashMap<>())
            .put(referencia, (long) entry.getValue().size());
      }
    }
    for (Map.Entry<Long, Map<String, Long>> entry : porDiscipulado.entrySet()) {
      Map<String, Long> serie = entry.getValue();
      for (LocalDate fimMes : finsDeMes) {
        serie.putIfAbsent(YearMonth.from(fimMes).toString(), 0L);
      }
    }
    return porDiscipulado;
  }

  private static boolean vigenteEm(
      PainelDesempenhoRepository.VinculoPeriodo vinculo, LocalDate data) {
    if (vinculo.getDataInicio().isAfter(data)) return false;
    LocalDate dataFim = vinculo.getDataFim();
    return dataFim == null || !dataFim.isBefore(data);
  }

  private static List<LocalDate> finsDeMes(LocalDate inicio, LocalDate fim) {
    List<LocalDate> fins = new ArrayList<>();
    YearMonth cursor = YearMonth.from(inicio);
    YearMonth limite = YearMonth.from(fim);
    while (!cursor.isAfter(limite)) {
      LocalDate fimMes = cursor.atEndOfMonth();
      if (fimMes.isAfter(fim)) fimMes = fim;
      if (!fimMes.isBefore(inicio)) fins.add(fimMes);
      cursor = cursor.plusMonths(1);
    }
    return fins;
  }

  private static void validar(LocalDate inicio, LocalDate fim) {
    if (inicio == null || fim == null)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Data inicial e final são obrigatórias.");
    if (inicio.isAfter(fim))
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "A data inicial não pode ser posterior à data final.");
    if (fim.isAfter(inicio.plusMonths(24)))
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "O período máximo permitido é de 24 meses.");
  }

  private static long valor(Number valor) {
    return valor == null ? 0 : valor.longValue();
  }

  public record PainelDesempenhoResponse(
      LocalDate dataInicio, LocalDate dataFim, List<DiscipuladoDesempenho> discipulados) {}

  public record DiscipuladoDesempenho(
      long id,
      String nome,
      String sexo,
      String faixaEtaria,
      long gerenciaId,
      String gerenciaNome,
      boolean ativo,
      List<FrequenciaMensal> frequencia,
      List<QuantidadeMensal> discipulos) {}

  public record FrequenciaMensal(String referencia, long presentes, long ausentes) {}

  public record QuantidadeMensal(String referencia, long quantidade) {}
}
