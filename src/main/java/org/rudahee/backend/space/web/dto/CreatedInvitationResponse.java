package org.rudahee.backend.space.web.dto;

import org.rudahee.backend.space.service.InvitationService;

import java.time.Instant;

/**
 * Unica respuesta que contiene el codigo en claro.
 *
 * Solo se devuelve al crearla: la base de datos guarda el hash, asi que no hay
 * forma de recuperarlo despues. Si se pierde, se revoca y se crea otra.
 */
public record CreatedInvitationResponse(
        Long id,
        String code,
        Instant expiresAt) {

    public static CreatedInvitationResponse from(InvitationService.CreatedInvitation created) {
        return new CreatedInvitationResponse(
                created.invitation().getId(),
                created.plainCode(),
                created.invitation().getExpiresAt());
    }
}