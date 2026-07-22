package br.com.sgd.auth;

import br.com.sgd.audit.AuditLog;
import br.com.sgd.audit.AuditLogRepository;
import br.com.sgd.config.SecurityProperties;
import br.com.sgd.user.User;
import br.com.sgd.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PasswordCredentialService {
    static final long RESET_COOLDOWN_MINUTES = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository users;
    private final PasswordResetTokenRepository resetTokens;
    private final RefreshTokenRepository refreshTokens;
    private final AuditLogRepository audit;
    private final PasswordEncoder passwords;
    private final SecurityProperties properties;
    private final PasswordResetNotifier notifier;

    public PasswordCredentialService(UserRepository users, PasswordResetTokenRepository resetTokens,
                                     RefreshTokenRepository refreshTokens, AuditLogRepository audit,
                                     PasswordEncoder passwords, SecurityProperties properties,
                                     PasswordResetNotifier notifier) {
        this.users = users; this.resetTokens = resetTokens; this.refreshTokens = refreshTokens;
        this.audit = audit; this.passwords = passwords; this.properties = properties; this.notifier = notifier;
    }

    /**
     * Always returns normally for unknown, inactive and cooldown-limited accounts.
     * Cooldown applies only after a successful e-mail delivery, so failed sends can be retried immediately.
     */
    public void requestPasswordReset(String email) {
        users.findByEmailIgnoreCaseForUpdate(email.toLowerCase(Locale.ROOT)).filter(User::isAtivo).ifPresent(user -> {
            Instant now = Instant.now();
            boolean coolingDown = resetTokens.existsByUsuarioIdAndTipoAndEnviadoEmAfter(
                    user.getId(), PasswordResetToken.Type.REDEFINICAO,
                    now.minus(RESET_COOLDOWN_MINUTES, ChronoUnit.MINUTES));
            if (!coolingDown) issue(user, PasswordResetToken.Type.REDEFINICAO,
                    properties.passwordResetMinutes(), now, "SOLICITACAO_REDEFINICAO_SENHA");
        });
    }

    public void requestInitialSetup(User user) {
        issue(user, PasswordResetToken.Type.DEFINICAO_INICIAL,
                properties.passwordSetupHours() * 60, Instant.now(), "SOLICITACAO_DEFINICAO_INICIAL_SENHA");
    }

    public void resendInitialSetup(long userId) {
        User user = users.findById(userId).orElseThrow(br.com.sgd.user.UserService.UserNotFoundException::new);
        if (!user.isAtivo() || user.isSenhaDefinida()) throw new SetupResendNotAllowedException();
        requestInitialSetup(user);
    }

    public void resetPassword(String rawToken, String newPassword) {
        PasswordPolicy.validate(newPassword);
        Instant now = Instant.now();
        PasswordResetToken token = resetTokens.findByTokenHashForUpdate(hash(rawToken))
                .filter(candidate -> candidate.isValid(now))
                .orElseThrow(AuthService.InvalidTokenException::new);
        User user = token.getUsuario();
        if (!user.isAtivo()) throw new AuthService.InvalidTokenException();
        user.updatePassword(passwords.encode(newPassword));
        token.use(now);
        resetTokens.invalidateAllByUserId(user.getId(), now);
        refreshTokens.revokeAllByUserId(user.getId(), now);
        audit.save(new AuditLog(user, "USUARIO", token.getTipo() == PasswordResetToken.Type.DEFINICAO_INICIAL
                ? "DEFINICAO_INICIAL_SENHA" : "REDEFINICAO_SENHA", "{}"));
    }

    private void issue(User user, PasswordResetToken.Type type, long validityMinutes, Instant now, String auditAction) {
        String rawToken = randomToken();
        resetTokens.invalidateAllByUserId(user.getId(), now);
        resetTokens.save(new PasswordResetToken(user, hash(rawToken), now,
                now.plus(validityMinutes, ChronoUnit.MINUTES), type));
        audit.save(new AuditLog(user, "USUARIO", auditAction, "{}"));
        notifier.notify(user, rawToken, type, validityMinutes);
    }

    static String randomToken() {
        byte[] bytes = new byte[48];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Hash SHA-256 indisponível", exception);
        }
    }

    public static class SetupResendNotAllowedException extends RuntimeException { }
}
