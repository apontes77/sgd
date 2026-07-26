package br.com.sgd.adolescente;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Contatos da família do adolescente. Exige ao menos um par completo de nome e telefone entre mãe,
 * pai e responsável legal, para que a liderança sempre tenha um adulto a acionar.
 */
@Embeddable
public class ContatosAdolescente {
  @Column(name = "nome_mae", length = 120)
  private String nomeMae;

  @Column(name = "telefone_mae", length = 40)
  private String telefoneMae;

  @Column(name = "nome_pai", length = 120)
  private String nomePai;

  @Column(name = "telefone_pai", length = 40)
  private String telefonePai;

  @Column(name = "responsavel_nome", length = 120)
  private String responsavelNome;

  @Column(name = "responsavel_telefone", length = 40)
  private String responsavelTelefone;

  protected ContatosAdolescente() {}

  private ContatosAdolescente(
      String nomeMae,
      String telefoneMae,
      String nomePai,
      String telefonePai,
      String responsavelNome,
      String responsavelTelefone) {
    this.nomeMae = nomeMae;
    this.telefoneMae = telefoneMae;
    this.nomePai = nomePai;
    this.telefonePai = telefonePai;
    this.responsavelNome = responsavelNome;
    this.responsavelTelefone = responsavelTelefone;
  }

  public static ContatosAdolescente de(
      String nomeMae,
      String telefoneMae,
      String nomePai,
      String telefonePai,
      String responsavelNome,
      String responsavelTelefone) {
    String mae = normalizar(nomeMae);
    String pai = normalizar(nomePai);
    String responsavel = normalizar(responsavelNome);
    String telMae = TelefoneValidator.validarOpcional(telefoneMae, "telefone da mãe");
    String telPai = TelefoneValidator.validarOpcional(telefonePai, "telefone do pai");
    String telResponsavel =
        TelefoneValidator.validarOpcional(responsavelTelefone, "telefone do responsável");

    exigirParCompleto(mae, telMae, "da mãe");
    exigirParCompleto(pai, telPai, "do pai");
    exigirParCompleto(responsavel, telResponsavel, "do responsável");
    if (mae == null && pai == null && responsavel == null) {
      throw new IllegalArgumentException(
          "Informe nome e telefone da mãe, ou do pai, ou do responsável.");
    }
    return new ContatosAdolescente(mae, telMae, pai, telPai, responsavel, telResponsavel);
  }

  private static void exigirParCompleto(String nome, String telefone, String complemento) {
    if ((nome == null) != (telefone == null)) {
      throw new IllegalArgumentException("Informe nome e telefone " + complemento + ".");
    }
  }

  private static String normalizar(String valor) {
    return valor == null || valor.isBlank() ? null : valor.trim();
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

  public String getResponsavelNome() {
    return responsavelNome;
  }

  public String getResponsavelTelefone() {
    return responsavelTelefone;
  }
}
