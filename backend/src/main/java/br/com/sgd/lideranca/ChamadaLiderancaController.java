package br.com.sgd.lideranca;

import java.time.LocalDate;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.sgd.frequencia.SituacaoFrequencia;
import br.com.sgd.user.User;

@RestController
@RequestMapping("/api/v1/chamadas-lideranca")
@PreAuthorize("hasRole('ADMIN')")
public class ChamadaLiderancaController {
  private final ChamadaLiderancaService service;

  public ChamadaLiderancaController(ChamadaLiderancaService service) {
    this.service = service;
  }

  @GetMapping
  public ChamadaLiderancaService.ChamadaLiderancaResponse consultar(
      @AuthenticationPrincipal User usuario,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
    return service.consultar(usuario, data);
  }

  @PutMapping
  public ChamadaLiderancaService.ChamadaLiderancaResponse salvar(
      @AuthenticationPrincipal User usuario, @Valid @RequestBody SalvarRequest body) {
    return service.salvar(
        usuario,
        new ChamadaLiderancaService.SalvarChamadaLiderancaCommand(
            body.data(),
            body.observacaoGeral(),
            body.discipulados().stream()
                .map(
                    d ->
                        new ChamadaLiderancaService.DiscipuladoChamadaCommand(
                            d.discipuladoId(),
                            d.observacao(),
                            d.presencas().stream()
                                .map(
                                    p ->
                                        new ChamadaLiderancaService.PresencaCommand(
                                            p.usuarioId(), p.papel(), p.situacao()))
                                .toList()))
                .toList()));
  }

  public record SalvarRequest(
      @NotNull LocalDate data,
      @Size(max = 1000) String observacaoGeral,
      @NotNull List<@NotNull @Valid DiscipuladoRequest> discipulados) {}

  public record DiscipuladoRequest(
      @NotNull Long discipuladoId,
      @Size(max = 500) String observacao,
      @NotNull List<@NotNull @Valid PresencaRequest> presencas) {}

  public record PresencaRequest(
      @NotNull Long usuarioId,
      @NotNull PapelLideranca papel,
      @NotNull SituacaoFrequencia situacao) {}
}
