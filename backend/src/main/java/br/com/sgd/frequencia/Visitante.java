package br.com.sgd.frequencia;

import java.time.Instant;
import jakarta.persistence.*;

@Entity
@Table(name = "visitantes")
public class Visitante {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "encontro_id", unique = true)
  private Encontro encontro;

  @Column(nullable = false)
  private int quantidade;

  @Column(name = "atualizado_em", nullable = false)
  private Instant atualizadoEm;

  protected Visitante() {}

  public Visitante(Encontro encontro, int quantidade, Instant agora) {
    this.encontro = encontro;
    atualizar(quantidade, agora);
  }

  public void atualizar(int quantidade, Instant agora) {
    if (quantidade < 0) throw new IllegalArgumentException("A quantidade não pode ser negativa.");
    this.quantidade = quantidade;
    this.atualizadoEm = agora;
  }

  public Encontro getEncontro() {
    return encontro;
  }

  public int getQuantidade() {
    return quantidade;
  }
}
