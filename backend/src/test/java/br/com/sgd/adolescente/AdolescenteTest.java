package br.com.sgd.adolescente;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class AdolescenteTest {
  private static final LocalDate NASCIMENTO = LocalDate.of(2010, 3, 2);
  private static final LocalDate CONSENTIMENTO = LocalDate.of(2026, 1, 1);
  private static final String TELEFONE = "(11) 91234-5678";

  private static ContatosAdolescente contatosDoResponsavel() {
    return ContatosAdolescente.de(null, null, null, null, "Responsável", "(11) 98888-0000");
  }

  private static ContatosAdolescente contatosVazios() {
    return ContatosAdolescente.de(null, null, null, null, null, null);
  }

  private static DadosCadastroAdolescente cadastro(
      CategoriaAdolescente categoria,
      String motivoAfastamento,
      ContatosAdolescente contatos,
      String telefone) {
    return cadastro(categoria, motivoAfastamento, contatos, telefone, false, false);
  }

  private static DadosCadastroAdolescente cadastro(
      CategoriaAdolescente categoria,
      String motivoAfastamento,
      ContatosAdolescente contatos,
      String telefone,
      boolean naoPossuiTelefone) {
    return cadastro(categoria, motivoAfastamento, contatos, telefone, naoPossuiTelefone, false);
  }

  private static DadosCadastroAdolescente cadastro(
      CategoriaAdolescente categoria,
      String motivoAfastamento,
      ContatosAdolescente contatos,
      String telefone,
      boolean naoPossuiTelefone,
      boolean naoPossuiContatoFamiliar) {
    return new DadosCadastroAdolescente(
        "Ana",
        NASCIMENTO,
        telefone,
        null,
        CONSENTIMENTO,
        categoria,
        null,
        motivoAfastamento,
        contatos,
        naoPossuiTelefone,
        naoPossuiContatoFamiliar);
  }

  private static DadosCadastroAdolescente cadastro(
      CategoriaAdolescente categoria, String motivoAfastamento, ContatosAdolescente contatos) {
    String telefone = categoria == CategoriaAdolescente.DISCIPULO_GOE ? TELEFONE : null;
    return cadastro(categoria, motivoAfastamento, contatos, telefone);
  }

  @Test
  void discipuloGoeExigeMotivo() {
    var dados = cadastro(CategoriaAdolescente.DISCIPULO_GOE, "  ", contatosDoResponsavel());

    assertThatThrownBy(() -> new Adolescente(dados, true))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("motivo do afastamento");
  }

  @Test
  void discipuloGoeExigeTelefoneDoAdolescente() {
    var dados = cadastro(CategoriaAdolescente.DISCIPULO_GOE, "Afastou-se", contatosVazios(), null);

    assertThatThrownBy(() -> new Adolescente(dados, true))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("telefone do adolescente");
  }

  @Test
  void discipuloGoeAceitaSemTelefoneQuandoMarcadoQueNaoPossui() {
    Adolescente adolescente =
        new Adolescente(
            cadastro(
                CategoriaAdolescente.DISCIPULO_GOE, "Afastou-se", contatosVazios(), null, true),
            true);

    assertThat(adolescente.getCategoria()).isEqualTo(CategoriaAdolescente.DISCIPULO_GOE);
    assertThat(adolescente.getTelefone()).isNull();
  }

  @Test
  void discipuloGoeAceitaSemContatoFamiliarQuandoTemTelefone() {
    Adolescente adolescente =
        new Adolescente(
            cadastro(CategoriaAdolescente.DISCIPULO_GOE, "Afastou-se", contatosVazios(), TELEFONE),
            true);

    assertThat(adolescente.getCategoria()).isEqualTo(CategoriaAdolescente.DISCIPULO_GOE);
    assertThat(adolescente.getTelefone()).isEqualTo(TELEFONE);
    assertThat(adolescente.getResponsavelNome()).isNull();
    assertThat(adolescente.getNomeMae()).isNull();
    assertThat(adolescente.getNomePai()).isNull();
  }

  @Test
  void categoriaNaoGoeLimpaMotivo() {
    Adolescente adolescente =
        new Adolescente(
            cadastro(CategoriaAdolescente.DISCIPULO_GOE, "Afastou-se", contatosDoResponsavel()),
            true);

    adolescente.atualizar(
        cadastro(CategoriaAdolescente.DISCIPULO, "Afastou-se", contatosDoResponsavel()), true);

    assertThat(adolescente.getCategoria()).isEqualTo(CategoriaAdolescente.DISCIPULO);
    assertThat(adolescente.getMotivoAfastamento()).isNull();
  }

  @Test
  void promoveVisitanteParaDiscipulo() {
    Adolescente adolescente =
        new Adolescente(
            cadastro(CategoriaAdolescente.VISITANTE, null, contatosDoResponsavel()), true);

    adolescente.promoverDeVisitanteParaDiscipulo();

    assertThat(adolescente.getCategoria()).isEqualTo(CategoriaAdolescente.DISCIPULO);
  }

  @Test
  void rejeitaPromocaoQuandoNaoEVisitante() {
    Adolescente adolescente =
        new Adolescente(
            cadastro(CategoriaAdolescente.DISCIPULO, null, contatosDoResponsavel()), true);

    assertThatThrownBy(adolescente::promoverDeVisitanteParaDiscipulo)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Somente visitante");
  }

  @Test
  void exigeContatoMinimoDeMaePaiOuResponsavelParaNaoGoe() {
    assertThatThrownBy(
            () ->
                new Adolescente(
                    cadastro(CategoriaAdolescente.DISCIPULO, null, contatosVazios()), true))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("mãe, ou do pai, ou do responsável");
  }

  @Test
  void aceitaCadastroSemContatoFamiliarQuandoMarcadoQueNaoPossui() {
    Adolescente adolescente =
        new Adolescente(
            cadastro(CategoriaAdolescente.DISCIPULO, null, contatosVazios(), null, false, true),
            true);

    assertThat(adolescente.getCategoria()).isEqualTo(CategoriaAdolescente.DISCIPULO);
    assertThat(adolescente.getNomeMae()).isNull();
    assertThat(adolescente.getNomePai()).isNull();
    assertThat(adolescente.getResponsavelNome()).isNull();
  }

  @Test
  void exigeNomeETelefoneDoMesmoContato() {
    assertThatThrownBy(() -> ContatosAdolescente.de("Maria", null, null, null, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("nome e telefone da mãe");
  }

  @Test
  void contatosVaziosSaoPermitidosNoEmbeddable() {
    ContatosAdolescente contatos = contatosVazios();

    assertThat(contatos.temContatoFamiliar()).isFalse();
  }

  @Test
  void rejeitaTelefoneInvalido() {
    var dados =
        new DadosCadastroAdolescente(
            "Ana",
            NASCIMENTO,
            "123",
            null,
            CONSENTIMENTO,
            CategoriaAdolescente.DISCIPULO,
            null,
            null,
            contatosDoResponsavel(),
            false,
            false);

    assertThatThrownBy(() -> new Adolescente(dados, true))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("telefone do adolescente");
  }

  @Test
  void aceitaCadastroComNomeETelefoneDaMae() {
    var contatos = ContatosAdolescente.de("Maria", "(11) 91234-5678", null, null, null, null);

    Adolescente adolescente =
        new Adolescente(cadastro(CategoriaAdolescente.DISCIPULO, null, contatos), true);

    assertThat(adolescente.getNomeMae()).isEqualTo("Maria");
    assertThat(adolescente.getTelefoneMae()).isEqualTo("(11) 91234-5678");
    assertThat(adolescente.getResponsavelNome()).isNull();
  }

  @Test
  void anonimizarLimpaTodosOsContatos() {
    Adolescente adolescente =
        new Adolescente(
            cadastro(CategoriaAdolescente.DISCIPULO, null, contatosDoResponsavel()), true);

    adolescente.anonimizar();

    assertThat(adolescente.getResponsavelNome()).isNull();
    assertThat(adolescente.getResponsavelTelefone()).isNull();
    assertThat(adolescente.getTelefoneMae()).isNull();
    assertThat(adolescente.getNomePai()).isNull();
  }
}
