package br.com.sgd.frequencia;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import br.com.sgd.controller.PaginaResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;

@RestController
@RequestMapping("/api/v1/auditoria")
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class AuditoriaController {
    private final AuditoriaConsultaRepository auditoria;
    private final ObjectMapper json;

    public AuditoriaController(AuditoriaConsultaRepository a, ObjectMapper j) {
        auditoria = a;
        json = j;
    }

    @GetMapping
    public PaginaResponse<AuditoriaResponse> listar(@RequestParam(required = false) String entidade, @RequestParam(required = false) Long usuarioId, @RequestParam(defaultValue = "0") @Min(0) int page, @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return PaginaResponse.of(auditoria.consultar(entidade, usuarioId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "data_hora"))).map(this::response));
    }

    private AuditoriaResponse response(AuditoriaConsultaRepository.LinhaAuditoria a) {
        Map<String, Object> detalhes;
        try {
            detalhes = a.getDetalhes() == null ? Map.of() : json.readValue(a.getDetalhes(), new TypeReference<>() {
            });
        } catch (Exception e) {
            detalhes = Map.of("texto", a.getDetalhes());
        }
        return new AuditoriaResponse(a.getId(), a.getUsuarioId(), a.getDataHora(), a.getEntidade(), a.getAcao(), detalhes);
    }

    public record AuditoriaResponse(Long id, Long usuarioId, java.time.Instant dataHora, String entidade, String acao,
                                    Map<String, Object> detalhes) {
    }
}
