package br.com.sgd.relatorio;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import br.com.sgd.frequencia.PrazoLancamentoFrequencia;
import br.com.sgd.frequencia.SituacaoEncontro;
import br.com.sgd.frequencia.SituacaoFrequencia;
import br.com.sgd.organizacao.DiscipuladoRepository;
import br.com.sgd.relatorio.RelatorioFrequenciaRepository.CoLiderRow;
import br.com.sgd.relatorio.RelatorioFrequenciaRepository.EncontroCabecalho;
import br.com.sgd.relatorio.RelatorioFrequenciaRepository.ObservacaoLiderancaRow;
import br.com.sgd.relatorio.RelatorioFrequenciaRepository.ParticipanteRow;
import br.com.sgd.relatorio.RelatorioFrequenciaRepository.ResumoEncontro;
import br.com.sgd.relatorio.RelatorioFrequenciaRepository.VisitantesPorEncontro;
import br.com.sgd.user.Role;
import br.com.sgd.user.User;

@Service
@Transactional(readOnly = true)
public class RelatorioFrequenciaService {
  private final RelatorioFrequenciaRepository relatorios;
  private final DiscipuladoRepository discipulados;
  private final Clock clock;

  public RelatorioFrequenciaService(
      RelatorioFrequenciaRepository relatorios, DiscipuladoRepository discipulados, Clock clock) {
    this.relatorios = relatorios;
    this.discipulados = discipulados;
    this.clock = clock;
  }

