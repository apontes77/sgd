package br.com.sgd.frequencia;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;

import org.junit.jupiter.api.Test;

class EncontroJanelaTest {
  @Test
  void encontroMantemInstanteDeCriacaoEAncoraJanelaNaChamada() {
    var criado = Instant.parse("2026-07-12T00:00:00Z");
    var chamada = Instant.parse("2026-07-12T02:00:00Z");
    var encontro =
        new Encontro(
            nullSafeDiscipulado(), LocalDate.of(2026, 7, 11), SituacaoEncontro.REALIZADO, criado);
    assertEquals(criado, encontro.getCriadoEm());
    assertEquals(criado, encontro.getAtualizadoEm());
    assertEquals(null, encontro.getChamadaSalvaEm());
    encontro.marcarChamadaSalva(chamada);
    assertEquals(chamada, encontro.getChamadaSalvaEm());
    assertEquals(chamada, encontro.getAtualizadoEm());
    var segunda = chamada.plusSeconds(60);
    encontro.marcarChamadaSalva(segunda);
    assertEquals(chamada, encontro.getChamadaSalvaEm());
    assertEquals(segunda, encontro.getAtualizadoEm());
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
