package br.com.sgd.adolescente;

import br.com.sgd.controller.PaginaResponse;
import br.com.sgd.user.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/adolescentes")
@Validated
public class AdolescenteController {
    private final AdolescenteService service;
    public AdolescenteController(AdolescenteService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','DISCIPULADOR','CO_LIDER')")
    public PaginaResponse<AdolescenteResponse> listar(Authentication auth, @RequestParam(required = false) Long discipuladoId,
            @RequestParam(required = false) Boolean ativo, @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return PaginaResponse.of(service.listar(usuario(auth), discipuladoId, ativo, PageRequest.of(page, size)).map(a -> resposta(a, service.vinculoAtual(a.getId()).getDiscipulado().getId())));
    }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','DISCIPULADOR','CO_LIDER')")
    public AdolescenteResponse criar(Authentication auth, @Valid @RequestBody AdolescenteRequest r) {
        var a = service.criar(usuario(auth), r.dados()); return resposta(a, r.discipuladoId());
    }

    @PatchMapping("/{adolescenteId}")
    @PreAuthorize("hasAnyRole('ADMIN','DISCIPULADOR','CO_LIDER')")
    public AdolescenteResponse atualizar(Authentication auth, @PathVariable long adolescenteId, @Valid @RequestBody AdolescenteRequest r) {
        return resposta(service.atualizar(usuario(auth), adolescenteId, r.dados()), r.discipuladoId());
    }

    @PostMapping("/{adolescenteId}/vinculos") @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','DISCIPULADOR','CO_LIDER')")
    public VinculoResponse transferir(Authentication auth, @PathVariable long adolescenteId, @Valid @RequestBody VinculoRequest r) {
        return VinculoResponse.of(service.transferir(usuario(auth), adolescenteId, r.discipuladoId(), r.dataInicio()));
    }

    private static User usuario(Authentication auth) { return (User) auth.getPrincipal(); }
    private static AdolescenteResponse resposta(Adolescente a, long discipuladoId) { return new AdolescenteResponse(a.getId(), a.getNome(), a.getDataNascimento(), a.getTelefone(), a.getInstagram(), discipuladoId, a.isAtivo()); }

    public record AdolescenteRequest(@NotBlank @Size(max=120) String nome, @NotNull @PastOrPresent LocalDate dataNascimento,
            @Size(max=40) String telefone, @Size(max=120) String instagram, @NotNull @Positive Long discipuladoId, Boolean ativo) {
        AdolescenteService.DadosAdolescente dados() { return new AdolescenteService.DadosAdolescente(nome, dataNascimento, telefone, instagram, discipuladoId, ativo); }
    }
    public record AdolescenteResponse(long id, String nome, LocalDate dataNascimento, String telefone, String instagram, long discipuladoId, boolean ativo) { }
    public record VinculoRequest(@NotNull @Positive Long discipuladoId, @NotNull LocalDate dataInicio) { }
    public record VinculoResponse(long id, long adolescenteId, long discipuladoId, LocalDate dataInicio, LocalDate dataFim, boolean ativo) {
        static VinculoResponse of(VinculoAdolescenteDiscipulado v) { return new VinculoResponse(v.getId(), v.getAdolescente().getId(), v.getDiscipulado().getId(), v.getDataInicio(), v.getDataFim(), v.isAtivo()); }
    }
}
