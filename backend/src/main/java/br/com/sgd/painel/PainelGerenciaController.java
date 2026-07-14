package br.com.sgd.painel;

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
@RequestMapping("/api/v1/painel/gerencia")
@PreAuthorize("hasRole('GERENTE')")
public class PainelGerenciaController {
    private final PainelGerenciaService service;
    public PainelGerenciaController(PainelGerenciaService service) { this.service = service; }

    @GetMapping
    public PainelGerenciaService.PainelGerenciaResponse consultar(@AuthenticationPrincipal User usuario,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        return service.consultar(usuario, dataInicio, dataFim);
    }
}
