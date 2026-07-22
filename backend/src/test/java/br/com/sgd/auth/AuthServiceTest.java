package br.com.sgd.auth;

import static org.mockito.Mockito.*;

import br.com.sgd.audit.AuditLogRepository;
import br.com.sgd.config.SecurityProperties;
import br.com.sgd.user.User;
import br.com.sgd.user.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {
    private UserRepository users;
    private RefreshTokenRepository refreshTokens;
    private AuditLogRepository audit;
    private PasswordEncoder passwords;
    private PasswordCredentialService credentials;
    private AuthService service;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class); refreshTokens = mock(RefreshTokenRepository.class);
        audit = mock(AuditLogRepository.class); passwords = mock(PasswordEncoder.class);
        credentials = mock(PasswordCredentialService.class);
        service = new AuthService(users, refreshTokens, audit, passwords, mock(JwtService.class),
                mock(SecurityProperties.class), credentials);
    }

    @Test
    void logoutRevokesKnownRefreshToken() {
        User user = mock(User.class); RefreshToken token = mock(RefreshToken.class);
        when(token.isValid()).thenReturn(true); when(token.getUsuario()).thenReturn(user);
        when(refreshTokens.findByTokenHash(PasswordCredentialService.hash("refresh-value"))).thenReturn(Optional.of(token));
        service.logout("refresh-value");
        verify(token).revoke(); verify(audit).save(any());
    }

    @Test
    void pendingPasswordCannotAuthenticate() {
        User user = mock(User.class);
        when(user.isAtivo()).thenReturn(true); when(user.isSenhaDefinida()).thenReturn(false);
        when(users.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.login("user@example.com", "some-password"))
                .isInstanceOf(AuthService.InvalidCredentialsException.class);
        verifyNoInteractions(passwords);
    }

    @Test
    void passwordOperationsDelegateToCredentialService() {
        service.requestPasswordReset("user@example.com");
        service.resetPassword("token", "very-secure-password");
        verify(credentials).requestPasswordReset("user@example.com");
        verify(credentials).resetPassword("token", "very-secure-password");
    }
}
