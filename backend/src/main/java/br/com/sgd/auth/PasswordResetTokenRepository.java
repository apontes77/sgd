package br.com.sgd.auth;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, java.util.UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from PasswordResetToken token join fetch token.usuario where token.tokenHash = :tokenHash")
    Optional<PasswordResetToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);
    boolean existsByUsuarioIdAndTipoAndCriadoEmAfter(long usuarioId, PasswordResetToken.Type tipo, Instant criadoEm);
    @Modifying
    @Query("update PasswordResetToken token set token.usadoEm = :now where token.usuario.id = :userId and token.usadoEm is null")
    int invalidateAllByUserId(@Param("userId") long userId, @Param("now") Instant now);
}
