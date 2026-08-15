package br.com.sgd.lideranca;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import br.com.sgd.frequencia.SituacaoFrequencia;
import br.com.sgd.user.User;

@Entity
@Table(
    name = "presencas_lideranca",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_presenca_lideranca_item_usuario",
            columnNames = {"item_id", "usuario_id"}))
public class PresencaLideranca {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "item_id", nullable = false)
  private ChamadaLiderancaDiscipulado item;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "usuario_id", nullable = false)
  private User usuario;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private PapelLideranca papel;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private SituacaoFrequencia situacao;

  protected PresencaLideranca() {}

  public PresencaLideranca(User usuario, PapelLideranca papel, SituacaoFrequencia situacao) {
    if (usuario == null || papel == null || situacao == null)
      throw new IllegalArgumentException("Os dados da presença são obrigatórios.");
    this.usuario = usuario;
    this.papel = papel;
    this.situacao = situacao;
  }

  void vincularItem(ChamadaLiderancaDiscipulado item) {
    this.item = item;
  }

  void atualizar(PapelLideranca papel, SituacaoFrequencia situacao) {
    if (papel == null || situacao == null)
      throw new IllegalArgumentException("Os dados da presença são obrigatórios.");
    this.papel = papel;
    this.situacao = situacao;
  }

  public Long getId() {
    return id;
  }

  public ChamadaLiderancaDiscipulado getItem() {
    return item;
  }

  public User getUsuario() {
    return usuario;
  }

  public PapelLideranca getPapel() {
    return papel;
  }

  public SituacaoFrequencia getSituacao() {
    return situacao;
  }
}
