package br.com.sgd.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import br.com.sgd.user.User;
import br.com.sgd.user.UserRepository;

class OAuthIdentityServiceTest {
  private OAuthIdentityRepository identities;
  private UserRepository users;
  private PasswordEncoder passwords;
  private OAuthIdentityService service;

  @BeforeEach
  void setUp() {
    identities = mock(OAuthIdentityRepository.class);
    users = mock(UserRepository.class);
    passwords = mock(PasswordEncoder.class);
    service = new OAuthIdentityService(identities, users, passwords);
  }

  @Test
  void returnsActiveUserFromExistingIdentity() {
    User user = user(true, 10L);
    when(identities.findByProviderAndExternalSubject(OAuthProvider.GOOGLE, "subject"))
        .thenReturn(Optional.of(new OAuthIdentity(user, OAuthProvider.GOOGLE, "subject")));

    assertThat(
            service.resolveOrProvision(OAuthProvider.GOOGLE, "subject", "user@example.com", "User"))
        .isSameAs(user);
  }

  @Test
  void rejectsIncompleteOrInactiveIdentities() {
    assertThatThrownBy(
            () -> service.resolveOrProvision(null, "subject", "user@example.com", "User"))
        .isInstanceOf(OAuthIdentityService.InvalidOAuthIdentityException.class);
    assertThatThrownBy(
            () -> service.resolveOrProvision(OAuthProvider.GOOGLE, " ", "user@example.com", "User"))
        .isInstanceOf(OAuthIdentityService.InvalidOAuthIdentityException.class);
    assertThatThrownBy(
            () -> service.resolveOrProvision(OAuthProvider.GOOGLE, "subject", null, "User"))
        .isInstanceOf(OAuthIdentityService.InvalidOAuthIdentityException.class);

    User inactive = user(false, 11L);
    when(identities.findByProviderAndExternalSubject(OAuthProvider.GOOGLE, "inactive"))
        .thenReturn(Optional.of(new OAuthIdentity(inactive, OAuthProvider.GOOGLE, "inactive")));
    assertThatThrownBy(
            () ->
                service.resolveOrProvision(
                    OAuthProvider.GOOGLE, "inactive", "user@example.com", "User"))
        .isInstanceOf(OAuthIdentityService.InvalidOAuthIdentityException.class);
  }

  @Test
  void linksIdentityToExistingVerifiedEmail() {
    User user = user(true, 12L);
    when(identities.findByProviderAndExternalSubject(OAuthProvider.MICROSOFT, "subject"))
        .thenReturn(Optional.empty());
    when(users.findByEmailIgnoreCase("existing@example.com")).thenReturn(Optional.of(user));
    when(identities.findByProviderAndUsuarioId(OAuthProvider.MICROSOFT, 12L))
        .thenReturn(Optional.empty());

    assertThat(
            service.resolveOrProvision(
                OAuthProvider.MICROSOFT, "subject", "existing@example.com", "Existing"))
        .isSameAs(user);
    verify(identities).save(any(OAuthIdentity.class));
  }

  @Test
  void provisionsUserAndUsesEmailWhenDisplayNameIsBlank() {
    when(identities.findByProviderAndExternalSubject(OAuthProvider.GOOGLE, "new-subject"))
        .thenReturn(Optional.empty());
    when(users.findByEmailIgnoreCase("new@example.com")).thenReturn(Optional.empty());
    when(passwords.encode(any())).thenReturn("encoded-random-password");
    when(users.save(any(User.class)))
        .thenAnswer(invocation -> withId(invocation.getArgument(0), 15L));
    when(identities.findByProviderAndUsuarioId(OAuthProvider.GOOGLE, 15L))
        .thenReturn(Optional.empty());

    User created =
        service.resolveOrProvision(OAuthProvider.GOOGLE, "new-subject", "new@example.com", " ");

    assertThat(created.getNome()).isEqualTo("new@example.com");
    assertThat(created.getEmail()).isEqualTo("new@example.com");
    assertThat(created.getPerfis()).isEqualTo(Set.of());
    verify(passwords).encode(any(String.class));
    ArgumentCaptor<OAuthIdentity> identity = ArgumentCaptor.forClass(OAuthIdentity.class);
    verify(identities).save(identity.capture());
    assertThat(identity.getValue().getExternalSubject()).isEqualTo("new-subject");
  }

  @Test
  void rejectsInactiveEmailAccountAndProviderConflict() {
    User inactive = user(false, 13L);
    when(identities.findByProviderAndExternalSubject(OAuthProvider.GOOGLE, "inactive-email"))
        .thenReturn(Optional.empty());
    when(users.findByEmailIgnoreCase("inactive@example.com")).thenReturn(Optional.of(inactive));
    assertThatThrownBy(
            () ->
                service.resolveOrProvision(
                    OAuthProvider.GOOGLE, "inactive-email", "inactive@example.com", "Inactive"))
        .isInstanceOf(OAuthIdentityService.InvalidOAuthIdentityException.class);

    User active = user(true, 14L);
    when(identities.findByProviderAndExternalSubject(OAuthProvider.GOOGLE, "conflict"))
        .thenReturn(Optional.empty());
    when(users.findByEmailIgnoreCase("active@example.com")).thenReturn(Optional.of(active));
    when(identities.findByProviderAndUsuarioId(OAuthProvider.GOOGLE, 14L))
        .thenReturn(Optional.of(new OAuthIdentity(active, OAuthProvider.GOOGLE, "old-subject")));
    assertThatThrownBy(
            () ->
                service.resolveOrProvision(
                    OAuthProvider.GOOGLE, "conflict", "active@example.com", "Active"))
        .isInstanceOf(OAuthIdentityService.OAuthIdentityConflictException.class);
  }

  private User user(boolean active, long id) {
    User user = mock(User.class);
    when(user.isAtivo()).thenReturn(active);
    when(user.getId()).thenReturn(id);
    return user;
  }

  private static User withId(User user, long id) {
    try {
      var field = User.class.getDeclaredField("id");
      field.setAccessible(true);
      field.set(user, id);
      return user;
    } catch (ReflectiveOperationException exception) {
      throw new AssertionError(exception);
    }
  }
}
