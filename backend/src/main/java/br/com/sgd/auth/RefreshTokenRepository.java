package br.com.sgd.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, java.util.UUID> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
}
