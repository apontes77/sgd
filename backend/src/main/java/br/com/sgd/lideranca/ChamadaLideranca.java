package br.com.sgd.lideranca;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "chamadas_lideranca",
    uniqueConstraints = @UniqueConstraint(name = "uk_chamada_lideranca_data", columnNames = "data"))
public class ChamadaLideranca {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private LocalDate data;

  @Column(name = "observacao_geral", length = 1000)
  private String observacaoGeral;

  @Column(name = "criado_em", nullable = false)
  private Instant criadoEm = Instant.now();

  @Column(name = "atualizado_em", nullable = false)
  private Instant atualizadoEm = Instant.now();

  @OneToMany(mappedBy = "chamada", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("id ASC")
  private Set<ChamadaLiderancaDiscipulado> itens = new LinkedHashSet<>();

  protected ChamadaLideranca() {}

  public ChamadaLideranca(LocalDate data) {
    if (data == null) throw new IllegalArgumentException("A data da chamada é obrigatória.");
    this.data = data;
  }

  public void atualizarObservacaoGeral(String observacaoGeral, Instant agora) {
    this.observacaoGeral = normalizar(observacaoGeral, 1000);
    this.atualizadoEm = agora;
  }

  public void substituirItens(List<ChamadaLiderancaDiscipulado> novos, Instant agora) {
    itens.clear();
    for (ChamadaLiderancaDiscipulado item : novos) {
      item.vincularChamada(this);
      itens.add(item);
    }
    this.atualizadoEm = agora;
  }

  private static String normalizar(String valor, int max) {
    if (valor == null || valor.isBlank()) return null;
    String trim = valor.trim();
    if (trim.length() > max)
      throw new IllegalArgumentException(
          "A observação geral deve ter no máximo " + max + " caracteres.");
    return trim;
  }

  public Long getId() {
    return id;
  }

  public LocalDate getData() {
    return data;
  }

  public String getObservacaoGeral() {
    return observacaoGeral;
  }

  public Instant getCriadoEm() {
    return criadoEm;
  }

  public Instant getAtualizadoEm() {
    return atualizadoEm;
  }

  public List<ChamadaLiderancaDiscipulado> getItens() {
    return List.copyOf(itens);
  }
}
