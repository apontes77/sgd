package br.com.sgd.auth;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.mail.MailSendException;
import org.springframework.security.crypto.password.PasswordEncoder;

import br.com.sgd.audit.AuditLogRepository;
import br.com.sgd.config.SecurityProperties;
import br.com.sgd.user.Role;
import br.com.sgd.user.User;
import br.com.sgd.user.UserRepository;

class AuthServiceTest {
  private UserRepository users;
  private RefreshTokenRepository refreshTokens;
  private PasswordResetTokenRepository resetTokens;
  private AuditLogRepository audit;
  private PasswordEncoder passwords;
  private PasswordResetNotifier notifier;
  private AuthService service;

  @BeforeEach
  void setUp() {
    users = mock(UserRepository.class);
    refreshTokens = mock(RefreshTokenRepository.class);
    resetTokens = mock(PasswordResetTokenRepository.class);
    audit = mock(AuditLogRepository.class);
    passwords = mock(PasswordEncoder.class);
    notifier = mock(PasswordResetNotifier.class);
    service =
        new AuthService(
            users,
            refreshTokens,
            resetTokens,
            audit,
            passwords,
            mock(JwtService.class),
            new SecurityProperties("01234567890123456789012345678901", 15, 7, 30, null, null),
            notifier);
  }

  @Test
  void logoutRevokesKnownRefreshToken() throws Exception {
    User user = mock(User.class);
    RefreshToken token = mock(RefreshToken.class);
    when(token.isValid()).thenReturn(true);
    when(token.getUsuario()).thenReturn(user);
    when(refreshTokens.findByTokenHash(hash("refresh-value"))).thenReturn(Optional.of(token));
    service.logout("refresh-value");
    verify(token).revoke();
    verify(audit).save(any());
  }

  @Test
  void passwordResetRequestInvalidatesPreviousTokenAndNotifies() {
    User user = mock(User.class);
    when(user.getId()).thenReturn(42L);
    when(user.isAtivo()).thenReturn(true);
    when(users.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
    service.requestPasswordReset("user@example.com");
    verify(resetTokens).invalidateAllByUserId(eq(42L), any());
    verify(resetTokens).save(any(PasswordResetToken.class));
    verify(notifier).notify(eq(user), anyString());
  }

  @ParameterizedTest
  @EnumSource(Role.class)
  void passwordResetIsAvailableForEveryLocalRole(Role role) {
    String email = role.name().toLowerCase() + "@example.com";
    User user = mock(User.class);
    when(user.getId()).thenReturn(42L);
    when(user.getEmail()).thenReturn(email);
    when(user.getPerfis()).thenReturn(java.util.Set.of(role));
    when(user.isAtivo()).thenReturn(true);
    when(users.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));

    service.requestPasswordReset(email);

    verify(resetTokens).save(any(PasswordResetToken.class));
    verify(notifier).notify(eq(user), anyString());
  }

  @Test
  void passwordResetRequestRemainsNeutralWhenEmailDeliveryFails() {
    User user = mock(User.class);
    when(user.getId()).thenReturn(42L);
    when(user.isAtivo()).thenReturn(true);
    when(users.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
    doThrow(new MailSendException("smtp unavailable")).when(notifier).notify(eq(user), anyString());

    service.requestPasswordReset("user@example.com");

    verify(resetTokens).save(any(PasswordResetToken.class));
  }

  private String hash(String value) throws Exception {
    return HexFormat.of()
        .formatHex(
            MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
  }
}
