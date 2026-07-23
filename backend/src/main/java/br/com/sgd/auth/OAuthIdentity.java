package br.com.sgd.auth;

import java.time.Instant;
import jakarta.persistence.*;

import br.com.sgd.user.User;

@Entity
@Table(
    name = "identidades_oauth",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_identidade_oauth_provedor_subject",
          columnNames = {"provedor", "subject_externo"}),
      @UniqueConstraint(
          name = "uk_identidade_oauth_provedor_usuario",
          columnNames = {"provedor", "usuario_id"})
    })
public class OAuthIdentity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "usuario_id", nullable = false)
  private User usuario;

  @Enumerated(EnumType.STRING)
  @Column(name = "provedor", nullable = false, length = 20)
  private OAuthProvider provider;

  @Column(name = "subject_externo", nullable = false, length = 255)
  private String externalSubject;

  @Column(name = "criado_em", nullable = false)
  private Instant createdAt = Instant.now();

  protected OAuthIdentity() {}

  public OAuthIdentity(User user, OAuthProvider provider, String externalSubject) {
    this.usuario = user;
    this.provider = provider;
    this.externalSubject = externalSubject;
  }

  public User getUsuario() {
    return usuario;
  }

  public OAuthProvider getProvider() {
    return provider;
  }

  public String getExternalSubject() {
    return externalSubject;
  }
}
