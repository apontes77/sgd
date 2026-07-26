package br.com.sgd.organizacao;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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

import br.com.sgd.user.User;

@Entity
@Table(name = "gerencias")
public class Gerencia {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 120)
  private String nome;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private Sexo sexo;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "gerencia_faixas_etarias",
      joinColumns = @JoinColumn(name = "gerencia_id"))
  @Enumerated(EnumType.STRING)
  @Column(name = "faixa_etaria", nullable = false, length = 20)
  private Set<FaixaEtaria> faixasEtarias = new LinkedHashSet<>();

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "gerente_id", nullable = false)
  private User gerente;

  @Column(nullable = false)
  private boolean ativo = true;

  @Column(name = "criado_em", nullable = false)
  private Instant criadoEm = Instant.now();

  @Column(name = "atualizado_em", nullable = false)
  private Instant atualizadoEm = Instant.now();

  protected Gerencia() {}

  public Gerencia(String nome, Sexo sexo, Collection<FaixaEtaria> faixasEtarias, User gerente) {
    this.nome = normalizarNome(nome);
    if (sexo == null) throw new IllegalArgumentException("O sexo da gerência é obrigatório.");
    this.sexo = sexo;
    this.faixasEtarias = normalizarFaixas(faixasEtarias);
    this.gerente = gerente;
  }

  public Long getId() {
    return id;
  }

  public String getNome() {
    return nome;
  }

  public Sexo getSexo() {
    return sexo;
  }

  public Set<FaixaEtaria> getFaixasEtarias() {
    return Set.copyOf(faixasEtarias);
  }

  public User getGerente() {
    return gerente;
  }

  public boolean isAtivo() {
    return ativo;
  }

  public Instant getCriadoEm() {
    return criadoEm;
  }

  public Instant getAtualizadoEm() {
    return atualizadoEm;
  }

  public void update(
      String nome, Sexo sexo, Collection<FaixaEtaria> faixasEtarias, User gerente, Boolean ativo) {
    if (nome != null) this.nome = normalizarNome(nome);
    if (sexo != null) this.sexo = sexo;
    if (faixasEtarias != null) this.faixasEtarias = normalizarFaixas(faixasEtarias);
    if (gerente != null) this.gerente = gerente;
    if (ativo != null) this.ativo = ativo;
    this.atualizadoEm = Instant.now();
  }

  private static String normalizarNome(String nome) {
    if (nome == null || nome.isBlank())
      throw new IllegalArgumentException("O nome da gerência é obrigatório.");
    return nome.trim();
  }

  private static Set<FaixaEtaria> normalizarFaixas(Collection<FaixaEtaria> faixasEtarias) {
    if (faixasEtarias == null || faixasEtarias.isEmpty())
      throw new IllegalArgumentException("Informe ao menos uma faixa etária para a gerência.");
    if (faixasEtarias.stream().anyMatch(faixa -> faixa == null))
      throw new IllegalArgumentException("A faixa etária da gerência é obrigatória.");
    return new LinkedHashSet<>(faixasEtarias);
  }
}
