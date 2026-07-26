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

  @Column(name = "consentimento_em")
  private LocalDate consentimentoEm;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private CategoriaAdolescente categoria = CategoriaAdolescente.DISCIPULO;

  @Embedded private ContatosAdolescente contatos;

  @Column(length = 120)
  private String estrutura;

  @Column(name = "motivo_afastamento", length = 500)
  private String motivoAfastamento;

  @Column(name = "anonimizado_em")
  private Instant anonimizadoEm;

  @Column(nullable = false)
  private boolean ativo = true;

  @Column(name = "criado_em", nullable = false)
  private Instant criadoEm = Instant.now();

  @Column(name = "atualizado_em", nullable = false)
  private Instant atualizadoEm = Instant.now();

  protected Adolescente() {}

  public Adolescente(DadosCadastroAdolescente dados, Boolean ativo) {
    atualizarDados(dados);
    if (ativo != null) this.ativo = ativo;
  }

  /** Atualiza apenas os dados básicos, preservando contatos, consentimento e categoria atuais. */
  public void atualizar(
      String nome, LocalDate dataNascimento, String telefone, String instagram, Boolean ativo) {
    atualizar(
        new DadosCadastroAdolescente(
            nome,
            dataNascimento,
            telefone,
            instagram,
            consentimentoEm,
            categoria,
            estrutura,
            motivoAfastamento,
            contatos),
        ativo);
  }

  public void atualizar(DadosCadastroAdolescente dados, Boolean ativo) {
    atualizarDados(dados);
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
    this.contatos = null;
    this.estrutura = null;
    this.motivoAfastamento = null;
    this.categoria = CategoriaAdolescente.DISCIPULO;
    this.ativo = false;
    this.anonimizadoEm = Instant.now();
    this.atualizadoEm = Instant.now();
  }

  private void atualizarDados(DadosCadastroAdolescente dados) {
    if (dados.nome() == null || dados.nome().isBlank())
      throw new IllegalArgumentException("O nome do adolescente é obrigatório.");
    if (dados.dataNascimento() == null || dados.dataNascimento().isAfter(LocalDate.now()))
      throw new IllegalArgumentException("A data de nascimento é inválida.");
    if (dados.categoria() == null)
      throw new IllegalArgumentException("A categoria do adolescente é obrigatória.");
    if (dados.contatos() == null)
      throw new IllegalArgumentException(
          "Informe nome e telefone da mãe, ou do pai, ou do responsável.");

    this.nome = dados.nome().trim();
    this.dataNascimento = dados.dataNascimento();
    this.telefone = TelefoneValidator.validarOpcional(dados.telefone(), "telefone do adolescente");
    this.instagram = normalizar(dados.instagram());
    this.consentimentoEm = dados.consentimentoEm();
    this.categoria = dados.categoria();
    this.contatos = dados.contatos();
    this.estrutura = normalizar(dados.estrutura());
    this.motivoAfastamento = motivoExigidoPelaCategoria(dados);
  }

  /** O motivo do afastamento só faz sentido para discípulo GOE, então é limpo nas demais. */
  private static String motivoExigidoPelaCategoria(DadosCadastroAdolescente dados) {
    String motivo = normalizar(dados.motivoAfastamento());
    if (dados.categoria() != CategoriaAdolescente.DISCIPULO_GOE) return null;
    if (motivo == null)
      throw new IllegalArgumentException(
          "O motivo do afastamento é obrigatório para discípulo GOE.");
    return motivo;
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
    return contatos == null ? null : contatos.getResponsavelNome();
  }

  public String getResponsavelTelefone() {
    return contatos == null ? null : contatos.getResponsavelTelefone();
  }

  public LocalDate getConsentimentoEm() {
    return consentimentoEm;
  }

  public CategoriaAdolescente getCategoria() {
    return categoria;
  }

  public String getNomeMae() {
    return contatos == null ? null : contatos.getNomeMae();
  }

  public String getTelefoneMae() {
    return contatos == null ? null : contatos.getTelefoneMae();
  }

  public String getNomePai() {
    return contatos == null ? null : contatos.getNomePai();
  }

  public String getTelefonePai() {
    return contatos == null ? null : contatos.getTelefonePai();
  }

  public String getEstrutura() {
    return estrutura;
  }

  public String getMotivoAfastamento() {
    return motivoAfastamento;
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
