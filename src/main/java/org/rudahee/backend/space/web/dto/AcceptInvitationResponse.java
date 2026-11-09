package org.rudahee.backend.space.web.dto;

import org.rudahee.backend.security.web.dto.TokenResponse;
import org.rudahee.backend.space.service.InvitationService;

/** La cuenta queda creada y con sesion iniciada: no hace falta pasar por login. */
public record AcceptInvitationResponse(
        Long spaceId,
        Long userId,
        TokenResponse tokens) {

    public static AcceptInvitationResponse from(InvitationService.RedeemedInvitation redeemed) {
        return new AcceptInvitationResponse(redeemed.spaceId(), redeemed.userId(),
                TokenResponse.from(redeemed.tokens()));
    }
}