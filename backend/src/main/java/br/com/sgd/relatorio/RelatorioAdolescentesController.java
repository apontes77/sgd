package br.com.sgd.relatorio;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import jakarta.servlet.http.HttpServletResponse;

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
public class RelatorioAdolescentesController {
  private static final ZoneId ZONA_NEGOCIO = ZoneId.of("America/Sao_Paulo");

  private final RelatorioAdolescentesService service;

  public RelatorioAdolescentesController(RelatorioAdolescentesService service) {
    this.service = service;
  }

  @GetMapping(value = "/adolescentes/export", produces = "text/csv")
  @PreAuthorize("hasRole('ADMIN')")
  public void exportar(
      @AuthenticationPrincipal User usuario,
      @RequestParam(required = false) Long discipuladoId,
      @RequestParam(required = false) Boolean ativo,
      HttpServletResponse response)
      throws IOException {
    LocalDate hoje = LocalDate.now(ZONA_NEGOCIO);
    response.setCharacterEncoding("UTF-8");
    response.setContentType("text/csv; charset=UTF-8");
    response.setHeader(
        HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"adolescentes-" + hoje + ".csv\"");
    service.exportarCsv(usuario, discipuladoId, ativo, response.getOutputStream());
  }
}
