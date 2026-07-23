package br.com.sgd.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import br.com.sgd.audit.AuditLogRepository;
import br.com.sgd.config.SecurityProperties;
import br.com.sgd.user.User;
import br.com.sgd.user.UserRepository;

class AuthServiceCoverageTest {
  private UserRepository users;
  private RefreshTokenRepository refreshTokens;
  private PasswordResetTokenRepository resetTokens;
  private AuditLogRepository audit;
  private PasswordEncoder passwords;
  private JwtService jwt;
  private PasswordResetNotifier notifier;
  private AuthService service;

  @BeforeEach
  void setUp() {
    users = mock(UserRepository.class);
    refreshTokens = mock(RefreshTokenRepository.class);
    resetTokens = mock(PasswordResetTokenRepository.class);
    audit = mock(AuditLogRepository.class);
    passwords = mock(PasswordEncoder.class);
    jwt = mock(JwtService.class);
    notifier = mock(PasswordResetNotifier.class);
    service = service(null, null);
  }

  @Test
  void loginIssuesTokensAndAuditsActiveUser() {
    User user = new User("User", "user@example.com", "encoded", Set.of());
    when(users.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
    when(passwords.matches("secret", "encoded")).thenReturn(true);
    when(jwt.createAccessToken(user)).thenReturn("access-token");

    AuthService.Tokens tokens = service.login("user@example.com", "secret");

    assertThat(tokens.accessToken()).isEqualTo("access-token");
    assertThat(tokens.refreshToken()).isNotBlank();
    assertThat(tokens.user()).isSameAs(user);
    verify(refreshTokens).save(any(RefreshToken.class));
    verify(audit).save(any());
  }

  @Test
  void loginRejectsUnknownInactiveAndWrongPassword() {
    when(users.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.login("missing@example.com", "secret"))
        .isInstanceOf(AuthService.InvalidCredentialsException.class);

    User inactive = mock(User.class);
    when(inactive.isAtivo()).thenReturn(false);
    when(users.findByEmailIgnoreCase("inactive@example.com")).thenReturn(Optional.of(inactive));
    assertThatThrownBy(() -> service.login("inactive@example.com", "secret"))
        .isInstanceOf(AuthService.InvalidCredentialsException.class);

    User active = mock(User.class);
    when(active.isAtivo()).thenReturn(true);
    when(active.getSenhaHash()).thenReturn("encoded");
    when(users.findByEmailIgnoreCase("active@example.com")).thenReturn(Optional.of(active));
    when(passwords.matches("wrong", "encoded")).thenReturn(false);
    assertThatThrownBy(() -> service.login("active@example.com", "wrong"))
        .isInstanceOf(AuthService.InvalidCredentialsException.class);
  }

  @Test
  void refreshRotatesValidToken() throws Exception {
    User user = mock(User.class);
    RefreshToken stored = mock(RefreshToken.class);
    when(stored.isValid()).thenReturn(true);
    when(stored.getUsuario()).thenReturn(user);
    when(user.isAtivo()).thenReturn(true);
    when(refreshTokens.findByTokenHash(hash("old-refresh"))).thenReturn(Optional.of(stored));

    AuthService.Tokens result = service.refresh("old-refresh");

    assertThat(result.user()).isSameAs(user);
    verify(stored).revoke();
    verify(refreshTokens).save(any(RefreshToken.class));
  }

  @Test
  void refreshRejectsMissingInvalidAndInactiveTokens() throws Exception {
    when(refreshTokens.findByTokenHash(hash("missing"))).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.refresh("missing"))
        .isInstanceOf(AuthService.InvalidCredentialsException.class);

    RefreshToken invalid = mock(RefreshToken.class);
    when(invalid.isValid()).thenReturn(false);
    when(refreshTokens.findByTokenHash(hash("invalid"))).thenReturn(Optional.of(invalid));
    assertThatThrownBy(() -> service.refresh("invalid"))
        .isInstanceOf(AuthService.InvalidCredentialsException.class);

    User inactive = mock(User.class);
    RefreshToken valid = mock(RefreshToken.class);
    when(valid.isValid()).thenReturn(true);
    when(valid.getUsuario()).thenReturn(inactive);
    when(inactive.isAtivo()).thenReturn(false);
    when(refreshTokens.findByTokenHash(hash("inactive"))).thenReturn(Optional.of(valid));
    assertThatThrownBy(() -> service.refresh("inactive"))
        .isInstanceOf(AuthService.InvalidCredentialsException.class);
    verify(valid).revoke();
  }

