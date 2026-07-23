package br.com.sgd.adolescente;

import java.time.LocalDate;
import jakarta.persistence.*;

import br.com.sgd.organizacao.Discipulado;

@Entity
@Table(name = "vinculos_adolescente_discipulado")
public class VinculoAdolescenteDiscipulado {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "adolescente_id", nullable = false)
  private Adolescente adolescente;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "discipulado_id", nullable = false)
  private Discipulado discipulado;

  @Column(name = "data_inicio", nullable = false)
  private LocalDate dataInicio;

  @Column(name = "data_fim")
  private LocalDate dataFim;

  @Column(nullable = false)
  private boolean ativo = true;

  protected VinculoAdolescenteDiscipulado() {}

  public VinculoAdolescenteDiscipulado(
      Adolescente adolescente, Discipulado discipulado, LocalDate dataInicio) {
    if (adolescente == null || discipulado == null || dataInicio == null)
      throw new IllegalArgumentException("Os dados do vínculo são obrigatórios.");
    this.adolescente = adolescente;
    this.discipulado = discipulado;
    this.dataInicio = dataInicio;
  }

  public void encerrar(LocalDate dataFim) {
    if (dataFim == null || dataFim.isBefore(dataInicio))
      throw new IllegalArgumentException(
          "A data final não pode ser anterior ao início do vínculo.");
    this.dataFim = dataFim;
    this.ativo = false;
  }

  public Long getId() {
    return id;
  }

  public Adolescente getAdolescente() {
    return adolescente;
  }

  public Discipulado getDiscipulado() {
    return discipulado;
  }

  public LocalDate getDataInicio() {
    return dataInicio;
  }

  public LocalDate getDataFim() {
    return dataFim;
  }

  public boolean isAtivo() {
    return ativo;
  }
}
