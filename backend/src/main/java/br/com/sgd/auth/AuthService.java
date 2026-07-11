package br.com.sgd.auth;

import br.com.sgd.audit.AuditLog;
import br.com.sgd.audit.AuditLogRepository;
import br.com.sgd.config.SecurityProperties;
import br.com.sgd.user.Role;
import br.com.sgd.user.User;
import br.com.sgd.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {
    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordResetTokenRepository resetTokens;
    private final AuditLogRepository audit;
    private final PasswordEncoder passwords;
    private final JwtService jwt;
    private final SecurityProperties properties;

    public AuthService(UserRepository users, RefreshTokenRepository refreshTokens, PasswordResetTokenRepository resetTokens,
                       AuditLogRepository audit, PasswordEncoder passwords, JwtService jwt, SecurityProperties properties) {
        this.users = users; this.refreshTokens = refreshTokens; this.resetTokens = resetTokens;
        this.audit = audit; this.passwords = passwords; this.jwt = jwt; this.properties = properties;
    }
    public Tokens login(String email, String password) {
        User user = users.findByEmailIgnoreCase(email).filter(User::isAtivo)
                .orElseThrow(() -> new InvalidCredentialsException());
        if (!passwords.matches(password, user.getSenhaHash())) throw new InvalidCredentialsException();
        audit.save(new AuditLog(user, "USUARIO", "LOGIN", "{}"));
        return issueTokens(user);
    }
    public Tokens refresh(String rawToken) {
        RefreshToken stored = refreshTokens.findByTokenHash(hash(rawToken))
                .filter(RefreshToken::isValid).orElseThrow(() -> new InvalidCredentialsException());
        stored.revoke();
        if (!stored.getUsuario().isAtivo()) throw new InvalidCredentialsException();
        return issueTokens(stored.getUsuario());
    }
    public void requestPasswordReset(String email) {
        users.findByEmailIgnoreCase(email).filter(User::isAtivo).ifPresent(user -> {
            String rawToken = randomToken();
            resetTokens.save(new PasswordResetToken(user, hash(rawToken), Instant.now().plus(properties.passwordResetMinutes(), ChronoUnit.MINUTES)));
            audit.save(new AuditLog(user, "USUARIO", "SOLICITACAO_REDEFINICAO_SENHA", "{}"));
            // Integre um provedor de e-mail aqui. O token não é retornado pela API por segurança.
            org.slf4j.LoggerFactory.getLogger(AuthService.class).info("Redefinição solicitada para {}", user.getEmail());
        });
    }
    public void resetPassword(String rawToken, String newPassword) {
        PasswordResetToken token = resetTokens.findByTokenHash(hash(rawToken)).filter(PasswordResetToken::isValid)
                .orElseThrow(() -> new InvalidTokenException());
        token.getUsuario().updatePassword(passwords.encode(newPassword));
        token.use();
        audit.save(new AuditLog(token.getUsuario(), "USUARIO", "REDEFINICAO_SENHA", "{}"));
    }
    public void bootstrapAdmin() {
        if (properties.adminEmail() == null || properties.adminEmail().isBlank() || properties.adminPassword() == null || properties.adminPassword().isBlank()) return;
        if (!users.existsByEmailIgnoreCase(properties.adminEmail())) {
            users.save(new User("Administrador", properties.adminEmail(), passwords.encode(properties.adminPassword()), Set.of(Role.ADMIN)));
        }
    }
    private Tokens issueTokens(User user) {
        String rawRefresh = randomToken();
        refreshTokens.save(new RefreshToken(user, hash(rawRefresh), Instant.now().plus(properties.refreshTokenDays(), ChronoUnit.DAYS)));
        return new Tokens(jwt.createAccessToken(user), rawRefresh, user);
    }
    private String randomToken() { byte[] bytes = new byte[48]; new SecureRandom().nextBytes(bytes); return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
    private String hash(String value) {
        try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception exception) { throw new IllegalStateException("Hash SHA-256 indisponível", exception); }
    }
    public record Tokens(String accessToken, String refreshToken, User user) { }
    public static class InvalidCredentialsException extends RuntimeException { }
    public static class InvalidTokenException extends RuntimeException { }
}
