package br.com.sgd.adolescente;

import java.time.Instant;
import java.time.LocalDate;
import jakarta.persistence.*;

@Entity
@Table(name = "adolescentes")
public class Adolescente {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 120)
  private String nome;

  @Column(name = "data_nascimento", nullable = false)
  private LocalDate dataNascimento;

  @Column(length = 40)
  private String telefone;

  @Column(length = 120)
  private String instagram;

  @Column(name = "responsavel_nome", length = 120)
  private String responsavelNome;

  @Column(name = "responsavel_telefone", length = 40)
  private String responsavelTelefone;

  @Column(name = "consentimento_em")
  private LocalDate consentimentoEm;

  @Column(name = "anonimizado_em")
  private Instant anonimizadoEm;

  @Column(nullable = false)
  private boolean ativo = true;

  @Column(name = "criado_em", nullable = false)
  private Instant criadoEm = Instant.now();

  @Column(name = "atualizado_em", nullable = false)
  private Instant atualizadoEm = Instant.now();

  protected Adolescente() {}

  public Adolescente(String nome, LocalDate dataNascimento, String telefone, String instagram) {
    this(nome, dataNascimento, telefone, instagram, null, null, null);
  }

  public Adolescente(
      String nome,
      LocalDate dataNascimento,
      String telefone,
      String instagram,
      String responsavelNome,
      String responsavelTelefone,
      LocalDate consentimentoEm) {
    atualizarDados(
        nome,
        dataNascimento,
        telefone,
        instagram,
        responsavelNome,
        responsavelTelefone,
        consentimentoEm);
  }

  public void atualizar(
      String nome, LocalDate dataNascimento, String telefone, String instagram, Boolean ativo) {
    atualizar(
        nome,
        dataNascimento,
        telefone,
        instagram,
        this.responsavelNome,
        this.responsavelTelefone,
        this.consentimentoEm,
        ativo);
  }

  public void atualizar(
      String nome,
      LocalDate dataNascimento,
      String telefone,
      String instagram,
      String responsavelNome,
      String responsavelTelefone,
      LocalDate consentimentoEm,
      Boolean ativo) {
    atualizarDados(
        nome,
        dataNascimento,
        telefone,
        instagram,
        responsavelNome,
        responsavelTelefone,
        consentimentoEm);
    if (ativo != null) this.ativo = ativo;
    atualizadoEm = Instant.now();
  }

  /**
   * Remove os dados pessoais diretos preservando o histórico agregado de frequência. Atende ao
   * direito de eliminação do titular/responsável (LGPD art. 18) sem descartar as contagens de
   * presença já registradas.
   */
  public void anonimizar() {
    this.nome = "Adolescente anonimizado";
    this.telefone = null;
    this.instagram = null;
    this.responsavelNome = null;
    this.responsavelTelefone = null;
    this.ativo = false;
    this.anonimizadoEm = Instant.now();
    this.atualizadoEm = Instant.now();
  }

  private void atualizarDados(
      String nome,
      LocalDate dataNascimento,
      String telefone,
      String instagram,
      String responsavelNome,
      String responsavelTelefone,
      LocalDate consentimentoEm) {
    if (nome == null || nome.isBlank())
      throw new IllegalArgumentException("O nome do adolescente é obrigatório.");
    if (dataNascimento == null || dataNascimento.isAfter(LocalDate.now()))
      throw new IllegalArgumentException("A data de nascimento é inválida.");
    this.nome = nome.trim();
    this.dataNascimento = dataNascimento;
    this.telefone = normalizar(telefone);
    this.instagram = normalizar(instagram);
    this.responsavelNome = normalizar(responsavelNome);
    this.responsavelTelefone = normalizar(responsavelTelefone);
    this.consentimentoEm = consentimentoEm;
  }

  private static String normalizar(String valor) {
    return valor == null || valor.isBlank() ? null : valor.trim();
  }

  public Long getId() {
    return id;
  }

  public String getNome() {
    return nome;
  }

  public LocalDate getDataNascimento() {
    return dataNascimento;
  }

  public String getTelefone() {
    return telefone;
  }

  public String getInstagram() {
    return instagram;
  }

  public String getResponsavelNome() {
    return responsavelNome;
  }

  public String getResponsavelTelefone() {
    return responsavelTelefone;
  }

  public LocalDate getConsentimentoEm() {
    return consentimentoEm;
  }

  public Instant getAnonimizadoEm() {
    return anonimizadoEm;
  }

  public boolean isAnonimizado() {
    return anonimizadoEm != null;
  }

  public boolean isAtivo() {
    return ativo;
  }
}
