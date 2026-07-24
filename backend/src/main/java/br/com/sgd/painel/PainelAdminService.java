package br.com.sgd.painel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class PainelAdminService {
  private final PainelAdminRepository repository;

  public PainelAdminService(PainelAdminRepository repository) {
    this.repository = repository;
  }

  public PainelAdminResponse consultar(LocalDate inicio, LocalDate fim) {
    validar(inicio, fim);
    List<EvolucaoMensal> evolucao = evolucaoMensal(inicio, fim);
    return new PainelAdminResponse(
        inicio,
        fim,
        resumo(inicio, fim, evolucao),
        evolucao,
        gerencias(inicio, fim),
        sexos(inicio, fim),
        repository.encontrosNaoRealizados(inicio, fim),
        gerenciasMensal(inicio, fim));
  }

  private List<EvolucaoMensal> evolucaoMensal(LocalDate inicio, LocalDate fim) {
    Map<String, EvolucaoMensal> meses = new LinkedHashMap<>();
    repository
        .frequenciasMensais(inicio, fim)
        .forEach(
            item ->
                meses.put(
                    item.getReferencia(),
                    new EvolucaoMensal(
                        item.getReferencia(),
                        valor(item.getPresentes()),
                        valor(item.getAusentes()),
                        0,
                        percentual(item.getPresentes(), item.getAusentes()))));
    repository
        .visitantesMensais(inicio, fim)
        .forEach(
            item ->
                meses.compute(
                    item.getReferencia(),
                    (referencia, atual) ->
                        atual == null
                            ? new EvolucaoMensal(
                                referencia, 0, 0, valor(item.getVisitantes()), BigDecimal.ZERO)
                            : new EvolucaoMensal(
                                referencia,
                                atual.presentes(),
                                atual.ausentes(),
                                valor(item.getVisitantes()),
                                atual.percentualPresenca())));
    return meses.values().stream()
        .sorted(Comparator.comparing(EvolucaoMensal::referencia))
        .toList();
  }

  private Resumo resumo(LocalDate inicio, LocalDate fim, List<EvolucaoMensal> evolucao) {
    long presentes = evolucao.stream().mapToLong(EvolucaoMensal::presentes).sum();
    long ausentes = evolucao.stream().mapToLong(EvolucaoMensal::ausentes).sum();
    long visitantes = evolucao.stream().mapToLong(EvolucaoMensal::visitantes).sum();
    return new Resumo(
        repository.encontrosRealizados(inicio, fim),
        presentes,
        ausentes,
        visitantes,
        percentual(presentes, ausentes));
  }

  private List<GerenciaIndicador> gerencias(LocalDate inicio, LocalDate fim) {
    return repository.porGerencia(inicio, fim).stream()
        .map(
            item ->
                new GerenciaIndicador(
                    item.getId(),
                    item.getNome(),
                    valor(item.getPresentes()),
                    valor(item.getAusentes()),
                    percentual(item.getPresentes(), item.getAusentes())))
        .sorted(
            Comparator.comparing(GerenciaIndicador::percentualPresenca)
                .reversed()
                .thenComparing(GerenciaIndicador::nome))
        .toList();
  }

  private List<GerenciaMensalIndicador> gerenciasMensal(LocalDate inicio, LocalDate fim) {
    return repository.porGerenciaMensal(inicio, fim).stream()
        .map(
            item ->
                new GerenciaMensalIndicador(
                    item.getGerenciaId(),
                    item.getGerenciaNome(),
                    item.getReferencia(),
                    valor(item.getPresentes()),
                    valor(item.getAusentes()),
                    percentual(item.getPresentes(), item.getAusentes())))
        .toList();
  }

  private List<SexoIndicador> sexos(LocalDate inicio, LocalDate fim) {
    Map<String, SexoIndicador> encontrados = new LinkedHashMap<>();
    repository
        .porSexo(inicio, fim)
        .forEach(
            item ->
                encontrados.put(
                    item.getSexo(),
                    new SexoIndicador(
                        item.getSexo(),
                        valor(item.getPresentes()),
                        valor(item.getAusentes()),
                        percentual(item.getPresentes(), item.getAusentes()))));
    List<SexoIndicador> sexos = new ArrayList<>();
    sexos.add(encontrados.getOrDefault("MASCULINO", sexoVazio("MASCULINO")));
    sexos.add(encontrados.getOrDefault("FEMININO", sexoVazio("FEMININO")));
    return sexos;
  }

  private static SexoIndicador sexoVazio(String sexo) {
    return new SexoIndicador(sexo, 0, 0, BigDecimal.ZERO);
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

  private static BigDecimal percentual(Number presentes, Number ausentes) {
    return percentual(valor(presentes), valor(ausentes));
  }

  private static BigDecimal percentual(long presentes, long ausentes) {
    long total = presentes + ausentes;
    return total == 0
        ? BigDecimal.ZERO
        : BigDecimal.valueOf(presentes)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
  }

  public record PainelAdminResponse(
      LocalDate dataInicio,
      LocalDate dataFim,
      Resumo resumo,
      List<EvolucaoMensal> evolucao,
      List<GerenciaIndicador> gerencias,
      List<SexoIndicador> sexos,
      long encontrosNaoRealizados,
      List<GerenciaMensalIndicador> gerenciasMensal) {}

  public record Resumo(
      long encontrosRealizados,
      long presentes,
      long ausentes,
      long visitantes,
      BigDecimal percentualPresenca) {}

  public record EvolucaoMensal(
      String referencia,
      long presentes,
      long ausentes,
      long visitantes,
      BigDecimal percentualPresenca) {}

  public record GerenciaIndicador(
      long id, String nome, long presentes, long ausentes, BigDecimal percentualPresenca) {}

  public record GerenciaMensalIndicador(
      long gerenciaId,
      String gerenciaNome,
      String referencia,
      long presentes,
      long ausentes,
      BigDecimal percentualPresenca) {}

  public record SexoIndicador(
      String sexo, long presentes, long ausentes, BigDecimal percentualPresenca) {}
}
