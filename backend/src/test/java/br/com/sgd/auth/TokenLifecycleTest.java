package br.com.sgd.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Set;

import org.junit.jupiter.api.Test;

import br.com.sgd.user.User;

class TokenLifecycleTest {
  private final User user = new User("User", "user@example.com", "hash", Set.of());

  @Test
  void refreshTokenIsInvalidWhenExpiredOrRevoked() {
    assertThat(new RefreshToken(user, "expired", Instant.now().minusSeconds(1)).isValid())
        .isFalse();
    RefreshToken token = new RefreshToken(user, "valid", Instant.now().plusSeconds(60));
    assertThat(token.isValid()).isTrue();
    token.revoke();
    assertThat(token.isValid()).isFalse();
  }

  @Test
  void passwordResetTokenIsInvalidWhenExpiredOrUsed() {
    assertThat(new PasswordResetToken(user, "expired", Instant.now().minusSeconds(1)).isValid())
        .isFalse();
    PasswordResetToken token = new PasswordResetToken(user, "valid", Instant.now().plusSeconds(60));
    assertThat(token.isValid()).isTrue();
    token.use();
    assertThat(token.isValid()).isFalse();
  }
}
