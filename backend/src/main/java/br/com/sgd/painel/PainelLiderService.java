package br.com.sgd.painel;

import br.com.sgd.organizacao.Discipulado;
import br.com.sgd.organizacao.DiscipuladoRepository;
import br.com.sgd.user.User;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
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
public class PainelLiderService {
    private final PainelLiderRepository painel;
    private final DiscipuladoRepository discipulados;

    public PainelLiderService(PainelLiderRepository painel, DiscipuladoRepository discipulados) {
        this.painel = painel;
        this.discipulados = discipulados;
    }

    public PainelLiderResponse consultar(User usuario, LocalDate inicio, LocalDate fim) {
        PainelGerenciaService.validarPeriodo(inicio, fim);
        List<Discipulado> encontrados = discipulados.findAllByLiderancaUsuarioId(usuario.getId());
        if (encontrados.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "O usuário não possui um discipulado associado à sua liderança.");
        if (encontrados.size() > 1) throw new ResponseStatusException(HttpStatus.CONFLICT, "O usuário possui mais de uma associação de liderança.");
        Discipulado discipulado = encontrados.getFirst();
        long discipuladoId = discipulado.getId();

        Map<String, EvolucaoMensal> meses = new LinkedHashMap<>();
        painel.frequenciasMensais(discipuladoId, inicio, fim).forEach(item -> meses.put(item.getReferencia(),
            new EvolucaoMensal(item.getReferencia(), valor(item.getPresentes()), valor(item.getAusentes()), 0,
                percentual(item.getPresentes(), item.getAusentes()))));
        painel.visitantesMensais(discipuladoId, inicio, fim).forEach(item -> meses.compute(item.getReferencia(), (referencia, atual) ->
            atual == null ? new EvolucaoMensal(referencia, 0, 0, valor(item.getVisitantes()), BigDecimal.ZERO)
                : new EvolucaoMensal(referencia, atual.presentes(), atual.ausentes(), valor(item.getVisitantes()), atual.percentualPresenca())));
        List<EvolucaoMensal> evolucao = meses.values().stream().sorted(Comparator.comparing(EvolucaoMensal::referencia)).toList();
        long presentes = evolucao.stream().mapToLong(EvolucaoMensal::presentes).sum();
        long ausentes = evolucao.stream().mapToLong(EvolucaoMensal::ausentes).sum();
        long visitantes = evolucao.stream().mapToLong(EvolucaoMensal::visitantes).sum();
        Resumo resumo = new Resumo(painel.encontrosRealizados(discipuladoId, inicio, fim), presentes, ausentes, visitantes, percentual(presentes, ausentes));
        DiscipuladoInfo info = new DiscipuladoInfo(discipuladoId, discipulado.getNome(), discipulado.getSexo().name(), discipulado.isAtivo());
        return new PainelLiderResponse(inicio, fim, info, resumo, evolucao);
    }

    private static long valor(Number valor) { return valor == null ? 0 : valor.longValue(); }
    private static BigDecimal percentual(Number presentes, Number ausentes) { return percentual(valor(presentes), valor(ausentes)); }
    private static BigDecimal percentual(long presentes, long ausentes) {
        long total = presentes + ausentes;
        return total == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(presentes).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    public record PainelLiderResponse(LocalDate dataInicio, LocalDate dataFim, DiscipuladoInfo discipulado, Resumo resumo, List<EvolucaoMensal> evolucao) { }
    public record DiscipuladoInfo(long id, String nome, String sexo, boolean ativo) { }
    public record Resumo(long encontrosRealizados, long presentes, long ausentes, long visitantes, BigDecimal percentualPresenca) { }
    public record EvolucaoMensal(String referencia, long presentes, long ausentes, long visitantes, BigDecimal percentualPresenca) { }
}
