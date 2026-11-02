package com.poli.restaurante.web.dto;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/** Página de resultados con un formato estable para el cliente. */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <E, T> PageResponse<T> de(Page<E> pagina, Function<E, T> mapper) {
        return new PageResponse<>(pagina.getContent().stream().map(mapper).toList(),
                pagina.getNumber(), pagina.getSize(), pagina.getTotalElements(), pagina.getTotalPages());
    }
}
