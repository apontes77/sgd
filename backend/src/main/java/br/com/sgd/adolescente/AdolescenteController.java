package br.com.sgd.adolescente;

import java.time.LocalDate;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import br.com.sgd.common.PaginaResponse;
import br.com.sgd.user.User;

@RestController
@RequestMapping("/api/v1/adolescentes")
@Validated
public class AdolescenteController {
  private final AdolescenteService service;

  public AdolescenteController(AdolescenteService service) {
    this.service = service;
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN','GERENTE','DISCIPULADOR','CO_LIDER')")
  public PaginaResponse<AdolescenteResponse> listar(
      Authentication auth,
      @RequestParam(required = false) Long discipuladoId,
      @RequestParam(required = false) Boolean ativo,
      @RequestParam(required = false) List<CategoriaAdolescente> categoria,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return PaginaResponse.of(
        service
            .listar(usuario(auth), discipuladoId, ativo, categoria, PageRequest.of(page, size))
            .map(AdolescenteController::resposta));
  }

  @GetMapping("/alertas-goe")
  @PreAuthorize("hasAnyRole('ADMIN','GERENTE','DISCIPULADOR','CO_LIDER')")
  public List<AlertaGoeResponse> alertasGoe(
      Authentication auth, @RequestParam @NotNull @Positive Long discipuladoId) {
    return service.listarAlertasGoe(usuario(auth), discipuladoId).stream()
        .map(a -> new AlertaGoeResponse(a.adolescenteId(), a.nome(), a.faltas()))
        .toList();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyRole('ADMIN','DISCIPULADOR','CO_LIDER')")
  public AdolescenteResponse criar(Authentication auth, @Valid @RequestBody AdolescenteRequest r) {
    return resposta(service.comVinculoAtual(service.criar(usuario(auth), r.dados())));
  }

  @PatchMapping("/{adolescenteId}")
  @PreAuthorize("hasAnyRole('ADMIN','DISCIPULADOR','CO_LIDER')")
  public AdolescenteResponse atualizar(
      Authentication auth,
      @PathVariable long adolescenteId,
      @Valid @RequestBody AdolescenteRequest r) {
    return resposta(
        service.comVinculoAtual(service.atualizar(usuario(auth), adolescenteId, r.dados())));
  }

  @PostMapping("/{adolescenteId}/vinculos")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyRole('ADMIN','DISCIPULADOR','CO_LIDER')")
  public VinculoResponse transferir(
      Authentication auth, @PathVariable long adolescenteId, @Valid @RequestBody VinculoRequest r) {
    return VinculoResponse.of(
        service.transferir(usuario(auth), adolescenteId, r.discipuladoId(), r.dataInicio()));
  }

  @DeleteMapping("/{adolescenteId}/dados-pessoais")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasRole('ADMIN')")
  public void anonimizar(Authentication auth, @PathVariable long adolescenteId) {
    service.anonimizar(usuario(auth), adolescenteId);
  }

  private static User usuario(Authentication auth) {
    return (User) auth.getPrincipal();
  }

  private static AdolescenteResponse resposta(AdolescenteService.AdolescenteComVinculo item) {
    Adolescente a = item.adolescente();
    return new AdolescenteResponse(
        a.getId(),
        a.getNome(),
        a.getDataNascimento(),
        a.getTelefone(),
        a.getInstagram(),
        a.getResponsavelNome(),
        a.getResponsavelTelefone(),
        a.getConsentimentoEm(),
        a.getCategoria() == null ? CategoriaAdolescente.DISCIPULO : a.getCategoria(),
        a.getNomeMae(),
        a.getTelefoneMae(),
        a.getNomePai(),
        a.getTelefonePai(),
        a.getEstrutura(),
        a.getMotivoAfastamento(),
        a.isAnonimizado(),
        item.discipuladoId(),
        item.discipuladoNome(),
        a.isAtivo());
  }

  public record AdolescenteRequest(
      @NotBlank @Size(max = 120) String nome,
      @NotNull @PastOrPresent LocalDate dataNascimento,
      @Size(max = 40) String telefone,
      @Size(max = 120) String instagram,
      @Size(max = 120) String responsavelNome,
      @Size(max = 40) String responsavelTelefone,
      @NotNull @PastOrPresent LocalDate consentimentoEm,
      @NotNull CategoriaAdolescente categoria,
      @Size(max = 120) String nomeMae,
      @Size(max = 40) String telefoneMae,
      @Size(max = 120) String nomePai,
      @Size(max = 40) String telefonePai,
      @Size(max = 120) String estrutura,
      @Size(max = 500) String motivoAfastamento,
      @NotNull @Positive Long discipuladoId,
      Boolean ativo,
      LocalDate dataInicio) {
    AdolescenteService.DadosAdolescente dados() {
      return new AdolescenteService.DadosAdolescente(
          nome,
          dataNascimento,
          telefone,
          instagram,
          responsavelNome,
          responsavelTelefone,
          consentimentoEm,
          categoria,
          nomeMae,
          telefoneMae,
          nomePai,
          telefonePai,
          estrutura,
          motivoAfastamento,
          discipuladoId,
          ativo,
          dataInicio);
    }
  }

  public record AdolescenteResponse(
      long id,
      String nome,
      LocalDate dataNascimento,
      String telefone,
      String instagram,
      String responsavelNome,
      String responsavelTelefone,
      LocalDate consentimentoEm,
      CategoriaAdolescente categoria,
      String nomeMae,
      String telefoneMae,
      String nomePai,
      String telefonePai,
      String estrutura,
      String motivoAfastamento,
      boolean anonimizado,
      long discipuladoId,
      String discipuladoNome,
      boolean ativo) {}

  public record AlertaGoeResponse(long adolescenteId, String nome, long faltas) {}

  public record VinculoRequest(
      @NotNull @Positive Long discipuladoId, @NotNull LocalDate dataInicio) {}

  public record VinculoResponse(
      long id,
      long adolescenteId,
      long discipuladoId,
      LocalDate dataInicio,
      LocalDate dataFim,
      boolean ativo) {
    static VinculoResponse of(VinculoAdolescenteDiscipulado v) {
      return new VinculoResponse(
          v.getId(),
          v.getAdolescente().getId(),
          v.getDiscipulado().getId(),
          v.getDataInicio(),
          v.getDataFim(),
          v.isAtivo());
    }
  }
}
