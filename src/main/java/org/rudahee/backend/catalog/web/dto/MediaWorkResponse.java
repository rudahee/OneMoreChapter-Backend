package org.rudahee.backend.catalog.web.dto;

import org.rudahee.backend.catalog.domain.ExternalId;
import org.rudahee.backend.catalog.domain.MediaWork;
import org.rudahee.backend.metadata.domain.WorkStatus;
import org.rudahee.backend.metadata.domain.WorkType;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Detalle completo. Requiere que la obra venga cargada con sus unidades. */
public record MediaWorkResponse(
        Long id,
        WorkType type,
        String title,
        String originalTitle,
        Integer year,
        String coverUrl,
        boolean episodic,
        Integer totalUnits,
        WorkStatus status,
        Map<String, String> externalIds,
        List<MediaUnitResponse> units) {

    public static MediaWorkResponse from(MediaWork work) {
        return new MediaWorkResponse(
                work.getId(), work.getType(), work.getTitle(), work.getOriginalTitle(),
                work.getReleaseYear(), work.getCoverUrl(), work.isEpisodic(),
                work.getTotalUnits(), work.getStatus(),
                work.getExternalIds().stream().collect(Collectors.toMap(
                        ExternalId::getSource, ExternalId::getExternalId,
                        (first, second) -> first)),
                work.getUnits().stream().map(MediaUnitResponse::from).toList());
    }

    public static <T> List<T> mapAll(List<MediaWork> works, Function<MediaWork, T> mapper) {
        return works.stream().map(mapper).toList();
    }
}
