package br.com.sgd.familia;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import br.com.sgd.common.PaginaResponse;
import br.com.sgd.user.User;

@RestController
@Validated
public class FamiliaController {
  private final FamiliaService service;

  public FamiliaController(FamiliaService service) {
    this.service = service;
  }

  @GetMapping("/api/v1/familias")
  @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
  public PaginaResponse<FamiliaService.FamiliaResumo> listar(
      Authentication auth,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
      @RequestParam(required = false) String busca,
      @RequestParam(required = false) SituacaoIgrejaFamilia situacaoIgreja,
      @RequestParam(required = false) SituacaoPaisFamilia situacaoPais) {
    return PaginaResponse.of(
        service.listar(
            usuario(auth), PageRequest.of(page, size), busca, situacaoIgreja, situacaoPais));
  }

  @GetMapping("/api/v1/adolescentes/{adolescenteId}/familia")
  @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
  public FamiliaService.FamiliaResponse consultar(
      Authentication auth, @PathVariable long adolescenteId) {
    return service.consultarResponse(usuario(auth), adolescenteId);
  }

  @PutMapping("/api/v1/adolescentes/{adolescenteId}/familia")
  @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
  public FamiliaService.FamiliaResponse salvar(
      Authentication auth,
      @PathVariable long adolescenteId,
      @Valid @RequestBody FamiliaService.FamiliaRequest body) {
    if (body == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "A ficha de família é obrigatória.");
    }
    return service.salvarResponse(
        usuario(auth), adolescenteId, FamiliaService.dadosFromRequest(body));
  }

  private static User usuario(Authentication auth) {
    return (User) auth.getPrincipal();
  }
}
