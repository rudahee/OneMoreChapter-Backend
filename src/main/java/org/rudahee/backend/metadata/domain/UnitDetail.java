package org.rudahee.backend.metadata.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Episodio, capitulo o pista.
 *
 * @param seasonNumber {@code null} en obras sin temporadas (manga, podcast, album)
 */
public record UnitDetail(
        Integer seasonNumber,
        int unitNumber,
        String title,
        LocalDate airDate,
        Integer runtimeMinutes) {

    /**
     * Genera unidades numeradas cuando el proveedor solo conoce el total.
     * AniList da {@code chapters: 364} pero no la lista; para marcar progreso
     * eso basta.
     */
    public static List<UnitDetail> synthesize(int total) {
        return IntStream.rangeClosed(1, total)
                .mapToObj(i -> new UnitDetail(null, i, null, null, null))
                .toList();
    }

    public boolean isSynthetic() {
        return title == null && airDate == null;
    }
}
