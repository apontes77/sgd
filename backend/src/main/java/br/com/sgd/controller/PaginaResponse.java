package br.com.sgd.controller;

import java.util.List;
import org.springframework.data.domain.Page;

public record PaginaResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
    public static <T> PaginaResponse<T> of(Page<T> value) {
        return new PaginaResponse<>(value.getContent(), value.getNumber(), value.getSize(), value.getTotalElements(), value.getTotalPages());
    }
}
