package br.com.sgd.painel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
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
    public PainelAdminService(PainelAdminRepository repository) { this.repository = repository; }

    public PainelAdminResponse consultar(LocalDate inicio, LocalDate fim) {
        validar(inicio, fim);
        Map<String, EvolucaoMensal> evolucao = new LinkedHashMap<>();
        repository.frequenciasMensais(inicio, fim).forEach(item -> evolucao.put(item.getReferencia(),
            new EvolucaoMensal(item.getReferencia(), valor(item.getPresentes()), valor(item.getAusentes()), 0, percentual(item.getPresentes(), item.getAusentes()))));
        repository.visitantesMensais(inicio, fim).forEach(item -> evolucao.compute(item.getReferencia(), (referencia, atual) -> atual == null
            ? new EvolucaoMensal(referencia, 0, 0, valor(item.getVisitantes()), BigDecimal.ZERO)
            : new EvolucaoMensal(referencia, atual.presentes(), atual.ausentes(), valor(item.getVisitantes()), atual.percentualPresenca())));

        List<EvolucaoMensal> serie = evolucao.values().stream().sorted(java.util.Comparator.comparing(EvolucaoMensal::referencia)).toList();
        long presentes = serie.stream().mapToLong(EvolucaoMensal::presentes).sum();
        long ausentes = serie.stream().mapToLong(EvolucaoMensal::ausentes).sum();
        long visitantes = serie.stream().mapToLong(EvolucaoMensal::visitantes).sum();
        Resumo resumo = new Resumo(repository.encontrosRealizados(inicio, fim), presentes, ausentes, visitantes, percentual(presentes, ausentes));

        List<GerenciaIndicador> gerencias = repository.porGerencia(inicio, fim).stream()
            .map(item -> new GerenciaIndicador(item.getId(), item.getNome(), valor(item.getPresentes()), valor(item.getAusentes()), percentual(item.getPresentes(), item.getAusentes())))
            .sorted(java.util.Comparator.comparing(GerenciaIndicador::percentualPresenca).reversed().thenComparing(GerenciaIndicador::nome)).toList();

        List<GerenciaMensalIndicador> gerenciasMensal = repository.porGerenciaMensal(inicio, fim).stream()
            .map(item -> new GerenciaMensalIndicador(item.getGerenciaId(), item.getGerenciaNome(), item.getReferencia(),
                valor(item.getPresentes()), valor(item.getAusentes()), percentual(item.getPresentes(), item.getAusentes())))
            .toList();

        Map<String, SexoIndicador> sexosEncontrados = new LinkedHashMap<>();
        repository.porSexo(inicio, fim).forEach(item -> sexosEncontrados.put(item.getSexo(),
            new SexoIndicador(item.getSexo(), valor(item.getPresentes()), valor(item.getAusentes()), percentual(item.getPresentes(), item.getAusentes()))));
        List<SexoIndicador> sexos = new ArrayList<>();
        sexos.add(sexosEncontrados.getOrDefault("MASCULINO", new SexoIndicador("MASCULINO", 0, 0, BigDecimal.ZERO)));
        sexos.add(sexosEncontrados.getOrDefault("FEMININO", new SexoIndicador("FEMININO", 0, 0, BigDecimal.ZERO)));
        long naoRealizados = repository.encontrosNaoRealizados(inicio, fim);
        return new PainelAdminResponse(inicio, fim, resumo, serie, gerencias, sexos, naoRealizados, gerenciasMensal);
    }

    private static void validar(LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data inicial e final são obrigatórias.");
        if (inicio.isAfter(fim)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data inicial não pode ser posterior à data final.");
        if (fim.isAfter(inicio.plusMonths(24))) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O período máximo permitido é de 24 meses.");
    }
    private static long valor(Number valor) { return valor == null ? 0 : valor.longValue(); }
    private static BigDecimal percentual(Number presentes, Number ausentes) { return percentual(valor(presentes), valor(ausentes)); }
    private static BigDecimal percentual(long presentes, long ausentes) {
        long total = presentes + ausentes;
        return total == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(presentes).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    public record PainelAdminResponse(LocalDate dataInicio, LocalDate dataFim, Resumo resumo, List<EvolucaoMensal> evolucao,
                                      List<GerenciaIndicador> gerencias, List<SexoIndicador> sexos,
                                      long encontrosNaoRealizados, List<GerenciaMensalIndicador> gerenciasMensal) { }
    public record Resumo(long encontrosRealizados, long presentes, long ausentes, long visitantes, BigDecimal percentualPresenca) { }
    public record EvolucaoMensal(String referencia, long presentes, long ausentes, long visitantes, BigDecimal percentualPresenca) { }
    public record GerenciaIndicador(long id, String nome, long presentes, long ausentes, BigDecimal percentualPresenca) { }
    public record GerenciaMensalIndicador(long gerenciaId, String gerenciaNome, String referencia, long presentes, long ausentes, BigDecimal percentualPresenca) { }
    public record SexoIndicador(String sexo, long presentes, long ausentes, BigDecimal percentualPresenca) { }
}
