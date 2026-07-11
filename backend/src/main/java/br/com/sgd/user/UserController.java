package br.com.sgd.user;

import br.com.sgd.auth.AuthController.UserResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/users")
public class UserController {
    private final UserService service; public UserController(UserService service) { this.service = service; }
    @GetMapping public List<UserResponse> list() { return service.list().stream().map(UserResponse::of).toList(); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public UserResponse create(@Valid @RequestBody CreateUserRequest r) { return UserResponse.of(service.create(r.nome(), r.email(), r.password(), r.perfis())); }
    @PutMapping("/{id}") public UserResponse update(@PathVariable long id, @Valid @RequestBody UpdateUserRequest r) { return UserResponse.of(service.update(id, r.nome(), r.perfis(), r.ativo())); }
    public record CreateUserRequest(@NotBlank String nome, @Email @NotBlank String email, @NotBlank @Size(min = 12) String password, @NotEmpty Set<Role> perfis) { }
    public record UpdateUserRequest(@Size(min = 1, max = 120) String nome, Set<Role> perfis, Boolean ativo) { }
}
