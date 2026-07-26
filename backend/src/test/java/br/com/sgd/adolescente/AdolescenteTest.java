package br.com.sgd.adolescente;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class AdolescenteTest {
  private static final LocalDate NASCIMENTO = LocalDate.of(2010, 3, 2);
  private static final LocalDate CONSENTIMENTO = LocalDate.of(2026, 1, 1);

  private static ContatosAdolescente contatosDoResponsavel() {
    return ContatosAdolescente.de(null, null, null, null, "Responsável", "(11) 98888-0000");
  }

  private static DadosCadastroAdolescente cadastro(
      CategoriaAdolescente categoria, String motivoAfastamento, ContatosAdolescente contatos) {
    return new DadosCadastroAdolescente(
        "Ana", NASCIMENTO, null, null, CONSENTIMENTO, categoria, null, motivoAfastamento, contatos);
  }

  @Test
  void discipuloGoeExigeMotivo() {
    var dados = cadastro(CategoriaAdolescente.DISCIPULO_GOE, "  ", contatosDoResponsavel());

    assertThatThrownBy(() -> new Adolescente(dados, true))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("motivo do afastamento");
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
  void exigeContatoMinimoDeMaePaiOuResponsavel() {
    assertThatThrownBy(() -> ContatosAdolescente.de(null, null, null, null, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("mãe, ou do pai, ou do responsável");
  }

  @Test
  void exigeNomeETelefoneDoMesmoContato() {
    assertThatThrownBy(() -> ContatosAdolescente.de("Maria", null, null, null, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("nome e telefone da mãe");
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
            contatosDoResponsavel());

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
