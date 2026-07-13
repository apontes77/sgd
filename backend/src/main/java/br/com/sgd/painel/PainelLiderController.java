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
@RequestMapping("/api/v1/painel/lider")
@PreAuthorize("hasAnyRole('DISCIPULADOR','CO_LIDER')")
public class PainelLiderController {
    private final PainelLiderService service;
    public PainelLiderController(PainelLiderService service) { this.service = service; }

    @GetMapping
    public PainelLiderService.PainelLiderResponse consultar(@AuthenticationPrincipal User usuario,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        return service.consultar(usuario, dataInicio, dataFim);
    }
}
