package br.com.sgd.familia;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class EnderecoFamilia {
  @Column(nullable = false, length = 20)
  private String cep;

  @Column(nullable = false, length = 200)
  private String rua;

  @Column(nullable = false, length = 30)
  private String numero;

  @Column(nullable = false, length = 120)
  private String complemento;

  @Column(nullable = false, length = 120)
  private String bairro;

  @Column(nullable = false, length = 120)
  private String cidade;

  protected EnderecoFamilia() {}

  public EnderecoFamilia(
      String cep, String rua, String numero, String complemento, String bairro, String cidade) {
    this.cep = FamiliaConstantes.exigirTexto(cep, "O CEP", 20);
    this.rua = FamiliaConstantes.exigirTexto(rua, "A rua", 200);
    this.numero = FamiliaConstantes.exigirTexto(numero, "O número", 30);
    this.complemento = FamiliaConstantes.exigirTexto(complemento, "O complemento", 120);
    this.bairro = FamiliaConstantes.exigirTexto(bairro, "O bairro", 120);
    this.cidade = FamiliaConstantes.exigirTexto(cidade, "A cidade", 120);
  }

  public String resumo() {
    List<String> partes = new ArrayList<>();
    if (!FamiliaConstantes.isNaoConsta(rua)) partes.add(rua);
    if (!FamiliaConstantes.isNaoConsta(numero)) partes.add(numero);
    if (!FamiliaConstantes.isNaoConsta(bairro)) partes.add(bairro);
    if (!FamiliaConstantes.isNaoConsta(cidade)) partes.add(cidade);
    if (!FamiliaConstantes.isNaoConsta(cep)) partes.add(cep);
    return partes.isEmpty() ? FamiliaConstantes.NAO_CONSTA : String.join(", ", partes);
  }

  public String getCep() {
    return cep;
  }

  public String getRua() {
    return rua;
  }

  public String getNumero() {
    return numero;
  }

  public String getComplemento() {
    return complemento;
  }

  public String getBairro() {
    return bairro;
  }

  public String getCidade() {
    return cidade;
  }
}
