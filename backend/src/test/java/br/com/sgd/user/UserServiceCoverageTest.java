package br.com.sgd.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.sgd.audit.AuditLogRepository;
import br.com.sgd.auth.RefreshTokenRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserServiceCoverageTest {
    @Test
    void createsUserWithEncodedPasswordAndAudit() {
        UserRepository users = mock(UserRepository.class);
        PasswordEncoder passwords = mock(PasswordEncoder.class);
        AuditLogRepository audit = mock(AuditLogRepository.class);
        when(users.existsByEmailIgnoreCase("USER@example.com")).thenReturn(false);
        when(passwords.encode("secret")).thenReturn("encoded");
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        UserService service = new UserService(users, passwords, audit, mock(RefreshTokenRepository.class));

        User user = service.create("User", "USER@example.com", "secret", Set.of(Role.ADMIN));

        assertThat(user.getEmail()).isEqualTo("user@example.com");
        assertThat(user.getSenhaHash()).isEqualTo("encoded");
        assertThat(user.getPerfis()).containsExactly(Role.ADMIN);
        verify(audit).save(any());
    }

    @Test
    void rejectsDuplicateEmail() {
        UserRepository users = mock(UserRepository.class);
        when(users.existsByEmailIgnoreCase("duplicate@example.com")).thenReturn(true);
        UserService service = service(users, mock(RefreshTokenRepository.class), mock(AuditLogRepository.class));

        assertThatThrownBy(() -> service.create("User", "duplicate@example.com", "secret", Set.of()))
                .isInstanceOf(UserService.DuplicateEmailException.class);
        verify(users, never()).save(any());
    }

    @Test
    void updateChangesUserWithoutRevokingWhenStillActive() {
        UserRepository users = mock(UserRepository.class);
        RefreshTokenRepository tokens = mock(RefreshTokenRepository.class);
        AuditLogRepository audit = mock(AuditLogRepository.class);
        User user = mock(User.class);
        when(user.isAtivo()).thenReturn(true);
        when(users.findById(7L)).thenReturn(Optional.of(user));
        UserService service = service(users, tokens, audit);

        assertThat(service.update(7L, "New name", Set.of(Role.GERENTE), true)).isSameAs(user);
        verify(user).update("New name", Set.of(Role.GERENTE), true);
        verify(tokens, never()).revokeAllByUserId(anyLong(), any());
        verify(audit).save(any());
    }

    @Test
    void updateRejectsUnknownUser() {
        UserRepository users = mock(UserRepository.class);
        when(users.findById(99L)).thenReturn(Optional.empty());
        UserService service = service(users, mock(RefreshTokenRepository.class), mock(AuditLogRepository.class));
        assertThatThrownBy(() -> service.update(99L, "Name", Set.of(Role.ADMIN), true))
                .isInstanceOf(UserService.UserNotFoundException.class);
    }

    @Test
    void listUsesFilteredOrUnfilteredRepositoryQuery() {
        UserRepository users = mock(UserRepository.class);
        UserService service = service(users, mock(RefreshTokenRepository.class), mock(AuditLogRepository.class));
        PageRequest pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(List.of());
        when(users.findAll(pageable)).thenReturn(page);
        when(users.findAllByAtivo(true, pageable)).thenReturn(page);

        assertThat(service.list(null, pageable)).isSameAs(page);
        assertThat(service.list(true, pageable)).isSameAs(page);
        verify(users).findAll(pageable);
        verify(users).findAllByAtivo(true, pageable);
    }

    private UserService service(UserRepository users, RefreshTokenRepository tokens, AuditLogRepository audit) {
        return new UserService(users, mock(PasswordEncoder.class), audit, tokens);
    }
}
