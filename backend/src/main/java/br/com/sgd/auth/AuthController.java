package br.com.sgd.auth;

import br.com.sgd.user.Role;
import br.com.sgd.user.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/autenticacao")
public class AuthController {
    private final AuthService auth;
    public AuthController(AuthService auth) { this.auth = auth; }
    @PostMapping("/login") public TokenResponse login(@Valid @RequestBody LoginRequest request) { return TokenResponse.of(auth.login(request.email(), request.password())); }
    @PostMapping("/atualizar-token") public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) { return TokenResponse.of(auth.refresh(request.refreshToken())); }
    @PostMapping("/logout") public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) { auth.logout(request.refreshToken()); return ResponseEntity.noContent().build(); }
    @PostMapping("/esqueci-a-senha") public ResponseEntity<Void> forgot(@Valid @RequestBody ForgotPasswordRequest request) { auth.requestPasswordReset(request.email()); return ResponseEntity.noContent().build(); }
    @PostMapping("/redefinir-senha") public ResponseEntity<Void> reset(@Valid @RequestBody ResetPasswordRequest request) { auth.resetPassword(request.token(), request.novaSenha()); return ResponseEntity.noContent().build(); }
    @GetMapping("/eu") public UserResponse me(org.springframework.security.core.Authentication authentication) { return UserResponse.of((User) authentication.getPrincipal()); }
    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) { }
    public record RefreshRequest(@NotBlank String refreshToken) { }
    public record ForgotPasswordRequest(@Email @NotBlank String email) { }
    public record ResetPasswordRequest(@NotBlank String token, @NotBlank @Size(min = 12, max = 128) String novaSenha) { }
    public record TokenResponse(String accessToken, String refreshToken, UserResponse user) { static TokenResponse of(AuthService.Tokens t) { return new TokenResponse(t.accessToken(), t.refreshToken(), UserResponse.of(t.user())); } }
    public record UserResponse(Long id, String nome, String email, boolean ativo, Set<Role> perfis) { public static UserResponse of(User user) { return new UserResponse(user.getId(), user.getNome(), user.getEmail(), user.isAtivo(), user.getPerfis()); } }
}
