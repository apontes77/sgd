package br.com.sgd.frequencia;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;

import org.junit.jupiter.api.Test;

class EncontroJanelaTest {
  @Test
  void encontroMantemInstanteQueDefineAJanela() {
    var instante = Instant.parse("2026-07-12T00:00:00Z");
    var encontro =
        new Encontro(
            nullSafeDiscipulado(), LocalDate.of(2026, 7, 11), SituacaoEncontro.REALIZADO, instante);
    assertEquals(instante, encontro.getCriadoEm());
  }

  private static br.com.sgd.organizacao.Discipulado nullSafeDiscipulado() {
    try {
      var c = br.com.sgd.organizacao.Discipulado.class.getDeclaredConstructor();
      c.setAccessible(true);
      return c.newInstance();
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }
}
