package br.com.sgd.lideranca;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import br.com.sgd.organizacao.Discipulado;

@Entity
@Table(
    name = "chamadas_lideranca_discipulados",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_chamada_lideranca_discipulado",
            columnNames = {"chamada_id", "discipulado_id"}))
public class ChamadaLiderancaDiscipulado {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "chamada_id", nullable = false)
  private ChamadaLideranca chamada;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "discipulado_id", nullable = false)
  private Discipulado discipulado;

  @Column(length = 500)
  private String observacao;

  @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("id ASC")
  private Set<PresencaLideranca> presencas = new LinkedHashSet<>();

  protected ChamadaLiderancaDiscipulado() {}

  public ChamadaLiderancaDiscipulado(Discipulado discipulado, String observacao) {
    if (discipulado == null) throw new IllegalArgumentException("O discipulado é obrigatório.");
    this.discipulado = discipulado;
    this.observacao = normalizar(observacao);
  }

  void vincularChamada(ChamadaLideranca chamada) {
    this.chamada = chamada;
  }

  public void substituirPresencas(List<PresencaLideranca> novas) {
    presencas.clear();
    for (PresencaLideranca p : novas) {
      p.vincularItem(this);
      presencas.add(p);
    }
  }

  private static String normalizar(String valor) {
    if (valor == null || valor.isBlank()) return null;
    String trim = valor.trim();
    if (trim.length() > 500)
      throw new IllegalArgumentException("A observação deve ter no máximo 500 caracteres.");
    return trim;
  }

  public Long getId() {
    return id;
  }

  public ChamadaLideranca getChamada() {
    return chamada;
  }

  public Discipulado getDiscipulado() {
    return discipulado;
  }

  public String getObservacao() {
    return observacao;
  }

  public List<PresencaLideranca> getPresencas() {
    return List.copyOf(presencas);
  }
}
