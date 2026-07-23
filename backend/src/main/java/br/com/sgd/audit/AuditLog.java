package br.com.sgd.audit;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import br.com.sgd.user.User;

@Entity
@Table(name = "auditoria")
public class AuditLog {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "usuario_id")
  private User usuario;

  @Column(name = "data_hora", nullable = false)
  private Instant dataHora = Instant.now();

  @Column(nullable = false)
  private String entidade;

  @Column(nullable = false)
  private String acao;

  @Column private String detalhes;

  protected AuditLog() {}

  public AuditLog(User usuario, String entidade, String acao, String detalhes) {
    this.usuario = usuario;
    this.entidade = entidade;
    this.acao = acao;
    this.detalhes = detalhes;
  }
}
