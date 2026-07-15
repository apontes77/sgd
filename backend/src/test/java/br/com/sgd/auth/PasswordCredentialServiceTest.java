package br.com.sgd.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import br.com.sgd.audit.AuditLogRepository;
import br.com.sgd.config.SecurityProperties;
import br.com.sgd.user.User;
import br.com.sgd.user.UserRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

class PasswordCredentialServiceTest {
    private UserRepository users;
    private PasswordResetTokenRepository resetTokens;
    private RefreshTokenRepository refreshTokens;
    private AuditLogRepository audit;
    private PasswordEncoder passwords;
    private SecurityProperties properties;
    private PasswordResetNotifier notifier;
    private PasswordCredentialService service;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class); resetTokens = mock(PasswordResetTokenRepository.class);
        refreshTokens = mock(RefreshTokenRepository.class); audit = mock(AuditLogRepository.class);
        passwords = mock(PasswordEncoder.class); properties = mock(SecurityProperties.class);
        notifier = mock(PasswordResetNotifier.class);
        when(properties.passwordResetMinutes()).thenReturn(30L);
        when(properties.passwordSetupHours()).thenReturn(24L);
        service = new PasswordCredentialService(users, resetTokens, refreshTokens, audit, passwords, properties, notifier);
    }

    @Test
    void resetRequestStoresOnlyHashAndNotifiesWithRaw48ByteToken() {
        User user = user(42L, true);
        when(users.findByEmailIgnoreCaseForUpdate("user@example.com")).thenReturn(Optional.of(user));

        service.requestPasswordReset("user@example.com");

        ArgumentCaptor<PasswordResetToken> stored = ArgumentCaptor.forClass(PasswordResetToken.class);
        ArgumentCaptor<String> raw = ArgumentCaptor.forClass(String.class);
        verify(resetTokens).save(stored.capture());
        verify(notifier).notify(eq(user), raw.capture(), eq(PasswordResetToken.Type.REDEFINICAO), eq(30L));
        assertThat(java.util.Base64.getUrlDecoder().decode(raw.getValue())).hasSize(48);
        assertThat(stored.getValue().getTokenHash()).isEqualTo(PasswordCredentialService.hash(raw.getValue()));
        assertThat(stored.getValue().getTokenHash()).isNotEqualTo(raw.getValue());
        assertThat(stored.getValue().getTipo()).isEqualTo(PasswordResetToken.Type.REDEFINICAO);
    }

    @Test
    void cooldownAndUnknownAccountAreIndistinguishableAndDoNotNotify() {
        User user = user(42L, true);
        when(users.findByEmailIgnoreCaseForUpdate("known@example.com")).thenReturn(Optional.of(user));
        when(resetTokens.existsByUsuarioIdAndTipoAndCriadoEmAfter(eq(42L), eq(PasswordResetToken.Type.REDEFINICAO), any()))
                .thenReturn(true);

        service.requestPasswordReset("known@example.com");
        service.requestPasswordReset("unknown@example.com");

        verify(resetTokens, never()).save(any());
        verifyNoInteractions(notifier);
    }

    @Test
    void initialSetupUses24HoursAndInvalidatesPreviousTokens() {
        User user = user(7L, true);

        service.requestInitialSetup(user);

        verify(resetTokens).invalidateAllByUserId(eq(7L), any());
        verify(notifier).notify(eq(user), anyString(), eq(PasswordResetToken.Type.DEFINICAO_INICIAL), eq(1440L));
    }

    @Test
    void resetChangesHashConsumesTokenAndRevokesEverySession() {
        User user = user(9L, true);
        PasswordResetToken token = new PasswordResetToken(user, PasswordCredentialService.hash("raw-token"),
                Instant.now(), Instant.now().plusSeconds(60), PasswordResetToken.Type.REDEFINICAO);
        when(resetTokens.findByTokenHashForUpdate(PasswordCredentialService.hash("raw-token"))).thenReturn(Optional.of(token));
        when(passwords.encode("senha-muito-segura")).thenReturn("bcrypt-hash");

        service.resetPassword("raw-token", "senha-muito-segura");

        verify(user).updatePassword("bcrypt-hash");
        verify(resetTokens).invalidateAllByUserId(eq(9L), any());
        verify(refreshTokens).revokeAllByUserId(eq(9L), any());
        assertThat(token.isValid(Instant.now())).isFalse();
    }

    @Test
    void passwordPolicyCountsUtf8BytesAndEnforcesBcryptMaximum() {
        assertThatThrownBy(() -> service.resetPassword("token", "curta"))
                .isInstanceOf(PasswordPolicy.InvalidPasswordException.class);
        assertThatThrownBy(() -> service.resetPassword("token", "á".repeat(37)))
                .isInstanceOf(PasswordPolicy.InvalidPasswordException.class);
        verifyNoInteractions(resetTokens);
    }

    @Test
    void expiredTokenIsRejectedWithoutChangingCredentials() {
        User user = user(9L, true);
        PasswordResetToken expired = new PasswordResetToken(user, PasswordCredentialService.hash("expired"),
                Instant.now().minusSeconds(120), Instant.now().minusSeconds(60), PasswordResetToken.Type.REDEFINICAO);
        when(resetTokens.findByTokenHashForUpdate(PasswordCredentialService.hash("expired"))).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.resetPassword("expired", "senha-muito-segura"))
                .isInstanceOf(AuthService.InvalidTokenException.class);
        verify(user, never()).updatePassword(anyString());
        verifyNoInteractions(refreshTokens);
    }

    private User user(long id, boolean active) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id); when(user.isAtivo()).thenReturn(active);
        return user;
    }
}
