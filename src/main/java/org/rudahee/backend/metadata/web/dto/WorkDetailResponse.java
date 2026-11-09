package org.rudahee.backend.metadata.web.dto;

import org.rudahee.backend.metadata.domain.WorkDetail;
import org.rudahee.backend.metadata.domain.WorkStatus;
import org.rudahee.backend.metadata.domain.WorkType;

import java.util.List;
import java.util.Map;

public record WorkDetailResponse(
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
        List<UnitResponse> units) {

    public static WorkDetailResponse from(WorkDetail detail) {
        return new WorkDetailResponse(detail.source(), detail.externalId(), detail.type(),
                detail.title(), detail.originalTitle(), detail.year(), detail.coverUrl(),
                detail.episodic(), detail.totalUnits(), detail.status(), detail.externalIds(),
                detail.units().stream().map(UnitResponse::from).toList());
    }
}
