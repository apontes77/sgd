package br.com.sgd.auth;

import br.com.sgd.audit.AuditLog;
import br.com.sgd.audit.AuditLogRepository;
import br.com.sgd.config.SecurityProperties;
import br.com.sgd.user.Role;
import br.com.sgd.user.User;
import br.com.sgd.user.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {
    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final AuditLogRepository audit;
    private final PasswordEncoder passwords;
    private final JwtService jwt;
    private final SecurityProperties properties;
    private final PasswordCredentialService credentials;

    public AuthService(UserRepository users, RefreshTokenRepository refreshTokens,
                       AuditLogRepository audit, PasswordEncoder passwords, JwtService jwt,
                       SecurityProperties properties, PasswordCredentialService credentials) {
        this.users = users; this.refreshTokens = refreshTokens; this.audit = audit;
        this.passwords = passwords; this.jwt = jwt; this.properties = properties;
        this.credentials = credentials;
    }

    public Tokens login(String email, String password) {
        User user = users.findByEmailIgnoreCase(email).filter(User::isAtivo)
                .orElseThrow(InvalidCredentialsException::new);
        if (!user.isSenhaDefinida() || !passwords.matches(password, user.getSenhaHash()))
            throw new InvalidCredentialsException();
        audit.save(new AuditLog(user, "USUARIO", "LOGIN", "{}"));
        return issueTokens(user);
    }

    public Tokens refresh(String rawToken) {
        RefreshToken stored = refreshTokens.findByTokenHash(PasswordCredentialService.hash(rawToken))
                .filter(RefreshToken::isValid).orElseThrow(InvalidCredentialsException::new);
        stored.revoke();
        if (!stored.getUsuario().isAtivo()) throw new InvalidCredentialsException();
        return issueTokens(stored.getUsuario());
    }

    public void logout(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;
        refreshTokens.findByTokenHash(PasswordCredentialService.hash(rawToken)).filter(RefreshToken::isValid).ifPresent(token -> {
            token.revoke();
            audit.save(new AuditLog(token.getUsuario(), "USUARIO", "LOGOUT", "{}"));
        });
    }

    public void requestPasswordReset(String email) { credentials.requestPasswordReset(email); }
    public void resetPassword(String rawToken, String newPassword) { credentials.resetPassword(rawToken, newPassword); }

    public void bootstrapAdmin() {
        if (properties.adminEmail() == null || properties.adminEmail().isBlank()) return;
        if (!users.existsByEmailIgnoreCase(properties.adminEmail())) {
            User admin = users.save(new User("Administrador", properties.adminEmail(), null, Set.of(Role.ADMIN)));
            credentials.requestInitialSetup(admin);
        }
    }

    private Tokens issueTokens(User user) {
        String rawRefresh = PasswordCredentialService.randomToken();
        refreshTokens.save(new RefreshToken(user, PasswordCredentialService.hash(rawRefresh),
                Instant.now().plus(properties.refreshTokenDays(), ChronoUnit.DAYS)));
        return new Tokens(jwt.createAccessToken(user), rawRefresh, user);
    }

    public record Tokens(String accessToken, String refreshToken, User user) { }
    public static class InvalidCredentialsException extends RuntimeException { }
    public static class InvalidTokenException extends RuntimeException { }
}