  public RelatorioDiarioResponse consultar(User usuario, LocalDate data, Long discipuladoId) {
    if (data == null)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "A data do relatório é obrigatória.");
    RelatorioPeriodoResponse periodo = consultarPeriodo(usuario, data, data, discipuladoId, true);
    return new RelatorioDiarioResponse(data, periodo.emitidoEm(), periodo.relatorios());
  }

  public RelatorioPeriodoResponse consultarPeriodo(
      User usuario, LocalDate inicio, LocalDate fim, Long discipuladoId) {
    return consultarPeriodo(
        usuario, inicio, fim, discipuladoId, inicio != null && inicio.equals(fim));
  }

  public void exportarExcel(
      User usuario, LocalDate inicio, LocalDate fim, Long discipuladoId, OutputStream out)
      throws IOException {
    RelatorioPeriodoResponse periodo = consultarPeriodo(usuario, inicio, fim, discipuladoId, false);
    try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
      Sheet sheet = workbook.createSheet("Frequências");
      escreverCabecalhoExcel(sheet);
      int linha = 1;
      for (RelatorioEncontro item : periodo.relatorios()) {
        preencherLinhaExcel(sheet.createRow(linha++), item);
      }
      workbook.write(out);
      workbook.dispose();
    }
  }

  private static void escreverCabecalhoExcel(Sheet sheet) {
    Row cabecalho = sheet.createRow(0);
    String[] colunas = {
      "Discipulador(a)",
      "Gerente",
      "Data",
      "Presentes",
      "Ausentes",
      "Visitantes",
      "GOE",
      "Total de presentes",
      "Observação do discipulador",
      "Observação estrutura"
    };
    for (int i = 0; i < colunas.length; i++) {
      cabecalho.createCell(i).setCellValue(colunas[i]);
    }
  }

  private static void preencherLinhaExcel(Row row, RelatorioEncontro item) {
    boolean naoRealizado = item.situacao() == SituacaoEncontro.NAO_REALIZADO;
    long presentes = naoRealizado ? 0 : item.resumo().presentes();
    long ausentes = naoRealizado ? 0 : item.resumo().ausentes();
    int visitantes = naoRealizado ? 0 : item.visitantes();
    int goe = naoRealizado ? 0 : item.goe();
    row.createCell(0).setCellValue(item.discipulador().nome());
    row.createCell(1).setCellValue(item.gerenteNome());
    row.createCell(2)
        .setCellValue(
            item.data().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    row.createCell(3).setCellValue(presentes);
    row.createCell(4).setCellValue(ausentes);
    row.createCell(5).setCellValue(visitantes);
    row.createCell(6).setCellValue(goe);
    row.createCell(7).setCellValue(presentes + visitantes + goe);
    row.createCell(8).setCellValue(observacaoExcel(item));
    row.createCell(9).setCellValue(texto(item.observacaoEstrutura()));
  }

  private static String observacaoExcel(RelatorioEncontro item) {
    String observacao = item.observacao();
    if ((observacao == null || observacao.isBlank())
        && item.situacao() == SituacaoEncontro.NAO_REALIZADO
        && item.justificativa() != null) {
      observacao = item.justificativa();
    }
    if (!item.fechamentoAutomatico()) {
      return observacao == null ? "" : observacao;
    }
    String aviso = PrazoLancamentoFrequencia.AVISO_LANCAMENTO_PENDENTE;
    if (observacao == null || observacao.isBlank()) return aviso;
    return aviso + " " + observacao;
  }

  private RelatorioPeriodoResponse consultarPeriodo(
      User usuario,
      LocalDate inicio,
      LocalDate fim,
      Long discipuladoId,
      boolean incluirParticipantes) {
    if (inicio == null || fim == null)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "As datas inicial e final s\u00e3o obrigat\u00f3rias.");
    if (inicio.isAfter(fim))
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "A data inicial n\u00e3o pode ser posterior \u00e0 data final.");
    if (fim.isAfter(inicio.plusMonths(12)))
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "O per\u00edodo do relat\u00f3rio deve ser de no m\u00e1ximo 12 meses.");
    boolean administrador = usuario.getPerfis().contains(Role.ADMIN);
    Escopo escopo = administrador ? new Escopo(Set.of()) : escopoRestrito(usuario);
    Set<Long> idsConsulta = resolverDiscipuladosConsulta(administrador, escopo, discipuladoId);
    List<EncontroCabecalho> encontrados =
        idsConsulta == null
            ? relatorios.cabecalhosNoPeriodo(inicio, fim)
            : idsConsulta.isEmpty()
                ? List.of()
                : relatorios.cabecalhosNoPeriodoDoEscopo(inicio, fim, idsConsulta);
    return new RelatorioPeriodoResponse(
        inicio, fim, clock.instant(), montarRelatorios(encontrados, incluirParticipantes));
  }

  private Set<Long> resolverDiscipuladosConsulta(
      boolean administrador, Escopo escopo, Long discipuladoId) {
    if (discipuladoId == null) {
      return administrador ? null : escopo.discipuladoIds();
    }
    if (administrador) {
      if (!discipulados.existsById(discipuladoId))
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Discipulado não encontrado.");
      return Set.of(discipuladoId);
    }
    if (!escopo.discipuladoIds().contains(discipuladoId))
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "O discipulado informado está fora do seu escopo.");
    return Set.of(discipuladoId);
  }

  private Escopo escopoRestrito(User usuario) {
    Set<Long> ids = new LinkedHashSet<>();
    boolean associado = false;
    if (usuario.getPerfis().contains(Role.GERENTE)) {
      List<Long> gerenciaIds = relatorios.idsPorGerente(usuario.getId());
      associado = !gerenciaIds.isEmpty();
      ids.addAll(gerenciaIds);
    }
    if (usuario.getPerfis().contains(Role.DISCIPULADOR)
        || usuario.getPerfis().contains(Role.CO_LIDER)) {
      List<Long> liderados = relatorios.idsPorLideranca(usuario.getId());
      associado = associado || !liderados.isEmpty();
      ids.addAll(liderados);
    }
    if (!associado)
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND,
          "O usuário não possui escopo organizacional para relatórios de frequência.");
    return new Escopo(ids);
  }

  private List<RelatorioEncontro> montarRelatorios(
      List<EncontroCabecalho> encontrados, boolean incluirParticipantes) {
    if (encontrados.isEmpty()) return List.of();
    List<Long> encontroIds = encontrados.stream().map(EncontroCabecalho::getEncontroId).toList();
    List<Long> discipuladoIds =
        encontrados.stream().map(EncontroCabecalho::getDiscipuladoId).distinct().toList();
    Map<Long, List<LiderInfo>> coLideresPorDiscipulado = coLideres(discipuladoIds);
    Map<Long, ResumoEncontro> resumosPorEncontro = resumos(encontroIds);
    Map<Long, Integer> visitantesPorEncontro = visitantes(encontroIds);
    Map<ChaveObservacao, String> observacoesEstrutura = observacoesEstrutura(encontrados);
    Map<Long, List<ParticipanteInfo>> participantesPorEncontro =
        incluirParticipantes ? participantes(encontroIds) : Map.of();
    return encontrados.stream()
        .map(
            encontro ->
                montarEncontro(
                    encontro,
                    coLideresPorDiscipulado,
                    resumosPorEncontro,
                    visitantesPorEncontro,
                    observacoesEstrutura,
                    participantesPorEncontro))
        .toList();
  }

  private RelatorioEncontro montarEncontro(
      EncontroCabecalho encontro,
      Map<Long, List<LiderInfo>> coLideresPorDiscipulado,
      Map<Long, ResumoEncontro> resumosPorEncontro,
      Map<Long, Integer> visitantesPorEncontro,
      Map<ChaveObservacao, String> observacoesEstrutura,
      Map<Long, List<ParticipanteInfo>> participantesPorEncontro) {
    SituacaoEncontro situacao = SituacaoEncontro.valueOf(encontro.getSituacao());
    if (situacao == SituacaoEncontro.NAO_REALIZADO) {
      return relatorioNaoRealizado(encontro, coLideresPorDiscipulado);
    }
    ResumoEncontro resumoSql = resumosPorEncontro.get(encontro.getEncontroId());
    long presentes = valor(resumoSql == null ? null : resumoSql.getPresentes());
    long ausentes = valor(resumoSql == null ? null : resumoSql.getAusentes());
    int goe = valorInt(resumoSql == null ? null : resumoSql.getGoe());
    int quantidadeVisitantes =
        valorInt(resumoSql == null ? null : resumoSql.getVisitantesNominais())
            + visitantesPorEncontro.getOrDefault(encontro.getEncontroId(), 0);
    String observacaoEstrutura =
        observacoesEstrutura.get(
            new ChaveObservacao(encontro.getData(), encontro.getDiscipuladoId()));
    return new RelatorioEncontro(
        encontro.getEncontroId(),
        encontro.getData(),
        situacao,
        null,
        encontro.getObservacao(),
        Boolean.TRUE.equals(encontro.getFechamentoAutomatico()),
        new GerenciaInfo(encontro.getGerenciaId(), encontro.getGerenciaNome()),
        new DiscipuladoInfo(
            encontro.getDiscipuladoId(), encontro.getDiscipuladoNome(), encontro.getSexo()),
        new LiderInfo(encontro.getDiscipuladorId(), encontro.getDiscipuladorNome()),
        encontro.getGerenteNome(),
        coLideresPorDiscipulado.getOrDefault(encontro.getDiscipuladoId(), List.of()),
        participantesPorEncontro.getOrDefault(encontro.getEncontroId(), List.of()),
        quantidadeVisitantes,
        goe,
        observacaoEstrutura,
        new ResumoFrequencia(
            presentes,
            ausentes,
            presentes + ausentes,
            quantidadeVisitantes,
            goe,
            percentual(presentes, ausentes)));
  }

  private static RelatorioEncontro relatorioNaoRealizado(
      EncontroCabecalho encontro, Map<Long, List<LiderInfo>> coLideresPorDiscipulado) {
    return new RelatorioEncontro(
        encontro.getEncontroId(),
        encontro.getData(),
        SituacaoEncontro.NAO_REALIZADO,
        encontro.getJustificativa(),
        encontro.getObservacao(),
        Boolean.TRUE.equals(encontro.getFechamentoAutomatico()),
        new GerenciaInfo(encontro.getGerenciaId(), encontro.getGerenciaNome()),
        new DiscipuladoInfo(
            encontro.getDiscipuladoId(), encontro.getDiscipuladoNome(), encontro.getSexo()),
        new LiderInfo(encontro.getDiscipuladorId(), encontro.getDiscipuladorNome()),
        encontro.getGerenteNome(),
        coLideresPorDiscipulado.getOrDefault(encontro.getDiscipuladoId(), List.of()),
        List.of(),
        0,
        0,
        null,
        new ResumoFrequencia(0, 0, 0, 0, 0, BigDecimal.ZERO));
  }

  private Map<ChaveObservacao, String> observacoesEstrutura(List<EncontroCabecalho> encontrados) {
    List<Long> discipuladoIds =
        encontrados.stream().map(EncontroCabecalho::getDiscipuladoId).distinct().toList();
    LocalDate inicio =
        encontrados.stream()
            .map(EncontroCabecalho::getData)
            .min(Comparator.naturalOrder())
            .orElseThrow();
    LocalDate fim =
        encontrados.stream()
            .map(EncontroCabecalho::getData)
            .max(Comparator.naturalOrder())
            .orElseThrow();
    Map<ChaveObservacao, String> porChave = new LinkedHashMap<>();
    for (ObservacaoLiderancaRow row :
        relatorios.observacoesChamadaLideranca(inicio, fim, discipuladoIds)) {
      if (row.getObservacao() == null || row.getObservacao().isBlank()) continue;
      porChave.putIfAbsent(
          new ChaveObservacao(row.getData(), row.getDiscipuladoId()), row.getObservacao());
    }
    return porChave;
  }

  private Map<Long, ResumoEncontro> resumos(List<Long> encontroIds) {
    return relatorios.resumirPorEncontro(encontroIds).stream()
        .collect(Collectors.toMap(ResumoEncontro::getEncontroId, r -> r));
  }

  private Map<Long, Integer> visitantes(List<Long> encontroIds) {
    return relatorios.contarVisitantesPorEncontro(encontroIds).stream()
        .collect(
            Collectors.toMap(
                VisitantesPorEncontro::getEncontroId, v -> valorInt(v.getVisitantes())));
  }

  private Map<Long, List<LiderInfo>> coLideres(List<Long> discipuladoIds) {
    if (discipuladoIds.isEmpty()) return Map.of();
    Map<Long, List<LiderInfo>> porDiscipulado = new LinkedHashMap<>();
    for (CoLiderRow row : relatorios.coLideresPorDiscipulado(discipuladoIds)) {
      porDiscipulado
          .computeIfAbsent(row.getDiscipuladoId(), id -> new ArrayList<>())
          .add(new LiderInfo(row.getUsuarioId(), row.getNome()));
    }
    porDiscipulado.replaceAll(
        (id, lideres) ->
            lideres.stream()
                .sorted(Comparator.comparing(LiderInfo::nome, String.CASE_INSENSITIVE_ORDER))
                .toList());
    return porDiscipulado;
  }

  private Map<Long, List<ParticipanteInfo>> participantes(List<Long> encontroIds) {
    Map<Long, List<ParticipanteInfo>> porEncontro = new LinkedHashMap<>();
    for (ParticipanteRow row : relatorios.participantesPorEncontro(encontroIds)) {
      porEncontro
          .computeIfAbsent(row.getEncontroId(), id -> new ArrayList<>())
          .add(
              new ParticipanteInfo(
                  row.getAdolescenteId(),
                  row.getNome(),
                  row.getTelefone(),
                  SituacaoFrequencia.valueOf(row.getSituacao())));
    }
    porEncontro.replaceAll(
        (id, itens) ->
            itens.stream()
                .sorted(Comparator.comparing(ParticipanteInfo::nome, String.CASE_INSENSITIVE_ORDER))
                .toList());
    return porEncontro;
  }

  private static long valor(Number n) {
    return n == null ? 0 : n.longValue();
  }

  private static int valorInt(Number n) {
    return n == null ? 0 : n.intValue();
  }

  private static String texto(String valor) {
    return valor == null ? "" : valor;
  }

  private static BigDecimal percentual(long presentes, long ausentes) {
    long total = presentes + ausentes;
    return total == 0
        ? BigDecimal.ZERO
        : BigDecimal.valueOf(presentes)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
  }

  private record Escopo(Set<Long> discipuladoIds) {}

  private record ChaveObservacao(LocalDate data, Long discipuladoId) {}

  public record RelatorioDiarioResponse(
      LocalDate data, Instant emitidoEm, List<RelatorioEncontro> relatorios) {}

  public record RelatorioPeriodoResponse(
      LocalDate dataInicio,
      LocalDate dataFim,
      Instant emitidoEm,
      List<RelatorioEncontro> relatorios) {}

  public record RelatorioEncontro(
      long encontroId,
      LocalDate data,
      SituacaoEncontro situacao,
      String justificativa,
      String observacao,
      boolean fechamentoAutomatico,
      GerenciaInfo gerencia,
      DiscipuladoInfo discipulado,
      LiderInfo discipulador,
      String gerenteNome,
      List<LiderInfo> coLideres,
      List<ParticipanteInfo> participantes,
      int visitantes,
      int goe,
      String observacaoEstrutura,
      ResumoFrequencia resumo) {}

  public record GerenciaInfo(long id, String nome) {}

  public record DiscipuladoInfo(long id, String nome, String sexo) {}

  public record LiderInfo(long id, String nome) {}

  public record ParticipanteInfo(
      long adolescenteId, String nome, String telefone, SituacaoFrequencia situacao) {}

  public record ResumoFrequencia(
      long presentes,
      long ausentes,
      long participantes,
      int visitantes,
      int goe,
      BigDecimal percentualPresenca) {}
}
