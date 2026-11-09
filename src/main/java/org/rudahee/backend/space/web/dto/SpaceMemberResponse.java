package org.rudahee.backend.space.web.dto;

import org.rudahee.backend.space.domain.SpaceMember;
import org.rudahee.backend.space.domain.SpaceRole;

import java.time.Instant;

/**
 * No expone el correo de los demas miembros: dentro de un espacio basta con el
 * nombre visible, y el correo es dato personal de otra persona.
 */
public record SpaceMemberResponse(
        Long userId,
        String displayName,
        SpaceRole role,
        Instant joinedAt) {

    public static SpaceMemberResponse from(SpaceMember member) {
        return new SpaceMemberResponse(member.getUser().getId(),
                member.getUser().getDisplayName(), member.getRole(), member.getJoinedAt());
    }
}