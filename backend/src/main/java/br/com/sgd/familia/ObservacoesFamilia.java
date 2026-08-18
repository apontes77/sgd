package br.com.sgd.familia;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class ObservacoesFamilia {
  @Column(nullable = false, length = 2000)
  private String intervencao;

  @Column(name = "observacao_discipulador", nullable = false, length = 1000)
  private String observacaoDiscipulador;

  @Column(name = "observacao_gerente", nullable = false, length = 1000)
  private String observacaoGerente;

  protected ObservacoesFamilia() {}

  public ObservacoesFamilia(
      String intervencao, String observacaoDiscipulador, String observacaoGerente) {
    this.intervencao = FamiliaConstantes.exigirTexto(intervencao, "A intervenção", 2000);
    this.observacaoDiscipulador =
        FamiliaConstantes.exigirTexto(observacaoDiscipulador, "A observação do discipulador", 1000);
    this.observacaoGerente =
        FamiliaConstantes.exigirTexto(observacaoGerente, "A observação do gerente", 1000);
  }

  public String getIntervencao() {
    return intervencao;
  }

  public String getObservacaoDiscipulador() {
    return observacaoDiscipulador;
  }

  public String getObservacaoGerente() {
    return observacaoGerente;
  }
}
