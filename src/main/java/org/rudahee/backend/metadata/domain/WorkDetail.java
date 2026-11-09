package org.rudahee.backend.metadata.domain;

import java.util.List;
import java.util.Map;

/**
 * Obra resuelta, lista para persistir en {@code media_work}.
 *
 * @param units lista real de unidades, o vacia si el proveedor solo da el total
 */
public record WorkDetail(
        String source,
        String externalId,
        WorkType type,
        String title,
        String originalTitle,
        Integer year,
        String coverUrl,
        boolean episodic,
        Integer totalUnits,
        WorkStatus status,
        Map<String, String> externalIds,
        List<UnitDetail> units) {

    public WorkDetail {
        externalIds = externalIds == null ? Map.of() : Map.copyOf(externalIds);
        units = units == null ? List.of() : List.copyOf(units);
    }

    public WorkDetail withUnits(List<UnitDetail> newUnits, Integer newTotal) {
        return new WorkDetail(source, externalId, type, title, originalTitle, year,
                coverUrl, episodic, newTotal, status, externalIds, newUnits);
    }
}
