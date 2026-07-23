package br.com.sgd.auth;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OAuthIdentityRepository extends JpaRepository<OAuthIdentity, Long> {
  Optional<OAuthIdentity> findByProviderAndExternalSubject(
      OAuthProvider provider, String externalSubject);

  Optional<OAuthIdentity> findByProviderAndUsuarioId(OAuthProvider provider, long usuarioId);
}
