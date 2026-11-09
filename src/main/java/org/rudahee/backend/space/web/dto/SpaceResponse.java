package org.rudahee.backend.space.web.dto;

import org.rudahee.backend.space.domain.Space;

public record SpaceResponse(
        Long id,
        String name,
        Integer maxMembers) {

    public static SpaceResponse from(Space space) {
        return new SpaceResponse(space.getId(), space.getName(), space.getMaxMembers());
    }
}