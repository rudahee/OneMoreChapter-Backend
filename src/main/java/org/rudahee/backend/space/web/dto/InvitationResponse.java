package org.rudahee.backend.space.web.dto;

import org.rudahee.backend.space.domain.Invitation;
import org.rudahee.backend.space.domain.SpaceRole;

import java.time.Instant;

/** Listado de invitaciones pendientes. Sin el codigo, ni siquiera su hash. */
public record InvitationResponse(
        Long id,
        SpaceRole grantedRole,
        String createdByDisplayName,
        Instant createdAt,
        Instant expiresAt) {

    public static InvitationResponse from(Invitation invitation) {
        return new InvitationResponse(
                invitation.getId(),
                invitation.getGrantedRole(),
                invitation.getCreatedBy().getDisplayName(),
                invitation.getCreatedAt(),
                invitation.getExpiresAt());
    }
}