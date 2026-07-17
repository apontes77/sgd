package br.com.sgd.auth;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.sgd.config.SecurityProperties;
import br.com.sgd.user.Role;
import br.com.sgd.user.User;
import java.util.Set;
import org.junit.jupiter.api.Test;

class JwtServiceTest {
    private static final String SECRET = "01234567890123456789012345678901";

    @Test
    void createsValidTokenWithUserSubject() {
        JwtService service = service(SECRET, 15);
        User user = new User("Admin", "ADMIN@EXAMPLE.COM", "hash", Set.of(Role.ADMIN));

        String token = service.createAccessToken(user);

        assertThat(service.isValid(token)).isTrue();
        assertThat(service.subject(token)).isEqualTo("admin@example.com");
    }

    @Test
    void rejectsMalformedWrongSignatureAndExpiredTokens() {
        JwtService service = service(SECRET, 15);
        String otherSignature = service("abcdefghijklmnopqrstuvwxyz123456", 15)
                .createAccessToken(new User("User", "user@example.com", "hash", Set.of()));
        String expired = service(SECRET, -1)
                .createAccessToken(new User("User", "user@example.com", "hash", Set.of()));

        assertThat(service.isValid("not-a-jwt")).isFalse();
        assertThat(service.isValid(otherSignature)).isFalse();
        assertThat(service.isValid(expired)).isFalse();
    }

    private JwtService service(String secret, long minutes) {
        return new JwtService(new SecurityProperties(secret, minutes, 7, 30, null, null));
    }
}
