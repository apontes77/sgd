package br.com.sgd.relatorio;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import br.com.sgd.frequencia.SituacaoFrequencia;
import br.com.sgd.lideranca.PapelLideranca;
import br.com.sgd.organizacao.DiscipuladoRepository;
import br.com.sgd.relatorio.RelatorioChamadaLiderancaRepository.CabecalhoChamada;
import br.com.sgd.relatorio.RelatorioChamadaLiderancaRepository.PresencaRow;
import br.com.sgd.relatorio.RelatorioChamadaLiderancaRepository.ResumoChamadaSql;
import br.com.sgd.user.Role;
import br.com.sgd.user.User;

@Service
@Transactional(readOnly = true)
public class RelatorioChamadaLiderancaService {
  private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  private final RelatorioChamadaLiderancaRepository relatorios;
  private final DiscipuladoRepository discipulados;
  private final Clock clock;

  public RelatorioChamadaLiderancaService(
      RelatorioChamadaLiderancaRepository relatorios,
      DiscipuladoRepository discipulados,
      Clock clock) {
    this.relatorios = relatorios;
    this.discipulados = discipulados;
    this.clock = clock;
  }

  public RelatorioPeriodoResponse consultar(
      User usuario, LocalDate inicio, LocalDate fim, Long discipuladoId) {
    exigirAdmin(usuario);
    validarPeriodo(inicio, fim);
    if (discipuladoId != null && !discipulados.existsById(discipuladoId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Discipulado não encontrado.");
    }
    return new RelatorioPeriodoResponse(
        inicio, fim, clock.instant(), montarRelatorios(inicio, fim, discipuladoId));
  }

  public void exportarExcel(
      User usuario, LocalDate inicio, LocalDate fim, Long discipuladoId, OutputStream out)
      throws IOException {
    RelatorioPeriodoResponse periodo = consultar(usuario, inicio, fim, discipuladoId);
    try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
      Sheet sheet = workbook.createSheet("Chamada de liderança");
      Row cabecalho = sheet.createRow(0);
      String[] colunas = {
        "Data",
        "Gerência",
        "Discipulado",
        "Nome",
        "Papel",
        "Situação",
        "Observação do discipulado",
        "Observação geral"
      };
      for (int i = 0; i < colunas.length; i++) {
        cabecalho.createCell(i).setCellValue(colunas[i]);
      }
      int linha = 1;
      for (RelatorioChamada item : periodo.relatorios()) {
        for (DiscipuladoRelatorio discipulado : item.discipulados()) {
          for (PresencaRelatorio presenca : discipulado.presencas()) {
            Row row = sheet.createRow(linha++);
            row.createCell(0).setCellValue(item.data().format(DATA_BR));
            row.createCell(1).setCellValue(discipulado.gerenciaNome());
            row.createCell(2).setCellValue(discipulado.discipuladoNome());
            row.createCell(3).setCellValue(presenca.nome());
            row.createCell(4).setCellValue(rotuloPapel(presenca.papel()));
            row.createCell(5).setCellValue(rotuloSituacao(presenca.situacao()));
            row.createCell(6).setCellValue(texto(discipulado.observacao()));
            row.createCell(7).setCellValue(texto(item.observacaoGeral()));
          }
        }
      }
      workbook.write(out);
      workbook.dispose();
    }
  }

