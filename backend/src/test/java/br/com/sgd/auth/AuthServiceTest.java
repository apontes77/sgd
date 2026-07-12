package br.com.sgd.auth;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import br.com.sgd.audit.AuditLogRepository;
import br.com.sgd.config.SecurityProperties;
import br.com.sgd.user.User;
import br.com.sgd.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {
    private UserRepository users; private RefreshTokenRepository refreshTokens; private PasswordResetTokenRepository resetTokens;
    private AuditLogRepository audit; private PasswordEncoder passwords; private PasswordResetNotifier notifier; private AuthService service;

    @BeforeEach void setUp() {
        users = mock(UserRepository.class); refreshTokens = mock(RefreshTokenRepository.class); resetTokens = mock(PasswordResetTokenRepository.class);
        audit = mock(AuditLogRepository.class); passwords = mock(PasswordEncoder.class); notifier = mock(PasswordResetNotifier.class);
        service = new AuthService(users, refreshTokens, resetTokens, audit, passwords, mock(JwtService.class),
                new SecurityProperties("01234567890123456789012345678901", 15, 7, 30, null, null), notifier);
    }

    @Test void logoutRevokesKnownRefreshToken() throws Exception {
        User user = mock(User.class); RefreshToken token = mock(RefreshToken.class);
        when(token.isValid()).thenReturn(true); when(token.getUsuario()).thenReturn(user);
        when(refreshTokens.findByTokenHash(hash("refresh-value"))).thenReturn(Optional.of(token));
        service.logout("refresh-value");
        verify(token).revoke(); verify(audit).save(any());
    }

    @Test void passwordResetRequestInvalidatesPreviousTokenAndNotifies() {
        User user = mock(User.class); when(user.getId()).thenReturn(42L); when(user.isAtivo()).thenReturn(true);
        when(users.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        service.requestPasswordReset("user@example.com");
        verify(resetTokens).invalidateAllByUserId(eq(42L), any());
        verify(resetTokens).save(any(PasswordResetToken.class));
        verify(notifier).notify(eq(user), anyString());
    }

    private String hash(String value) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
}
