package br.com.sgd.auth;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.sgd.user.User;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TokenLifecycleTest {
    private final User user = new User("User", "user@example.com", "hash", Set.of());

    @Test
    void refreshTokenIsInvalidWhenExpiredOrRevoked() {
        assertThat(new RefreshToken(user, "expired", Instant.now().minusSeconds(1)).isValid()).isFalse();
        RefreshToken token = new RefreshToken(user, "valid", Instant.now().plusSeconds(60));
        assertThat(token.isValid()).isTrue();
        token.revoke();
        assertThat(token.isValid()).isFalse();
    }

    @Test
    void passwordResetTokenIsInvalidWhenExpiredOrUsed() {
        Instant now = Instant.now();
        assertThat(new PasswordResetToken(user, "expired", now.minusSeconds(120), now.minusSeconds(1),
                PasswordResetToken.Type.REDEFINICAO).isValid(now)).isFalse();
        PasswordResetToken token = new PasswordResetToken(user, "valid", now, now.plusSeconds(60),
                PasswordResetToken.Type.REDEFINICAO);
        assertThat(token.isValid(now)).isTrue();
        token.use(now);
        assertThat(token.isValid(now)).isFalse();
    }
}