  private List<RelatorioChamada> montarRelatorios(
      LocalDate inicio, LocalDate fim, Long discipuladoId) {
    Map<ItemChave, List<PresencaRelatorio>> presencasPorItem = new LinkedHashMap<>();
    for (PresencaRow row : relatorios.presencas(inicio, fim, discipuladoId)) {
      presencasPorItem
          .computeIfAbsent(
              new ItemChave(row.getChamadaId(), row.getDiscipuladoId()), k -> new ArrayList<>())
          .add(
              new PresencaRelatorio(
                  row.getUsuarioId(),
                  row.getNome(),
                  PapelLideranca.valueOf(row.getPapel()),
                  SituacaoFrequencia.valueOf(row.getSituacao())));
    }
    presencasPorItem.replaceAll((chave, lista) -> lista.stream().sorted(ordemPresenca()).toList());

    Map<Long, ResumoChamadaSql> resumos =
        relatorios.resumir(inicio, fim, discipuladoId).stream()
            .collect(Collectors.toMap(ResumoChamadaSql::getChamadaId, r -> r));

    Map<Long, RelatorioChamadaBuilder> porChamada = new LinkedHashMap<>();
    for (CabecalhoChamada row : relatorios.cabecalhos(inicio, fim, discipuladoId)) {
      RelatorioChamadaBuilder builder =
          porChamada.computeIfAbsent(
              row.getChamadaId(),
              id -> new RelatorioChamadaBuilder(id, row.getData(), row.getObservacaoGeral()));
      if (row.getDiscipuladoId() == null) continue;
      builder.discipulados.add(
          new DiscipuladoRelatorio(
              row.getDiscipuladoId(),
              row.getDiscipuladoNome(),
              row.getSexo(),
              row.getGerenciaNome(),
              row.getObservacao(),
              presencasPorItem.getOrDefault(
                  new ItemChave(row.getChamadaId(), row.getDiscipuladoId()), List.of())));
    }

    List<RelatorioChamada> resultado = new ArrayList<>();
    for (RelatorioChamadaBuilder builder : porChamada.values()) {
      if (discipuladoId != null && builder.discipulados.isEmpty()) continue;
      ResumoChamadaSql sql = resumos.get(builder.chamadaId);
      long presentes = valor(sql == null ? null : sql.getPresentes());
      long ausentes = valor(sql == null ? null : sql.getAusentes());
      resultado.add(
          new RelatorioChamada(
              builder.chamadaId,
              builder.data,
              builder.observacaoGeral,
              List.copyOf(builder.discipulados),
              new ResumoChamada(
                  presentes, ausentes, presentes + ausentes, percentual(presentes, ausentes))));
    }
    return resultado;
  }

  private record ItemChave(Long chamadaId, Long discipuladoId) {}

  private static Comparator<PresencaRelatorio> ordemPresenca() {
    return Comparator.comparing((PresencaRelatorio p) -> p.papel() != PapelLideranca.DISCIPULADOR)
        .thenComparing(PresencaRelatorio::nome, String.CASE_INSENSITIVE_ORDER);
  }

  private static void validarPeriodo(LocalDate inicio, LocalDate fim) {
    if (inicio == null || fim == null)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "As datas inicial e final são obrigatórias.");
    if (inicio.isAfter(fim))
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "A data inicial não pode ser posterior à data final.");
    if (fim.isAfter(inicio.plusMonths(12)))
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "O período do relatório deve ser de no máximo 12 meses.");
  }

  private static void exigirAdmin(User usuario) {
    if (usuario == null || !usuario.getPerfis().contains(Role.ADMIN))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado.");
  }

  private static long valor(Number n) {
    return n == null ? 0 : n.longValue();
  }

  private static BigDecimal percentual(long presentes, long ausentes) {
    long total = presentes + ausentes;
    return total == 0
        ? BigDecimal.ZERO
        : BigDecimal.valueOf(presentes)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
  }

  private static String rotuloPapel(PapelLideranca papel) {
    return papel == PapelLideranca.DISCIPULADOR ? "Discipulador" : "Co-líder";
  }

  private static String rotuloSituacao(SituacaoFrequencia situacao) {
    return situacao == SituacaoFrequencia.PRESENTE ? "Presente" : "Ausente";
  }

  private static String texto(String valor) {
    return valor == null ? "" : valor;
  }

  private static final class RelatorioChamadaBuilder {
    private final Long chamadaId;
    private final LocalDate data;
    private final String observacaoGeral;
    private final List<DiscipuladoRelatorio> discipulados = new ArrayList<>();

    private RelatorioChamadaBuilder(Long chamadaId, LocalDate data, String observacaoGeral) {
      this.chamadaId = chamadaId;
      this.data = data;
      this.observacaoGeral = observacaoGeral;
    }
  }

  public record RelatorioPeriodoResponse(
      LocalDate dataInicio,
      LocalDate dataFim,
      Instant emitidoEm,
      List<RelatorioChamada> relatorios) {}

  public record RelatorioChamada(
      Long chamadaId,
      LocalDate data,
      String observacaoGeral,
      List<DiscipuladoRelatorio> discipulados,
      ResumoChamada resumo) {}

  public record DiscipuladoRelatorio(
      long discipuladoId,
      String discipuladoNome,
      String sexo,
      String gerenciaNome,
      String observacao,
      List<PresencaRelatorio> presencas) {}

  public record PresencaRelatorio(
      long usuarioId, String nome, PapelLideranca papel, SituacaoFrequencia situacao) {}

  public record ResumoChamada(
      long presentes, long ausentes, long participantes, BigDecimal percentualPresenca) {}
}
