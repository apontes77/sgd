package br.com.sgd.auth;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, java.util.UUID> {
  Optional<RefreshToken> findByTokenHash(String tokenHash);

  @Modifying
  @Query(
      "update RefreshToken token set token.revogadoEm = :now where token.usuario.id = :userId and token.revogadoEm is null")
  int revokeAllByUserId(@Param("userId") long userId, @Param("now") Instant now);
}
