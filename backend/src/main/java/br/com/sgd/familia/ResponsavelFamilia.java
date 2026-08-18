package br.com.sgd.familia;

import java.time.LocalDate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
    name = "ficha_familia_responsaveis",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_ficha_familia_responsavel_ordem",
            columnNames = {"ficha_id", "ordem"}))
public class ResponsavelFamilia {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "ficha_id", nullable = false)
  private FichaFamilia ficha;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(nullable = false)
  private int ordem;

  @Column(nullable = false, length = 120)
  private String nome;

  @Column(nullable = false, length = 80)
  private String parentesco;

  @Column(name = "data_nascimento")
  private LocalDate dataNascimento;

  @Column(name = "estado_civil", nullable = false, length = 80)
  private String estadoCivil;

  @Column(nullable = false, length = 120)
  private String profissao;

  @Column(nullable = false, length = 40)
  private String telefone;

  @Column(nullable = false, length = 120)
  private String email;

  @Column(name = "interesse_pessoal", nullable = false, length = 200)
  private String interessePessoal;

  protected ResponsavelFamilia() {}

  public ResponsavelFamilia(int ordem, DadosResponsavel dados) {
    if (ordem != 1 && ordem != 2) {
      throw new IllegalArgumentException("A ordem do responsável deve ser 1 ou 2.");
    }
    this.ordem = ordem;
    aplicar(dados);
  }

  void vincularFicha(FichaFamilia ficha) {
    this.ficha = ficha;
  }

  void atualizar(DadosResponsavel dados) {
    aplicar(dados);
  }

  private void aplicar(DadosResponsavel dados) {
    if (dados == null)
      throw new IllegalArgumentException("Os dados do responsável são obrigatórios.");
    this.nome = FamiliaConstantes.exigirTexto(dados.nome(), "O nome do responsável", 120);
    this.parentesco =
        FamiliaConstantes.exigirTexto(dados.parentesco(), "O parentesco do responsável", 80);
    this.dataNascimento = dados.dataNascimento();
    this.estadoCivil =
        FamiliaConstantes.exigirTexto(dados.estadoCivil(), "O estado civil do responsável", 80);
    this.profissao =
        FamiliaConstantes.exigirTexto(dados.profissao(), "A profissão do responsável", 120);
    this.telefone = telefoneFrom(dados.telefone());
    this.email = emailFrom(dados.email());
    this.interessePessoal =
        FamiliaConstantes.exigirTexto(
            dados.interessePessoal(), "O interesse pessoal do responsável", 200);
  }

  private static String telefoneFrom(String telefone) {
    String valor = FamiliaConstantes.exigirTexto(telefone, "O telefone do responsável", 40);
    if (FamiliaConstantes.isNaoConsta(valor)) return FamiliaConstantes.NAO_CONSTA;
    return TelefoneFamiliaValidator.validar(valor, "telefone do responsável");
  }

  private static String emailFrom(String email) {
    String valor = FamiliaConstantes.exigirTexto(email, "O e-mail do responsável", 120);
    if (FamiliaConstantes.isNaoConsta(valor)) return FamiliaConstantes.NAO_CONSTA;
    if (!valor.contains("@") || valor.length() < 5) {
      throw new IllegalArgumentException("Informe um e-mail válido do responsável.");
    }
    return valor;
  }

  public boolean preenchido() {
    return !FamiliaConstantes.isNaoConsta(nome);
  }

  public Long getId() {
    return id;
  }

  public int getOrdem() {
    return ordem;
  }

  public String getNome() {
    return nome;
  }

  public String getParentesco() {
    return parentesco;
  }

  public LocalDate getDataNascimento() {
    return dataNascimento;
  }

  public String getEstadoCivil() {
    return estadoCivil;
  }

  public String getProfissao() {
    return profissao;
  }

  public String getTelefone() {
    return telefone;
  }

  public String getEmail() {
    return email;
  }

  public String getInteressePessoal() {
    return interessePessoal;
  }

  public record DadosResponsavel(
      String nome,
      String parentesco,
      LocalDate dataNascimento,
      String estadoCivil,
      String profissao,
      String telefone,
      String email,
      String interessePessoal) {}
}
