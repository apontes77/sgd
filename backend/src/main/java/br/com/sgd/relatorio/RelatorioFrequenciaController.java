package br.com.sgd.relatorio;

import br.com.sgd.user.User;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/relatorios")
@PreAuthorize("hasAnyRole('ADMIN','GERENTE','DISCIPULADOR','CO_LIDER')")
public class RelatorioFrequenciaController {
    private final RelatorioFrequenciaService service;
    public RelatorioFrequenciaController(RelatorioFrequenciaService service) { this.service = service; }

    @GetMapping("/frequencia-diaria")
    public RelatorioFrequenciaService.RelatorioDiarioResponse consultar(@AuthenticationPrincipal User usuario,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return service.consultar(usuario, data);
    }

    @GetMapping("/frequencia")
    public RelatorioFrequenciaService.RelatorioPeriodoResponse consultarPeriodo(@AuthenticationPrincipal User usuario,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        return service.consultarPeriodo(usuario, dataInicio, dataFim);
    }
}
