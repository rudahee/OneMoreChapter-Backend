package org.rudahee.backend.common.web;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Envoltura estable de paginacion.
 *
 * Serializar {@code Page} o {@code PageImpl} directamente no es estable entre
 * versiones de Spring Data (de hecho Boot avisa por consola al hacerlo), y
 * arrastra al JSON campos internos del {@code Pageable} que el cliente no
 * necesita. Esta proyeccion fija el contrato.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isFirst(), page.isLast());
    }

    /** Convierte las entidades a DTO sin materializar una lista intermedia. */
    public static <S, T> PageResponse<T> from(Page<S> page, Function<S, T> mapper) {
        return from(page.map(mapper));
    }

    public static <T> PageResponse<T> empty() {
        return new PageResponse<>(List.of(), 0, 0, 0L, 0, true, true);
    }
}
