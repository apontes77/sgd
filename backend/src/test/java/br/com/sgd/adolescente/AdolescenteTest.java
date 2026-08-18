package br.com.sgd.adolescente;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class AdolescenteTest {
  private static DadosCadastroAdolescente dados(
      CategoriaAdolescente categoria, String telefone, String motivo, boolean naoPossuiTelefone) {
    return new DadosCadastroAdolescente(
        "Ana",
        LocalDate.of(2010, 3, 2),
        telefone,
        "@ana",
        LocalDate.of(2026, 1, 1),
        categoria,
        "Núcleo A",
        motivo,
        naoPossuiTelefone);
  }

  @Test
  void criaDiscipuloSemExigirContatoFamiliarNoAdolescente() {
    Adolescente adolescente =
        new Adolescente(dados(CategoriaAdolescente.DISCIPULO, null, null, true), true);
    assertThat(adolescente.getNome()).isEqualTo("Ana");
    assertThat(adolescente.getCategoria()).isEqualTo(CategoriaAdolescente.DISCIPULO);
    assertThat(adolescente.getTelefone()).isNull();
  }

  @Test
  void goeExigeTelefoneOuFlag() {
    assertThatThrownBy(
            () ->
                new Adolescente(
                    dados(CategoriaAdolescente.DISCIPULO_GOE, null, "Motivo", false), true))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("telefone do adolescente");
  }

  @Test
  void goeAceitaSemTelefoneComFlag() {
    Adolescente adolescente =
        new Adolescente(dados(CategoriaAdolescente.DISCIPULO_GOE, null, "Motivo", true), true);
    assertThat(adolescente.getTelefone()).isNull();
    assertThat(adolescente.getMotivoAfastamento()).isEqualTo("Motivo");
  }

  @Test
  void goeExigeMotivo() {
    assertThatThrownBy(
            () ->
                new Adolescente(
                    dados(CategoriaAdolescente.DISCIPULO_GOE, "(11) 91234-5678", null, false),
                    true))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("motivo do afastamento");
  }

  @Test
  void promoveVisitanteParaDiscipulo() {
    Adolescente adolescente =
        new Adolescente(dados(CategoriaAdolescente.VISITANTE, null, null, true), true);
    adolescente.promoverDeVisitanteParaDiscipulo();
    assertThat(adolescente.getCategoria()).isEqualTo(CategoriaAdolescente.DISCIPULO);
  }

  @Test
  void anonimizarLimpaDadosPessoais() {
    Adolescente adolescente =
        new Adolescente(
            dados(CategoriaAdolescente.DISCIPULO_GOE, "(11) 91234-5678", "Mudou", false), true);
    adolescente.anonimizar();
    assertThat(adolescente.getNome()).isEqualTo("Adolescente anonimizado");
    assertThat(adolescente.getTelefone()).isNull();
    assertThat(adolescente.getInstagram()).isNull();
    assertThat(adolescente.getEstrutura()).isNull();
    assertThat(adolescente.getMotivoAfastamento()).isNull();
    assertThat(adolescente.isAtivo()).isFalse();
    assertThat(adolescente.isAnonimizado()).isTrue();
  }
}
