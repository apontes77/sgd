package br.com.sgd.controller;

import br.com.sgd.auth.AuthController;
import br.com.sgd.organizacao.Discipulado;
import br.com.sgd.organizacao.DiscipuladoService;
import br.com.sgd.organizacao.Gerencia;
import br.com.sgd.organizacao.GerenciaService;
import br.com.sgd.organizacao.Sexo;
import br.com.sgd.user.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.Set;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

@RestController
@RequestMapping("/api/v1")
@Validated
public class OrganizacaoController {
    private final GerenciaService gerencias; private final DiscipuladoService discipulados;
    public OrganizacaoController(GerenciaService gerencias, DiscipuladoService discipulados) { this.gerencias = gerencias; this.discipulados = discipulados; }
    @GetMapping("/gerencias") public PaginaResponse<GerenciaResponse> listarGerencias(@RequestParam(defaultValue = "0") @Min(0) int page, @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) { return PaginaResponse.of(gerencias.list(PageRequest.of(page, size)).map(GerenciaResponse::of)); }
    @PostMapping("/gerencias") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('ADMIN')") public GerenciaResponse criarGerencia(@Valid @RequestBody GerenciaRequest r) { return GerenciaResponse.of(gerencias.create(r.nome(), r.gerenteId())); }
    @PatchMapping("/gerencias/{gerenciaId}") @PreAuthorize("hasRole('ADMIN')") public GerenciaResponse atualizarGerencia(@PathVariable long gerenciaId, @Valid @RequestBody AtualizarGerenciaRequest r) { return GerenciaResponse.of(gerencias.update(gerenciaId, r.nome(), r.gerenteId(), r.ativo())); }
    @GetMapping("/discipulados") public PaginaResponse<DiscipuladoResponse> listarDiscipulados(@RequestParam(required = false) Long gerenciaId, @RequestParam(required = false) Boolean ativo, @RequestParam(defaultValue = "0") @Min(0) int page, @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) { return PaginaResponse.of(discipulados.list(gerenciaId, ativo, PageRequest.of(page, size)).map(DiscipuladoResponse::of)); }
    @GetMapping("/discipulados/liderados") @PreAuthorize("hasAnyRole('DISCIPULADOR','CO_LIDER')") public List<DiscipuladoResponse> listarDiscipuladosLiderados(@AuthenticationPrincipal User usuario, @RequestParam(required = false) Boolean ativo) { return discipulados.listLiderados(usuario, ativo).stream().map(DiscipuladoResponse::of).toList(); }
    @PostMapping("/discipulados") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('ADMIN')") public DiscipuladoResponse criarDiscipulado(@Valid @RequestBody DiscipuladoRequest r) { return DiscipuladoResponse.of(discipulados.create(r.nome(), r.sexo(), r.gerenciaId(), r.discipuladorId())); }
    @PatchMapping("/discipulados/{discipuladoId}") @PreAuthorize("hasRole('ADMIN')") public DiscipuladoResponse atualizarDiscipulado(@PathVariable long discipuladoId, @Valid @RequestBody AtualizarDiscipuladoRequest r) { return DiscipuladoResponse.of(discipulados.update(discipuladoId, r.nome(), r.sexo(), r.gerenciaId(), r.discipuladorId(), r.ativo())); }
    @PutMapping("/discipulados/{discipuladoId}/co-lideres") @PreAuthorize("hasRole('ADMIN')") public DiscipuladoResponse definirCoLideres(@PathVariable long discipuladoId, @Valid @RequestBody CoLideresRequest r) { return DiscipuladoResponse.of(discipulados.replaceCoLideres(discipuladoId, r.usuarioIds())); }
    public record GerenciaRequest(@NotBlank String nome, @NotNull Long gerenteId) { }
    public record AtualizarGerenciaRequest(String nome, Long gerenteId, Boolean ativo) { }
    public record DiscipuladoRequest(@NotBlank String nome, @NotNull Sexo sexo, @NotNull Long gerenciaId, @NotNull Long discipuladorId) { }
    public record AtualizarDiscipuladoRequest(String nome, Sexo sexo, Long gerenciaId, Long discipuladorId, Boolean ativo) { }
    public record CoLideresRequest(@NotNull @Size(max = 2) List<Long> usuarioIds) { }
    public record GerenciaResponse(Long id, String nome, Long gerenteId, boolean ativo) { static GerenciaResponse of(Gerencia g) { return new GerenciaResponse(g.getId(), g.getNome(), g.getGerente().getId(), g.isAtivo()); } }
    public record DiscipuladoResponse(Long id, String nome, Sexo sexo, boolean ativo, Long gerenciaId, Long discipuladorId, Set<AuthController.UserResponse> coLideres) { static DiscipuladoResponse of(Discipulado d) { return new DiscipuladoResponse(d.getId(), d.getNome(), d.getSexo(), d.isAtivo(), d.getGerencia().getId(), d.getDiscipulador().getId(), d.getCoLideres().stream().map(AuthController.UserResponse::of).collect(java.util.stream.Collectors.toSet())); } }
}
