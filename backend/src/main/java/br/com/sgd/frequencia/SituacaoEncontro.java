package br.com.sgd.frequencia;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SituacaoEncontro {
    REALIZADO,
    NAO_REALIZADO;

    @JsonValue
    public String json() {
        return name();
    }

    @JsonCreator
    public static SituacaoEncontro fromJson(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("A situação do encontro é obrigatória.");
        }
        return switch (valor.trim()) {
            case "REALIZADO" -> REALIZADO;
            case "NAO_REALIZADO", "CANCELADO" -> NAO_REALIZADO;
            default -> throw new IllegalArgumentException(
                    "Situação inválida: '" + valor + "'. Use REALIZADO ou NAO_REALIZADO.");
        };
    }
}
