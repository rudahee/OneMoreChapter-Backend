package org.rudahee.backend.space.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.rudahee.backend.space.domain.SpaceRole;

/**
 * @param grantedRole rol que tendra quien la canjee; null equivale a MEMBER
 * @param ttlHours    validez. Corta por defecto: una invitacion que caduca
 *                    limita la ventana en la que un codigo filtrado sirve
 */
public record CreateInvitationRequest(
        SpaceRole grantedRole,
        @Min(1) @Max(720) Integer ttlHours) {

    public SpaceRole grantedRoleOrDefault() {
        return grantedRole == null ? SpaceRole.MEMBER : grantedRole;
    }

    public int ttlHoursOrDefault() {
        return ttlHours == null ? 72 : ttlHours;
    }
}