package br.com.sgd.adolescente;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class AdolescenteTest {
  @Test
  void discipuloGoeExigeMotivo() {
    Adolescente adolescente = new Adolescente("Ana", LocalDate.of(2010, 3, 2), null, null);

    assertThatThrownBy(
            () ->
                adolescente.atualizar(
                    "Ana",
                    LocalDate.of(2010, 3, 2),
                    null,
                    null,
                    "Responsável",
                    "(11) 98888-0000",
                    LocalDate.of(2026, 1, 1),
                    CategoriaAdolescente.DISCIPULO_GOE,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "  ",
                    true))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("motivo do afastamento");
  }

  @Test
  void categoriaNaoGoeLimpaMotivo() {
    Adolescente adolescente = new Adolescente("Ana", LocalDate.of(2010, 3, 2), null, null);
    adolescente.atualizar(
        "Ana",
        LocalDate.of(2010, 3, 2),
        null,
        null,
        "Responsável",
        "(11) 98888-0000",
        LocalDate.of(2026, 1, 1),
        CategoriaAdolescente.DISCIPULO_GOE,
        null,
        null,
        null,
        null,
        null,
        "Afastou-se",
        true);

    adolescente.atualizar(
        "Ana",
        LocalDate.of(2010, 3, 2),
        null,
        null,
        "Responsável",
        "(11) 98888-0000",
        LocalDate.of(2026, 1, 1),
        CategoriaAdolescente.DISCIPULO,
        null,
        null,
        null,
        null,
        null,
        "Afastou-se",
        true);

    assertThat(adolescente.getCategoria()).isEqualTo(CategoriaAdolescente.DISCIPULO);
    assertThat(adolescente.getMotivoAfastamento()).isNull();
  }

  @Test
  void exigeContatoMinimoDeMaePaiOuResponsavel() {
    Adolescente adolescente = new Adolescente("Ana", LocalDate.of(2010, 3, 2), null, null);

    assertThatThrownBy(
            () ->
                adolescente.atualizar(
                    "Ana",
                    LocalDate.of(2010, 3, 2),
                    null,
                    null,
                    null,
                    null,
                    LocalDate.of(2026, 1, 1),
                    CategoriaAdolescente.DISCIPULO,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    true))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("mãe, ou do pai, ou do responsável");
  }

  @Test
  void rejeitaTelefoneInvalido() {
    Adolescente adolescente = new Adolescente("Ana", LocalDate.of(2010, 3, 2), null, null);

    assertThatThrownBy(
            () ->
                adolescente.atualizar(
                    "Ana",
                    LocalDate.of(2010, 3, 2),
                    "123",
                    null,
                    "Responsável",
                    "(11) 98888-0000",
                    LocalDate.of(2026, 1, 1),
                    CategoriaAdolescente.DISCIPULO,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    true))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("telefone do adolescente");
  }

  @Test
  void aceitaCadastroComNomeETelefoneDaMae() {
    Adolescente adolescente = new Adolescente("Ana", LocalDate.of(2010, 3, 2), null, null);
    adolescente.atualizar(
        "Ana",
        LocalDate.of(2010, 3, 2),
        null,
        null,
        null,
        null,
        LocalDate.of(2026, 1, 1),
        CategoriaAdolescente.DISCIPULO,
        "Maria",
        "(11) 91234-5678",
        null,
        null,
        null,
        null,
        true);

    assertThat(adolescente.getNomeMae()).isEqualTo("Maria");
    assertThat(adolescente.getTelefoneMae()).isEqualTo("(11) 91234-5678");
    assertThat(adolescente.getResponsavelNome()).isNull();
  }
}
