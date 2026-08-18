package br.com.sgd.familia;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
public class SituacaoFamiliar {
  @Enumerated(EnumType.STRING)
  @Column(name = "situacao_igreja", nullable = false, length = 40)
  private SituacaoIgrejaFamilia situacaoIgreja;

  @Column(name = "atua_onde", nullable = false, length = 200)
  private String atuaOnde;

  @Enumerated(EnumType.STRING)
  @Column(name = "situacao_pais", nullable = false, length = 40)
  private SituacaoPaisFamilia situacaoPais;

  protected SituacaoFamiliar() {}

  public SituacaoFamiliar(
      SituacaoIgrejaFamilia situacaoIgreja, String atuaOnde, SituacaoPaisFamilia situacaoPais) {
    this.situacaoIgreja =
        situacaoIgreja == null ? SituacaoIgrejaFamilia.NAO_CONSTA : situacaoIgreja;
    this.atuaOnde = atuaOndeFrom(atuaOnde, this.situacaoIgreja);
    this.situacaoPais = situacaoPais == null ? SituacaoPaisFamilia.NAO_CONSTA : situacaoPais;
  }

  private static String atuaOndeFrom(String atuaOnde, SituacaoIgrejaFamilia situacao) {
    String valor = FamiliaConstantes.exigirTexto(atuaOnde, "O local de atuação", 200);
    if (situacao == SituacaoIgrejaFamilia.FDV_ATUANTES && FamiliaConstantes.isNaoConsta(valor)) {
      throw new IllegalArgumentException(
          "Informe onde a família atua quando pertence à Igreja Fonte da Vida e é atuante.");
    }
    if (situacao != SituacaoIgrejaFamilia.FDV_ATUANTES) {
      return FamiliaConstantes.NAO_CONSTA;
    }
    return valor;
  }

  public SituacaoIgrejaFamilia getSituacaoIgreja() {
    return situacaoIgreja;
  }

  public String getAtuaOnde() {
    return atuaOnde;
  }

  public SituacaoPaisFamilia getSituacaoPais() {
    return situacaoPais;
  }
}
