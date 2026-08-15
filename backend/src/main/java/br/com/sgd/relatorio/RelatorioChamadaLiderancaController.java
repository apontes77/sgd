package br.com.sgd.relatorio;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.sgd.user.User;

@RestController
@RequestMapping("/api/v1/relatorios")
@PreAuthorize("hasRole('ADMIN')")
public class RelatorioChamadaLiderancaController {
  private static final ZoneId ZONA_NEGOCIO = ZoneId.of("America/Sao_Paulo");
  private static final String XLSX =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  private final RelatorioChamadaLiderancaService service;

  public RelatorioChamadaLiderancaController(RelatorioChamadaLiderancaService service) {
    this.service = service;
  }

  @GetMapping("/chamadas-lideranca")
  public RelatorioChamadaLiderancaService.RelatorioPeriodoResponse consultar(
      @AuthenticationPrincipal User usuario,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
      @RequestParam(required = false) Long discipuladoId) {
    return service.consultar(usuario, dataInicio, dataFim, discipuladoId);
  }

  @GetMapping(value = "/chamadas-lideranca/export", produces = XLSX)
  public void exportar(
      @AuthenticationPrincipal User usuario,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
      @RequestParam(required = false) Long discipuladoId,
      HttpServletResponse response)
      throws IOException {
    LocalDate hoje = LocalDate.now(ZONA_NEGOCIO);
    response.setContentType(XLSX);
    response.setHeader(
        HttpHeaders.CONTENT_DISPOSITION,
        "attachment; filename=\"chamada-lideranca-" + hoje + ".xlsx\"");
    service.exportarExcel(usuario, dataInicio, dataFim, discipuladoId, response.getOutputStream());
  }
}
