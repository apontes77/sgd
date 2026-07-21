package br.com.sgd.frequencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SituacaoEncontroTest {
    @Test
    void aceitaValoresOficiaisEAliasCancelado() {
        assertThat(SituacaoEncontro.fromJson("REALIZADO")).isEqualTo(SituacaoEncontro.REALIZADO);
        assertThat(SituacaoEncontro.fromJson("NAO_REALIZADO")).isEqualTo(SituacaoEncontro.NAO_REALIZADO);
        assertThat(SituacaoEncontro.fromJson("CANCELADO")).isEqualTo(SituacaoEncontro.NAO_REALIZADO);
    }

    @Test
    void rejeitaValorDesconhecido() {
        assertThatThrownBy(() -> SituacaoEncontro.fromJson("FOO"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Situação inválida");
    }
}
