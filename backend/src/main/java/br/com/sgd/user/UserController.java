package br.com.sgd.user;

import br.com.sgd.auth.AuthController.UserResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import br.com.sgd.controller.PaginaResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/usuarios") @Validated
public class UserController {
    private final UserService service; public UserController(UserService service) { this.service = service; }
    @GetMapping public PaginaResponse<UserResponse> list(@RequestParam(required = false) Boolean ativo,
            @RequestParam(defaultValue = "0") @Min(0) int page, @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        var result = service.list(ativo, PageRequest.of(page, size));
        return PaginaResponse.of(result.map(UserResponse::of));
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public UserResponse create(@Valid @RequestBody CreateUserRequest r) { return UserResponse.of(service.create(r.nome(), r.email(), r.perfis())); }
    @PostMapping("/{id}/reenviar-definicao-senha") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resendSetup(@PathVariable long id) { service.resendInitialSetup(id); }
    @PatchMapping("/{id}") public UserResponse update(@PathVariable long id, @Valid @RequestBody UpdateUserRequest r) { return UserResponse.of(service.update(id, r.nome(), r.perfis(), r.ativo())); }
    public record CreateUserRequest(@NotBlank @Size(max = 120) String nome, @Email @NotBlank String email, @NotEmpty Set<Role> perfis) { }
    public record UpdateUserRequest(@Size(min = 1, max = 120) String nome, Set<Role> perfis, Boolean ativo) { }
}
