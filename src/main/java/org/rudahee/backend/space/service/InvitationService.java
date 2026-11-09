package org.rudahee.backend.space.service;

import org.rudahee.backend.security.service.TokenService;
import org.rudahee.backend.space.domain.Invitation;
import org.rudahee.backend.space.domain.SpaceRole;

import java.time.Duration;
import java.util.List;

/**
 * Alta por invitacion. Es el puente entre {@code space} y {@code security}, y
 * el motivo de que la dependencia vaya en ese sentido y no al reves.
 */
public interface InvitationService {

    /**
     * @param plainCode codigo en claro. Solo existe en este objeto: en la base
     *                  de datos se guarda su hash, asi que no hay forma de
     *                  recuperarlo despues.
     */
    record CreatedInvitation(Invitation invitation, String plainCode) { }

    /** Resultado del canje: la cuenta queda creada y con sesion iniciada. */
    record RedeemedInvitation(Long spaceId, Long userId, TokenService.TokenPair tokens) { }

    CreatedInvitation create(Long spaceId, SpaceRole grantedRole,
                             Duration ttl, Long createdByUserId);

    RedeemedInvitation accept(String plainCode, String email,
                              String password, String displayName);

    List<Invitation> pending(Long spaceId);

    void revoke(Long invitationId);
}
