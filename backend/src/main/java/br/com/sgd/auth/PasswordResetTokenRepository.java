package br.com.sgd.auth;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasswordResetTokenRepository
    extends JpaRepository<PasswordResetToken, java.util.UUID> {
  Optional<PasswordResetToken> findByTokenHash(String tokenHash);

  @Modifying
  @Query(
      "update PasswordResetToken token set token.usadoEm = :now where token.usuario.id = :userId and token.usadoEm is null")
  int invalidateAllByUserId(@Param("userId") long userId, @Param("now") Instant now);
}
