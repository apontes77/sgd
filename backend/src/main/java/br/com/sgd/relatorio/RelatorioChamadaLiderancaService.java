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
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import br.com.sgd.frequencia.SituacaoFrequencia;
import br.com.sgd.lideranca.ChamadaLideranca;
import br.com.sgd.lideranca.ChamadaLiderancaDiscipulado;
import br.com.sgd.lideranca.ChamadaLiderancaRepository;
import br.com.sgd.lideranca.PapelLideranca;
import br.com.sgd.lideranca.PresencaLideranca;
import br.com.sgd.organizacao.Discipulado;
import br.com.sgd.organizacao.DiscipuladoRepository;
import br.com.sgd.user.Role;
import br.com.sgd.user.User;

@Service
@Transactional(readOnly = true)
public class RelatorioChamadaLiderancaService {
  private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  private final ChamadaLiderancaRepository chamadas;
  private final DiscipuladoRepository discipulados;
  private final Clock clock;

  public RelatorioChamadaLiderancaService(
      ChamadaLiderancaRepository chamadas, DiscipuladoRepository discipulados, Clock clock) {
    this.chamadas = chamadas;
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
    List<ChamadaLideranca> encontradas = chamadas.findAllByDataBetweenOrderByDataAsc(inicio, fim);
    return new RelatorioPeriodoResponse(
        inicio, fim, clock.instant(), montarRelatorios(encontradas, discipuladoId));
  }

  public void exportarExcel(
      User usuario, LocalDate inicio, LocalDate fim, Long discipuladoId, OutputStream out)
      throws IOException {
    RelatorioPeriodoResponse periodo = consultar(usuario, inicio, fim, discipuladoId);
    try (XSSFWorkbook workbook = new XSSFWorkbook()) {
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
    }
  }

  private List<RelatorioChamada> montarRelatorios(
      List<ChamadaLideranca> encontradas, Long discipuladoId) {
    List<RelatorioChamada> resultado = new ArrayList<>();
    for (ChamadaLideranca chamada : encontradas) {
      List<DiscipuladoRelatorio> discipuladosRelatorio =
          chamada.getItens().stream()
              .filter(
                  item ->
                      discipuladoId == null || item.getDiscipulado().getId().equals(discipuladoId))
              .sorted(ordemDiscipulado())
              .map(RelatorioChamadaLiderancaService::montarDiscipulado)
              .toList();
      if (discipuladoId != null && discipuladosRelatorio.isEmpty()) continue;
      resultado.add(
          new RelatorioChamada(
              chamada.getId(),
              chamada.getData(),
              chamada.getObservacaoGeral(),
              discipuladosRelatorio,
              resumir(discipuladosRelatorio)));
    }
    return resultado;
  }

  private static DiscipuladoRelatorio montarDiscipulado(ChamadaLiderancaDiscipulado item) {
    Discipulado discipulado = item.getDiscipulado();
    List<PresencaRelatorio> presencas =
        item.getPresencas().stream()
            .sorted(ordemPresenca())
            .map(
                p ->
                    new PresencaRelatorio(
                        p.getUsuario().getId(),
                        p.getUsuario().getNome(),
                        p.getPapel(),
                        p.getSituacao()))
            .toList();
    return new DiscipuladoRelatorio(
        discipulado.getId(),
        discipulado.getNome(),
        discipulado.getSexo().name(),
        discipulado.getGerencia().getNome(),
        item.getObservacao(),
        presencas);
  }

  private static ResumoChamada resumir(List<DiscipuladoRelatorio> discipuladosRelatorio) {
    long presentes =
        discipuladosRelatorio.stream()
            .flatMap(d -> d.presencas().stream())
            .filter(p -> p.situacao() == SituacaoFrequencia.PRESENTE)
            .count();
    long ausentes =
        discipuladosRelatorio.stream()
            .flatMap(d -> d.presencas().stream())
            .filter(p -> p.situacao() == SituacaoFrequencia.AUSENTE)
            .count();
    long participantes = presentes + ausentes;
    return new ResumoChamada(presentes, ausentes, participantes, percentual(presentes, ausentes));
  }

  private static Comparator<ChamadaLiderancaDiscipulado> ordemDiscipulado() {
    return Comparator.comparing(
            (ChamadaLiderancaDiscipulado item) -> item.getDiscipulado().getGerencia().getNome(),
            String.CASE_INSENSITIVE_ORDER)
        .thenComparing(item -> item.getDiscipulado().getNome(), String.CASE_INSENSITIVE_ORDER);
  }

  private static Comparator<PresencaLideranca> ordemPresenca() {
    return Comparator.comparing(
            (PresencaLideranca p) -> p.getPapel() != PapelLideranca.DISCIPULADOR)
        .thenComparing(p -> p.getUsuario().getNome(), String.CASE_INSENSITIVE_ORDER);
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
