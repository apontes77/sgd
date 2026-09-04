package br.com.sgd.organizacao;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import br.com.sgd.auth.AuthController;
import br.com.sgd.common.PaginaResponse;
import br.com.sgd.user.Role;
import br.com.sgd.user.User;

@RestController
@RequestMapping("/api/v1")
@Validated
public class OrganizacaoController {
  private final GerenciaService gerencias;
  private final DiscipuladoService discipulados;

  public OrganizacaoController(GerenciaService gerencias, DiscipuladoService discipulados) {
    this.gerencias = gerencias;
    this.discipulados = discipulados;
  }

  @GetMapping("/gerencias")
  public PaginaResponse<GerenciaResponse> listarGerencias(
      @RequestParam(required = false) Sexo sexo,
      @RequestParam(required = false) FaixaEtaria faixaEtaria,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return PaginaResponse.of(
        gerencias.list(sexo, faixaEtaria, PageRequest.of(page, size)).map(GerenciaResponse::of));
  }

  @PostMapping("/gerencias")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('ADMIN')")
  public GerenciaResponse criarGerencia(@Valid @RequestBody GerenciaRequest r) {
    return GerenciaResponse.of(
        gerencias.create(r.nome(), r.sexo(), r.faixasEtarias(), r.gerenteId()));
  }

  @PatchMapping("/gerencias/{gerenciaId}")
  @PreAuthorize("hasRole('ADMIN')")
  public GerenciaResponse atualizarGerencia(
      @PathVariable long gerenciaId, @Valid @RequestBody AtualizarGerenciaRequest r) {
    return GerenciaResponse.of(
        gerencias.update(
            gerenciaId, r.nome(), r.sexo(), r.faixasEtarias(), r.gerenteId(), r.ativo()));
  }

  @DeleteMapping("/gerencias/{gerenciaId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasRole('ADMIN')")
  public void excluirGerencia(@PathVariable long gerenciaId) {
    gerencias.delete(gerenciaId);
  }

  @GetMapping("/discipulados")
  public PaginaResponse<DiscipuladoResponse> listarDiscipulados(
      Authentication auth,
      @RequestParam(required = false) Long gerenciaId,
      @RequestParam(required = false) Boolean ativo,
      @RequestParam(required = false) Boolean emFormacao,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return PaginaResponse.of(
        discipulados
            .list(usuario(auth), gerenciaId, ativo, emFormacao, PageRequest.of(page, size))
            .map(DiscipuladoResponse::of));
  }

  @GetMapping("/discipulados/liderados")
  @PreAuthorize("hasAnyRole('DISCIPULADOR','CO_LIDER')")
  public List<DiscipuladoResponse> listarDiscipuladosLiderados(
      @AuthenticationPrincipal User usuario, @RequestParam(required = false) Boolean ativo) {
    return discipulados.listLiderados(usuario, ativo).stream()
        .map(DiscipuladoResponse::of)
        .toList();
  }

  @PostMapping("/discipulados")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('ADMIN')")
  public DiscipuladoResponse criarDiscipulado(@Valid @RequestBody DiscipuladoRequest r) {
    return DiscipuladoResponse.of(
        discipulados.create(
            r.nome(),
            r.sexo(),
            r.faixaEtaria(),
            r.gerenciaId(),
            r.discipuladorId(),
            Boolean.TRUE.equals(r.emFormacao())));
  }

  @PatchMapping("/discipulados/{discipuladoId}")
  @PreAuthorize("hasRole('ADMIN')")
  public DiscipuladoResponse atualizarDiscipulado(
      @PathVariable long discipuladoId, @Valid @RequestBody AtualizarDiscipuladoRequest r) {
    return DiscipuladoResponse.of(
        discipulados.update(
            discipuladoId,
            r.nome(),
            r.sexo(),
            r.faixaEtaria(),
            r.gerenciaId(),
            r.discipuladorId(),
            r.ativo()));
  }

  @PutMapping("/discipulados/{discipuladoId}/co-lideres")
  @PreAuthorize("hasRole('ADMIN')")
  public DiscipuladoResponse definirCoLideres(
      @PathVariable long discipuladoId, @Valid @RequestBody CoLideresRequest r) {
    return DiscipuladoResponse.of(discipulados.replaceCoLideres(discipuladoId, r.usuarioIds()));
  }

  public record GerenciaRequest(
      @NotBlank String nome,
      @NotNull Sexo sexo,
      @NotEmpty Set<FaixaEtaria> faixasEtarias,
      @NotNull Long gerenteId) {}

  public record AtualizarGerenciaRequest(
      String nome, Sexo sexo, Set<FaixaEtaria> faixasEtarias, Long gerenteId, Boolean ativo) {}

  private static User usuario(Authentication auth) {
    if (auth.getPrincipal() instanceof User usuario) return usuario;
    Set<Role> perfis =
        auth.getAuthorities().stream()
            .map(a -> a.getAuthority())
            .filter(a -> a.startsWith("ROLE_"))
            .map(a -> Role.valueOf(a.substring(5)))
            .collect(java.util.stream.Collectors.toSet());
    return new User("Usuario autenticado", "mock@sgd.local", "hash-de-teste", perfis);
  }

  public record DiscipuladoRequest(
      @NotBlank String nome,
      @NotNull Sexo sexo,
      @NotNull FaixaEtaria faixaEtaria,
      Long gerenciaId,
      @NotNull Long discipuladorId,
      Boolean emFormacao) {}

  public record AtualizarDiscipuladoRequest(
      String nome,
      Sexo sexo,
      FaixaEtaria faixaEtaria,
      Long gerenciaId,
      Long discipuladorId,
      Boolean ativo) {}

  public record CoLideresRequest(@NotNull @Size(max = 2) List<Long> usuarioIds) {}

  public record GerenciaResponse(
      Long id,
      String nome,
      Sexo sexo,
      Set<FaixaEtaria> faixasEtarias,
      Long gerenteId,
      boolean ativo) {
    static GerenciaResponse of(Gerencia g) {
      return new GerenciaResponse(
          g.getId(),
          g.getNome(),
          g.getSexo(),
          new LinkedHashSet<>(g.getFaixasEtarias()),
          g.getGerente().getId(),
          g.isAtivo());
    }
  }

  public record DiscipuladoResponse(
      Long id,
      String nome,
      Sexo sexo,
      FaixaEtaria faixaEtaria,
      boolean ativo,
      boolean emFormacao,
      Long gerenciaId,
      Long discipuladorId,
      String discipuladorNome,
      List<AuthController.UserResponse> coLideres) {
    static DiscipuladoResponse of(Discipulado d) {
      return new DiscipuladoResponse(
          d.getId(),
          d.getNome(),
          d.getSexo(),
          d.getFaixaEtaria(),
          d.isAtivo(),
          d.isEmFormacao(),
          d.getGerencia() == null ? null : d.getGerencia().getId(),
          d.getDiscipulador().getId(),
          d.getDiscipulador().getNome(),
          d.getCoLideres().stream().map(AuthController.UserResponse::of).toList());
    }
  }
}
