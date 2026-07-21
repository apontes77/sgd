package br.com.sgd.relatorio;

import br.com.sgd.frequencia.Encontro;
import br.com.sgd.frequencia.Frequencia;
import br.com.sgd.frequencia.FrequenciaRepository;
import br.com.sgd.frequencia.SituacaoEncontro;
import br.com.sgd.frequencia.SituacaoFrequencia;
import br.com.sgd.organizacao.DiscipuladoRepository;
import br.com.sgd.organizacao.GerenciaRepository;
import br.com.sgd.user.Role;
import br.com.sgd.user.User;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class RelatorioFrequenciaService {
    private final RelatorioFrequenciaRepository encontros;
    private final FrequenciaRepository frequencias;
    private final GerenciaRepository gerencias;
    private final DiscipuladoRepository discipulados;
    private final Clock clock;

    public RelatorioFrequenciaService(RelatorioFrequenciaRepository encontros, FrequenciaRepository frequencias,
            GerenciaRepository gerencias, DiscipuladoRepository discipulados, Clock clock) {
        this.encontros = encontros;
        this.frequencias = frequencias;
        this.gerencias = gerencias;
        this.discipulados = discipulados;
        this.clock = clock;
    }

    public RelatorioDiarioResponse consultar(User usuario, LocalDate data) {
        if (data == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data do relatório é obrigatória.");
        RelatorioPeriodoResponse periodo = consultarPeriodo(usuario, data, data);
        return new RelatorioDiarioResponse(data, periodo.emitidoEm(), periodo.relatorios());
    }

    public RelatorioPeriodoResponse consultarPeriodo(User usuario, LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "As datas inicial e final s\u00e3o obrigat\u00f3rias.");
        if (inicio.isAfter(fim)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data inicial n\u00e3o pode ser posterior \u00e0 data final.");
        if (fim.isAfter(inicio.plusMonths(12))) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O per\u00edodo do relat\u00f3rio deve ser de no m\u00e1ximo 12 meses.");
        boolean administrador = usuario.getPerfis().contains(Role.ADMIN);
        Escopo escopo = administrador ? new Escopo(Set.of()) : escopoRestrito(usuario);
        List<Encontro> encontrados = administrador
                ? encontros.noPeriodo(inicio, fim)
                : escopo.discipuladoIds().isEmpty() ? List.of()
                : encontros.noPeriodoDoEscopo(inicio, fim, escopo.discipuladoIds());
        return new RelatorioPeriodoResponse(inicio, fim, clock.instant(), montarRelatorios(encontrados));
    }

    private Escopo escopoRestrito(User usuario) {
        Set<Long> ids = new LinkedHashSet<>();
        boolean associado = false;
        if (usuario.getPerfis().contains(Role.GERENTE)) {
            var gerenciasAtivas = gerencias.findAllByGerenteIdAndAtivoTrue(usuario.getId());
            associado = !gerenciasAtivas.isEmpty();
            gerenciasAtivas.forEach(gerencia -> discipulados.findAllByGerenciaIdOrderByNomeAsc(gerencia.getId())
                    .forEach(discipulado -> ids.add(discipulado.getId())));
        }
        if (usuario.getPerfis().contains(Role.DISCIPULADOR) || usuario.getPerfis().contains(Role.CO_LIDER)) {
            var liderados = discipulados.findAllByLiderancaUsuarioId(usuario.getId());
            associado = associado || !liderados.isEmpty();
            liderados.forEach(discipulado -> ids.add(discipulado.getId()));
        }
        if (!associado) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "O usuário não possui escopo organizacional para relatórios de frequência.");
        return new Escopo(ids);
    }

    private List<RelatorioEncontro> montarRelatorios(List<Encontro> encontrados) {
        if (encontrados.isEmpty()) return List.of();
        List<Long> encontroIds = encontrados.stream().map(Encontro::getId).toList();
        Map<Long, List<Frequencia>> frequenciasPorEncontro = frequencias
                .findAllByEncontroIdInOrderByEncontroIdAscAdolescenteNomeAsc(encontroIds).stream()
                .collect(Collectors.groupingBy(f -> f.getEncontro().getId()));
        Map<Long, Integer> visitantesPorEncontro = encontros.contarVisitantesPorEncontro(encontroIds).stream()
                .collect(Collectors.toMap(v -> v.getEncontroId(), v -> v.getVisitantes() == null ? 0 : v.getVisitantes()));

        List<RelatorioEncontro> resultado = new ArrayList<>();
        for (Encontro encontro : encontrados) {
            var discipulado = encontro.getDiscipulado();
            List<LiderInfo> coLideres = discipulado.getCoLideres().stream().map(u -> new LiderInfo(u.getId(), u.getNome()))
                    .sorted(Comparator.comparing(LiderInfo::nome, String.CASE_INSENSITIVE_ORDER)).toList();
            if (encontro.getSituacao() == SituacaoEncontro.NAO_REALIZADO) {
                resultado.add(new RelatorioEncontro(encontro.getId(), encontro.getData(), encontro.getSituacao(),
                        encontro.getJustificativa(),
                        new GerenciaInfo(discipulado.getGerencia().getId(), discipulado.getGerencia().getNome()),
                        new DiscipuladoInfo(discipulado.getId(), discipulado.getNome(), discipulado.getSexo().name()),
                        new LiderInfo(discipulado.getDiscipulador().getId(), discipulado.getDiscipulador().getNome()), coLideres,
                        List.of(), 0, new ResumoFrequencia(0, 0, 0, 0, BigDecimal.ZERO)));
                continue;
            }
            List<ParticipanteInfo> participantes = frequenciasPorEncontro.getOrDefault(encontro.getId(), List.of()).stream()
                    .map(f -> new ParticipanteInfo(f.getAdolescente().getId(), f.getAdolescente().getNome(),
                            f.getAdolescente().getTelefone(), f.getSituacao()))
                    .sorted(Comparator.comparing(ParticipanteInfo::nome, String.CASE_INSENSITIVE_ORDER)).toList();
            long presentes = participantes.stream().filter(p -> p.situacao() == SituacaoFrequencia.PRESENTE).count();
            long ausentes = participantes.stream().filter(p -> p.situacao() == SituacaoFrequencia.AUSENTE).count();
            int quantidadeVisitantes = visitantesPorEncontro.getOrDefault(encontro.getId(), 0);
            resultado.add(new RelatorioEncontro(encontro.getId(), encontro.getData(), encontro.getSituacao(), null,
                    new GerenciaInfo(discipulado.getGerencia().getId(), discipulado.getGerencia().getNome()),
                    new DiscipuladoInfo(discipulado.getId(), discipulado.getNome(), discipulado.getSexo().name()),
                    new LiderInfo(discipulado.getDiscipulador().getId(), discipulado.getDiscipulador().getNome()), coLideres,
                    participantes, quantidadeVisitantes,
                    new ResumoFrequencia(presentes, ausentes, participantes.size(), quantidadeVisitantes, percentual(presentes, ausentes))));
        }
        return resultado;
    }

    private static BigDecimal percentual(long presentes, long ausentes) {
        long total = presentes + ausentes;
        return total == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(presentes).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    private record Escopo(Set<Long> discipuladoIds) { }
    public record RelatorioDiarioResponse(LocalDate data, Instant emitidoEm, List<RelatorioEncontro> relatorios) { }
    public record RelatorioPeriodoResponse(LocalDate dataInicio, LocalDate dataFim, Instant emitidoEm, List<RelatorioEncontro> relatorios) { }
    public record RelatorioEncontro(long encontroId, LocalDate data, SituacaoEncontro situacao, String justificativa,
            GerenciaInfo gerencia, DiscipuladoInfo discipulado,
            LiderInfo discipulador, List<LiderInfo> coLideres, List<ParticipanteInfo> participantes, int visitantes,
            ResumoFrequencia resumo) { }
    public record GerenciaInfo(long id, String nome) { }
    public record DiscipuladoInfo(long id, String nome, String sexo) { }
    public record LiderInfo(long id, String nome) { }
    public record ParticipanteInfo(long adolescenteId, String nome, String telefone, SituacaoFrequencia situacao) { }
    public record ResumoFrequencia(long presentes, long ausentes, long participantes, int visitantes,
            BigDecimal percentualPresenca) { }
}
