package br.com.sgd.familia;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class ConhecendoFamilia {
  @Column(nullable = false, length = 1000)
  private String descricao;

  @Column(name = "desafio_financeiro", nullable = false)
  private boolean desafioFinanceiro;

  @Column(name = "desafio_emocional", nullable = false)
  private boolean desafioEmocional;

  @Column(name = "desafio_espiritual", nullable = false)
  private boolean desafioEspiritual;

  @Column(name = "desafios_descricao", nullable = false, length = 1000)
  private String desafiosDescricao;

  @Column(name = "atividades_juntas", nullable = false, length = 1000)
  private String atividadesJuntas;

  @Column(name = "rotina_semana", nullable = false, length = 1000)
  private String rotinaSemana;

  @Column(name = "irmao_dokmos", nullable = false, length = 200)
  private String irmaoDokmos;

  @Column(name = "pedido_oracao", nullable = false, length = 1000)
  private String pedidoOracao;

  protected ConhecendoFamilia() {}

  public ConhecendoFamilia(
      String descricao,
      Boolean desafioFinanceiro,
      Boolean desafioEmocional,
      Boolean desafioEspiritual,
      String desafiosDescricao,
      String atividadesJuntas,
      String rotinaSemana,
      String irmaoDokmos,
      String pedidoOracao) {
    this.descricao = FamiliaConstantes.exigirTexto(descricao, "A descrição da família", 1000);
    this.desafioFinanceiro = Boolean.TRUE.equals(desafioFinanceiro);
    this.desafioEmocional = Boolean.TRUE.equals(desafioEmocional);
    this.desafioEspiritual = Boolean.TRUE.equals(desafioEspiritual);
    this.desafiosDescricao =
        FamiliaConstantes.exigirTexto(desafiosDescricao, "A descrição dos desafios", 1000);
    this.atividadesJuntas =
        FamiliaConstantes.exigirTexto(atividadesJuntas, "As atividades em família", 1000);
    this.rotinaSemana = FamiliaConstantes.exigirTexto(rotinaSemana, "A rotina da semana", 1000);
    this.irmaoDokmos = FamiliaConstantes.exigirTexto(irmaoDokmos, "O irmão no Dokmos", 200);
    this.pedidoOracao = FamiliaConstantes.exigirTexto(pedidoOracao, "O pedido de oração", 1000);
  }

  public String getDescricao() {
    return descricao;
  }

  public boolean isDesafioFinanceiro() {
    return desafioFinanceiro;
  }

  public boolean isDesafioEmocional() {
    return desafioEmocional;
  }

  public boolean isDesafioEspiritual() {
    return desafioEspiritual;
  }

  public String getDesafiosDescricao() {
    return desafiosDescricao;
  }

  public String getAtividadesJuntas() {
    return atividadesJuntas;
  }

  public String getRotinaSemana() {
    return rotinaSemana;
  }

  public String getIrmaoDokmos() {
    return irmaoDokmos;
  }

  public String getPedidoOracao() {
    return pedidoOracao;
  }
}