  @Test
  void resetPasswordConsumesTokenAndRevokesSessions() throws Exception {
    User user = mock(User.class);
    when(user.getId()).thenReturn(42L);
    PasswordResetToken token = mock(PasswordResetToken.class);
    when(token.isValid()).thenReturn(true);
    when(token.getUsuario()).thenReturn(user);
    when(passwords.encode("new-password")).thenReturn("new-hash");
    when(resetTokens.findByTokenHash(hash("reset-token"))).thenReturn(Optional.of(token));

    service.resetPassword("reset-token", "new-password");

    verify(user).updatePassword("new-hash");
    verify(token).use();
    verify(resetTokens).invalidateAllByUserId(eq(42L), any(Instant.class));
    verify(refreshTokens).revokeAllByUserId(eq(42L), any(Instant.class));
    verify(audit).save(any());
  }

  @Test
  void resetPasswordRejectsMissingOrExpiredToken() throws Exception {
    when(resetTokens.findByTokenHash(hash("missing"))).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.resetPassword("missing", "password"))
        .isInstanceOf(AuthService.InvalidTokenException.class);
    PasswordResetToken expired = mock(PasswordResetToken.class);
    when(expired.isValid()).thenReturn(false);
    when(resetTokens.findByTokenHash(hash("expired"))).thenReturn(Optional.of(expired));
    assertThatThrownBy(() -> service.resetPassword("expired", "password"))
        .isInstanceOf(AuthService.InvalidTokenException.class);
  }

  @Test
  void resetRequestDoesNotRevealMissingOrInactiveAccount() {
    when(users.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());
    service.requestPasswordReset("missing@example.com");
    User inactive = mock(User.class);
    when(inactive.isAtivo()).thenReturn(false);
    when(users.findByEmailIgnoreCase("inactive@example.com")).thenReturn(Optional.of(inactive));
    service.requestPasswordReset("inactive@example.com");
    verifyNoInteractions(resetTokens, notifier);
  }

  @Test
  void logoutIgnoresBlankUnknownAndInvalidTokens() throws Exception {
    service.logout(null);
    service.logout(" ");
    when(refreshTokens.findByTokenHash(hash("unknown"))).thenReturn(Optional.empty());
    service.logout("unknown");
    RefreshToken invalid = mock(RefreshToken.class);
    when(invalid.isValid()).thenReturn(false);
    when(refreshTokens.findByTokenHash(hash("invalid"))).thenReturn(Optional.of(invalid));
    service.logout("invalid");
    verify(invalid, never()).revoke();
    verifyNoInteractions(audit);
  }

  @Test
  void bootstrapAdminOnlyWhenConfiguredAndAbsent() {
    service.bootstrapAdmin();
    verifyNoInteractions(users);

    service = service("admin@example.com", "secret");
    when(users.existsByEmailIgnoreCase("admin@example.com")).thenReturn(false);
    when(passwords.encode("secret")).thenReturn("encoded");
    service.bootstrapAdmin();
    verify(users).save(any(User.class));
  }

  private AuthService service(String adminEmail, String adminPassword) {
    return new AuthService(
        users,
        refreshTokens,
        resetTokens,
        audit,
        passwords,
        jwt,
        new SecurityProperties(
            "01234567890123456789012345678901", 15, 7, 30, adminEmail, adminPassword),
        notifier);
  }

  private String hash(String value) throws Exception {
    return HexFormat.of()
        .formatHex(
            MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
  }
}
