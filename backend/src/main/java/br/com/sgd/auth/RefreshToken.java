package br.com.sgd.auth;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import br.com.sgd.user.User;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {
  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "usuario_id", nullable = false)
  private User usuario;

  @Column(name = "token_hash", nullable = false, unique = true, length = 64)
  private String tokenHash;

  @Column(name = "expira_em", nullable = false)
  private Instant expiraEm;

  @Column(name = "revogado_em")
  private Instant revogadoEm;

  protected RefreshToken() {}

  public RefreshToken(User usuario, String tokenHash, Instant expiraEm) {
    this.id = UUID.randomUUID();
    this.usuario = usuario;
    this.tokenHash = tokenHash;
    this.expiraEm = expiraEm;
  }

  public User getUsuario() {
    return usuario;
  }

  public String getTokenHash() {
    return tokenHash;
  }

  public boolean isValid() {
    return revogadoEm == null && expiraEm.isAfter(Instant.now());
  }

  public void revoke() {
    revogadoEm = Instant.now();
  }
}
