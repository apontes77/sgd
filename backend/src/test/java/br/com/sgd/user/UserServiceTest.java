package br.com.sgd.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import br.com.sgd.audit.AuditLogRepository;
import br.com.sgd.auth.RefreshTokenRepository;

class UserServiceTest {
  @Test
  void deactivationRevokesAllRefreshTokens() {
    UserRepository users = mock(UserRepository.class);
    RefreshTokenRepository tokens = mock(RefreshTokenRepository.class);
    User user = mock(User.class);
    when(user.isAtivo()).thenReturn(true);
    when(user.getId()).thenReturn(9L);
    when(users.findById(9L)).thenReturn(Optional.of(user));
    UserService service =
        new UserService(users, mock(PasswordEncoder.class), mock(AuditLogRepository.class), tokens);
    service.update(9L, null, null, false);
    verify(user).update(null, null, false);
    verify(tokens).revokeAllByUserId(eq(9L), any());
  }
}
