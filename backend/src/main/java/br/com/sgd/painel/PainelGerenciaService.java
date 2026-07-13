package br.com.sgd.painel;

import br.com.sgd.organizacao.Discipulado;
import br.com.sgd.organizacao.DiscipuladoRepository;
import br.com.sgd.organizacao.Gerencia;
import br.com.sgd.organizacao.GerenciaRepository;
import br.com.sgd.user.User;
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
public class PainelGerenciaService {
    private final PainelGerenciaRepository painel;
    private final GerenciaRepository gerencias;
    private final DiscipuladoRepository discipulados;

    public PainelGerenciaService(PainelGerenciaRepository painel, GerenciaRepository gerencias, DiscipuladoRepository discipulados) {
        this.painel = painel;
        this.gerencias = gerencias;
        this.discipulados = discipulados;
    }

    public PainelGerenciaResponse consultar(User gerente, LocalDate inicio, LocalDate fim) {
        validarPeriodo(inicio, fim);
        List<Gerencia> encontradas = gerencias.findAllByGerenteIdAndAtivoTrue(gerente.getId());
        if (encontradas.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "O gerente não possui uma gerência ativa.");
        if (encontradas.size() > 1) throw new ResponseStatusException(HttpStatus.CONFLICT, "O gerente possui mais de uma gerência ativa.");
        Gerencia gerencia = encontradas.getFirst();
        long gerenciaId = gerencia.getId();

        List<EvolucaoMensal> evolucao = evolucaoGerencia(gerenciaId, inicio, fim);
        Resumo resumo = new Resumo(painel.encontrosRealizados(gerenciaId, inicio, fim),
            evolucao.stream().mapToLong(EvolucaoMensal::presentes).sum(), evolucao.stream().mapToLong(EvolucaoMensal::ausentes).sum(),
            evolucao.stream().mapToLong(EvolucaoMensal::visitantes).sum(), BigDecimal.ZERO);
        resumo = resumo.comPercentual(percentual(resumo.presentes(), resumo.ausentes()));

        Map<Long, Resumo> resumos = new LinkedHashMap<>();
        painel.frequenciasPorDiscipulado(gerenciaId, inicio, fim).forEach(item -> resumos.put(item.getDiscipuladoId(),
            new Resumo(valor(item.getEncontrosRealizados()), valor(item.getPresentes()), valor(item.getAusentes()), 0,
                percentual(item.getPresentes(), item.getAusentes()))));
        painel.visitantesPorDiscipulado(gerenciaId, inicio, fim).forEach(item -> resumos.compute(item.getDiscipuladoId(), (id, atual) ->
            (atual == null ? Resumo.vazio() : atual).comVisitantes(valor(item.getVisitantes()))));

        Map<Long, Map<String, EvolucaoMensal>> evolucoes = new LinkedHashMap<>();
        painel.frequenciasMensaisPorDiscipulado(gerenciaId, inicio, fim).forEach(item -> evolucoes.computeIfAbsent(item.getDiscipuladoId(), id -> new LinkedHashMap<>()).put(item.getReferencia(),
            new EvolucaoMensal(item.getReferencia(), valor(item.getPresentes()), valor(item.getAusentes()), 0, percentual(item.getPresentes(), item.getAusentes()))));
        painel.visitantesMensaisPorDiscipulado(gerenciaId, inicio, fim).forEach(item -> evolucoes.computeIfAbsent(item.getDiscipuladoId(), id -> new LinkedHashMap<>()).compute(item.getReferencia(), (referencia, atual) ->
            atual == null ? new EvolucaoMensal(referencia, 0, 0, valor(item.getVisitantes()), BigDecimal.ZERO)
                : new EvolucaoMensal(referencia, atual.presentes(), atual.ausentes(), valor(item.getVisitantes()), atual.percentualPresenca())));

        List<DiscipuladoIndicador> indicadores = new ArrayList<>();
        for (Discipulado d : discipulados.findAllByGerenciaIdOrderByNomeAsc(gerenciaId)) {
            Resumo itemResumo = resumos.getOrDefault(d.getId(), Resumo.vazio());
            if (!d.isAtivo() && itemResumo.presentes() + itemResumo.ausentes() + itemResumo.visitantes() == 0) continue;
            List<EvolucaoMensal> serie = evolucoes.getOrDefault(d.getId(), Map.of()).values().stream()
                .sorted(Comparator.comparing(EvolucaoMensal::referencia)).toList();
            indicadores.add(new DiscipuladoIndicador(d.getId(), d.getNome(), d.getSexo().name(), d.isAtivo(), itemResumo, serie));
        }
        indicadores.sort(Comparator.comparing((DiscipuladoIndicador d) -> d.resumo().percentualPresenca()).reversed().thenComparing(DiscipuladoIndicador::nome));
        return new PainelGerenciaResponse(inicio, fim, new GerenciaInfo(gerenciaId, gerencia.getNome()), resumo, evolucao, indicadores);
    }

    private List<EvolucaoMensal> evolucaoGerencia(long id, LocalDate inicio, LocalDate fim) {
        Map<String, EvolucaoMensal> itens = new LinkedHashMap<>();
        painel.frequenciasMensais(id, inicio, fim).forEach(item -> itens.put(item.getReferencia(), new EvolucaoMensal(item.getReferencia(),
            valor(item.getPresentes()), valor(item.getAusentes()), 0, percentual(item.getPresentes(), item.getAusentes()))));
        painel.visitantesMensais(id, inicio, fim).forEach(item -> itens.compute(item.getReferencia(), (referencia, atual) -> atual == null
            ? new EvolucaoMensal(referencia, 0, 0, valor(item.getVisitantes()), BigDecimal.ZERO)
            : new EvolucaoMensal(referencia, atual.presentes(), atual.ausentes(), valor(item.getVisitantes()), atual.percentualPresenca())));
        return itens.values().stream().sorted(Comparator.comparing(EvolucaoMensal::referencia)).toList();
    }

    static void validarPeriodo(LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data inicial e final são obrigatórias.");
        if (inicio.isAfter(fim)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data inicial não pode ser posterior à data final.");
        if (fim.isAfter(inicio.plusMonths(24))) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O período máximo permitido é de 24 meses.");
    }
    private static long valor(Number n) { return n == null ? 0 : n.longValue(); }
    private static BigDecimal percentual(Number p, Number a) { return percentual(valor(p), valor(a)); }
    private static BigDecimal percentual(long p, long a) { long total = p + a; return total == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(p).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP); }

    public record PainelGerenciaResponse(LocalDate dataInicio, LocalDate dataFim, GerenciaInfo gerencia, Resumo resumo, List<EvolucaoMensal> evolucao, List<DiscipuladoIndicador> discipulados) { }
    public record GerenciaInfo(long id, String nome) { }
    public record Resumo(long encontrosRealizados, long presentes, long ausentes, long visitantes, BigDecimal percentualPresenca) {
        static Resumo vazio() { return new Resumo(0, 0, 0, 0, BigDecimal.ZERO); }
        Resumo comVisitantes(long valor) { return new Resumo(encontrosRealizados, presentes, ausentes, valor, percentualPresenca); }
        Resumo comPercentual(BigDecimal valor) { return new Resumo(encontrosRealizados, presentes, ausentes, visitantes, valor); }
    }
    public record EvolucaoMensal(String referencia, long presentes, long ausentes, long visitantes, BigDecimal percentualPresenca) { }
    public record DiscipuladoIndicador(long id, String nome, String sexo, boolean ativo, Resumo resumo, List<EvolucaoMensal> evolucao) { }
}
