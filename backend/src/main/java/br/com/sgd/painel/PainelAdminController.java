package br.com.sgd.painel;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/painel/admin")
@PreAuthorize("hasRole('ADMIN')")
public class PainelAdminController {
    private final PainelAdminService service;
    public PainelAdminController(PainelAdminService service) { this.service = service; }

    @GetMapping
    public PainelAdminService.PainelAdminResponse consultar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        return service.consultar(dataInicio, dataFim);
    }
}
