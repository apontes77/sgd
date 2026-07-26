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

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private CategoriaAdolescente categoria = CategoriaAdolescente.DISCIPULO;

  @Column(name = "nome_mae", length = 120)
  private String nomeMae;

  @Column(name = "telefone_mae", length = 40)
  private String telefoneMae;

  @Column(name = "nome_pai", length = 120)
  private String nomePai;

  @Column(name = "telefone_pai", length = 40)
  private String telefonePai;

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

  public Adolescente(String nome, LocalDate dataNascimento, String telefone, String instagram) {
    if (nome == null || nome.isBlank())
      throw new IllegalArgumentException("O nome do adolescente é obrigatório.");
    if (dataNascimento == null || dataNascimento.isAfter(LocalDate.now()))
      throw new IllegalArgumentException("A data de nascimento é inválida.");
    this.nome = nome.trim();
    this.dataNascimento = dataNascimento;
    this.telefone = TelefoneValidator.validarOpcional(telefone, "telefone do adolescente");
    this.instagram = normalizar(instagram);
  }

  public Adolescente(
      String nome,
      LocalDate dataNascimento,
      String telefone,
      String instagram,
      String responsavelNome,
      String responsavelTelefone,
      LocalDate consentimentoEm) {
    this(nome, dataNascimento, telefone, instagram);
    atualizar(
        nome,
        dataNascimento,
        telefone,
        instagram,
        responsavelNome,
        responsavelTelefone,
        consentimentoEm,
        CategoriaAdolescente.DISCIPULO,
        null,
        null,
        null,
        null,
        null,
        null,
        true);
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
        this.categoria,
        this.nomeMae,
        this.telefoneMae,
        this.nomePai,
        this.telefonePai,
        this.estrutura,
        this.motivoAfastamento,
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
      CategoriaAdolescente categoria,
      String nomeMae,
      String telefoneMae,
      String nomePai,
      String telefonePai,
      String estrutura,
      String motivoAfastamento,
      Boolean ativo) {
    atualizarDados(
        nome,
        dataNascimento,
        telefone,
        instagram,
        responsavelNome,
        responsavelTelefone,
        consentimentoEm,
        categoria,
        nomeMae,
        telefoneMae,
        nomePai,
        telefonePai,
        estrutura,
        motivoAfastamento);
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
    this.nomeMae = null;
    this.telefoneMae = null;
    this.nomePai = null;
    this.telefonePai = null;
    this.estrutura = null;
    this.motivoAfastamento = null;
    this.categoria = CategoriaAdolescente.DISCIPULO;
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
      LocalDate consentimentoEm,
      CategoriaAdolescente categoria,
      String nomeMae,
      String telefoneMae,
      String nomePai,
      String telefonePai,
      String estrutura,
      String motivoAfastamento) {
    if (nome == null || nome.isBlank())
      throw new IllegalArgumentException("O nome do adolescente é obrigatório.");
    if (dataNascimento == null || dataNascimento.isAfter(LocalDate.now()))
      throw new IllegalArgumentException("A data de nascimento é inválida.");
    if (categoria == null)
      throw new IllegalArgumentException("A categoria do adolescente é obrigatória.");
    String motivo = normalizar(motivoAfastamento);
    if (categoria == CategoriaAdolescente.DISCIPULO_GOE) {
      if (motivo == null)
        throw new IllegalArgumentException(
            "O motivo do afastamento é obrigatório para discípulo GOE.");
    } else {
      motivo = null;
    }

    String tel = TelefoneValidator.validarOpcional(telefone, "telefone do adolescente");
    String telMae = TelefoneValidator.validarOpcional(telefoneMae, "telefone da mãe");
    String telPai = TelefoneValidator.validarOpcional(telefonePai, "telefone do pai");
    String telResp =
        TelefoneValidator.validarOpcional(responsavelTelefone, "telefone do responsável");
    String mae = normalizar(nomeMae);
    String pai = normalizar(nomePai);
    String resp = normalizar(responsavelNome);

    validarParContato(mae, telMae, "da mãe");
    validarParContato(pai, telPai, "do pai");
    validarParContato(resp, telResp, "do responsável");

    boolean maeOk = mae != null && telMae != null;
    boolean paiOk = pai != null && telPai != null;
    boolean respOk = resp != null && telResp != null;
    if (!maeOk && !paiOk && !respOk) {
      throw new IllegalArgumentException(
          "Informe nome e telefone da mãe, ou do pai, ou do responsável.");
    }

    this.nome = nome.trim();
    this.dataNascimento = dataNascimento;
    this.telefone = tel;
    this.instagram = normalizar(instagram);
    this.responsavelNome = resp;
    this.responsavelTelefone = telResp;
    this.consentimentoEm = consentimentoEm;
    this.categoria = categoria;
    this.nomeMae = mae;
    this.telefoneMae = telMae;
    this.nomePai = pai;
    this.telefonePai = telPai;
    this.estrutura = normalizar(estrutura);
    this.motivoAfastamento = motivo;
  }

  private static void validarParContato(String nomeContato, String telefone, String complemento) {
    boolean temNome = nomeContato != null;
    boolean temTel = telefone != null;
    if (temNome != temTel) {
      throw new IllegalArgumentException("Informe nome e telefone " + complemento + ".");
    }
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

  public CategoriaAdolescente getCategoria() {
    return categoria;
  }

  public String getNomeMae() {
    return nomeMae;
  }

  public String getTelefoneMae() {
    return telefoneMae;
  }

  public String getNomePai() {
    return nomePai;
  }

  public String getTelefonePai() {
    return telefonePai;
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
