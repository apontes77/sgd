package br.com.sgd.familia;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import br.com.sgd.adolescente.Adolescente;

@Entity
@Table(
    name = "fichas_familia",
    uniqueConstraints =
        @UniqueConstraint(name = "uk_ficha_familia_adolescente", columnNames = "adolescente_id"))
public class FichaFamilia {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "adolescente_id", nullable = false)
  private Adolescente adolescente;

  @Embedded private EnderecoFamilia endereco;

  @Embedded private SituacaoFamiliar situacao;

  @Embedded private ConhecendoFamilia conhecendo;

  @Embedded private ObservacoesFamilia observacoes;

  @Column(name = "criado_em", nullable = false)
  private Instant criadoEm = Instant.now();

  @Column(name = "atualizado_em", nullable = false)
  private Instant atualizadoEm = Instant.now();

  @OneToMany(mappedBy = "ficha", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("ordem ASC")
  private Set<ResponsavelFamilia> responsaveis = new LinkedHashSet<>();

  protected FichaFamilia() {}

  public FichaFamilia(Adolescente adolescente, DadosFicha dados) {
    if (adolescente == null) throw new IllegalArgumentException("O adolescente é obrigatório.");
    this.adolescente = adolescente;
    aplicar(dados);
  }

  public void atualizar(DadosFicha dados) {
    aplicar(dados);
    this.atualizadoEm = Instant.now();
  }

  public void anonimizar() {
    aplicar(dadosNaoConsta());
    this.atualizadoEm = Instant.now();
  }

  private void aplicar(DadosFicha dados) {
    if (dados == null) throw new IllegalArgumentException("Os dados da ficha são obrigatórios.");
    if (dados.responsavel1() == null || dados.responsavel2() == null) {
      throw new IllegalArgumentException("Informe os responsáveis 1 e 2 da ficha de família.");
    }
    this.endereco =
        new EnderecoFamilia(
            dados.cep(),
            dados.rua(),
            dados.numero(),
            dados.complemento(),
            dados.bairro(),
            dados.cidade());
    this.situacao =
        new SituacaoFamiliar(dados.situacaoIgreja(), dados.atuaOnde(), dados.situacaoPais());
    this.conhecendo =
        new ConhecendoFamilia(
            dados.descricao(),
            dados.desafioFinanceiro(),
            dados.desafioEmocional(),
            dados.desafioEspiritual(),
            dados.desafiosDescricao(),
            dados.atividadesJuntas(),
            dados.rotinaSemana(),
            dados.irmaoDokmos(),
            dados.pedidoOracao());
    this.observacoes =
        new ObservacoesFamilia(
            dados.intervencao(), dados.observacaoDiscipulador(), dados.observacaoGerente());
    mesclarResponsaveis(dados.responsavel1(), dados.responsavel2());
  }

  private void mesclarResponsaveis(
      ResponsavelFamilia.DadosResponsavel r1, ResponsavelFamilia.DadosResponsavel r2) {
    ResponsavelFamilia existente1 = porOrdem(1);
    ResponsavelFamilia existente2 = porOrdem(2);
    if (existente1 == null) {
      ResponsavelFamilia novo = new ResponsavelFamilia(1, r1);
      novo.vincularFicha(this);
      responsaveis.add(novo);
    } else {
      existente1.atualizar(r1);
    }
    if (existente2 == null) {
      ResponsavelFamilia novo = new ResponsavelFamilia(2, r2);
      novo.vincularFicha(this);
      responsaveis.add(novo);
    } else {
      existente2.atualizar(r2);
    }
  }

  private ResponsavelFamilia porOrdem(int ordem) {
    for (ResponsavelFamilia responsavel : responsaveis) {
      if (responsavel.getOrdem() == ordem) return responsavel;
    }
    return null;
  }

  public static DadosFicha dadosNaoConsta() {
    ResponsavelFamilia.DadosResponsavel vazio =
        new ResponsavelFamilia.DadosResponsavel(
            FamiliaConstantes.NAO_CONSTA,
            FamiliaConstantes.NAO_CONSTA,
            null,
            FamiliaConstantes.NAO_CONSTA,
            FamiliaConstantes.NAO_CONSTA,
            FamiliaConstantes.NAO_CONSTA,
            FamiliaConstantes.NAO_CONSTA,
            FamiliaConstantes.NAO_CONSTA);
    return new DadosFicha(
        FamiliaConstantes.NAO_CONSTA,
        FamiliaConstantes.NAO_CONSTA,
        FamiliaConstantes.NAO_CONSTA,
        FamiliaConstantes.NAO_CONSTA,
        FamiliaConstantes.NAO_CONSTA,
        FamiliaConstantes.NAO_CONSTA,
        SituacaoIgrejaFamilia.NAO_CONSTA,
        FamiliaConstantes.NAO_CONSTA,
        SituacaoPaisFamilia.NAO_CONSTA,
        FamiliaConstantes.NAO_CONSTA,
        false,
        false,
        false,
        FamiliaConstantes.NAO_CONSTA,
        FamiliaConstantes.NAO_CONSTA,
        FamiliaConstantes.NAO_CONSTA,
        FamiliaConstantes.NAO_CONSTA,
        FamiliaConstantes.NAO_CONSTA,
        FamiliaConstantes.NAO_CONSTA,
        FamiliaConstantes.NAO_CONSTA,
        FamiliaConstantes.NAO_CONSTA,
        vazio,
        vazio);
  }

  public SituacaoFichaFamilia situacaoFicha() {
    for (ResponsavelFamilia responsavel : responsaveis) {
      if (responsavel.preenchido()) return SituacaoFichaFamilia.PREENCHIDA;
    }
    return SituacaoFichaFamilia.NAO_CONSTA;
  }

  public String enderecoResumo() {
    return endereco == null ? FamiliaConstantes.NAO_CONSTA : endereco.resumo();
  }

  public Long getId() {
    return id;
  }

  public Adolescente getAdolescente() {
    return adolescente;
  }

  public String getCep() {
    return endereco.getCep();
  }

  public String getRua() {
    return endereco.getRua();
  }

  public String getNumero() {
    return endereco.getNumero();
  }

  public String getComplemento() {
    return endereco.getComplemento();
  }

  public String getBairro() {
    return endereco.getBairro();
  }

  public String getCidade() {
    return endereco.getCidade();
  }

  public SituacaoIgrejaFamilia getSituacaoIgreja() {
    return situacao.getSituacaoIgreja();
  }

  public String getAtuaOnde() {
    return situacao.getAtuaOnde();
  }

  public SituacaoPaisFamilia getSituacaoPais() {
    return situacao.getSituacaoPais();
  }

  public String getDescricao() {
    return conhecendo.getDescricao();
  }

  public boolean isDesafioFinanceiro() {
    return conhecendo.isDesafioFinanceiro();
  }

  public boolean isDesafioEmocional() {
    return conhecendo.isDesafioEmocional();
  }

  public boolean isDesafioEspiritual() {
    return conhecendo.isDesafioEspiritual();
  }

  public String getDesafiosDescricao() {
    return conhecendo.getDesafiosDescricao();
  }

  public String getAtividadesJuntas() {
    return conhecendo.getAtividadesJuntas();
  }

  public String getRotinaSemana() {
    return conhecendo.getRotinaSemana();
  }

  public String getIrmaoDokmos() {
    return conhecendo.getIrmaoDokmos();
  }

  public String getPedidoOracao() {
    return conhecendo.getPedidoOracao();
  }

  public String getIntervencao() {
    return observacoes.getIntervencao();
  }

  public String getObservacaoDiscipulador() {
    return observacoes.getObservacaoDiscipulador();
  }

  public String getObservacaoGerente() {
    return observacoes.getObservacaoGerente();
  }

  public Instant getCriadoEm() {
    return criadoEm;
  }

  public Instant getAtualizadoEm() {
    return atualizadoEm;
  }

  public List<ResponsavelFamilia> getResponsaveis() {
    return responsaveis.stream()
        .sorted(Comparator.comparingInt(ResponsavelFamilia::getOrdem))
        .toList();
  }

  public ResponsavelFamilia getResponsavel1() {
    return porOrdem(1);
  }

  public ResponsavelFamilia getResponsavel2() {
    return porOrdem(2);
  }

  public record DadosFicha(
      String cep,
      String rua,
      String numero,
      String complemento,
      String bairro,
      String cidade,
      SituacaoIgrejaFamilia situacaoIgreja,
      String atuaOnde,
      SituacaoPaisFamilia situacaoPais,
      String descricao,
      Boolean desafioFinanceiro,
      Boolean desafioEmocional,
      Boolean desafioEspiritual,
      String desafiosDescricao,
      String atividadesJuntas,
      String rotinaSemana,
      String irmaoDokmos,
      String pedidoOracao,
      String intervencao,
      String observacaoDiscipulador,
      String observacaoGerente,
      ResponsavelFamilia.DadosResponsavel responsavel1,
      ResponsavelFamilia.DadosResponsavel responsavel2) {}
}
