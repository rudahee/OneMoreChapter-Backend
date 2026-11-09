package org.rudahee.backend.space.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rudahee.backend.common.exception.BusinessRuleException;
import org.rudahee.backend.common.exception.NotFoundException;
import org.rudahee.backend.security.domain.AppUser;
import org.rudahee.backend.security.service.AuthService;
import org.rudahee.backend.security.service.TokenService;
import org.rudahee.backend.space.domain.Invitation;
import org.rudahee.backend.space.domain.Space;
import org.rudahee.backend.space.domain.SpaceMember;
import org.rudahee.backend.space.domain.SpaceRole;
import org.rudahee.backend.space.repository.InvitationRepository;
import org.rudahee.backend.space.repository.SpaceMemberRepository;
import org.rudahee.backend.space.repository.SpaceRepository;
import org.rudahee.backend.space.service.InvitationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvitationServiceImpl implements InvitationService {

    /**
     * Alfabeto sin caracteres ambiguos: nada de 0/O ni 1/I/l. El codigo se
     * comparte por mensaje y a veces se teclea a mano.
     */
    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";

    /** 16 caracteres sobre 31 simbolos rondan los 79 bits de entropia. */
    private static final int CODE_LENGTH = 16;

    private final InvitationRepository invitationRepository;
    private final SpaceRepository spaceRepository;
    private final SpaceMemberRepository memberRepository;
    private final AuthService authService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public CreatedInvitation create(Long spaceId, SpaceRole grantedRole,
                                    Duration ttl, Long createdByUserId) {
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> NotFoundException.of("Espacio", spaceId));

        if (space.getMaxMembers() != null
                && memberRepository.countBySpaceId(spaceId) >= space.getMaxMembers()) {
            throw new BusinessRuleException("SPACE_FULL",
                    "El espacio ya tiene el maximo de miembros");
        }

        AppUser creator = authService.requireUser(createdByUserId);
        String plainCode = generateCode();

        Invitation invitation = invitationRepository.save(new Invitation(
                space, hash(plainCode), grantedRole, creator, Instant.now().plus(ttl)));

        log.info("Invitacion {} creada para el espacio {} por el usuario {}",
                invitation.getId(), spaceId, createdByUserId);

        return new CreatedInvitation(invitation, plainCode);
    }

    /**
     * Canje: crea la cuenta y la pertenencia en una sola transaccion.
     *
     * Si la creacion del usuario falla (correo ya registrado), la invitacion no
     * se marca como usada: revienta la transaccion entera y el codigo sigue
     * sirviendo. Es lo correcto, porque quien lo intento no llego a entrar.
     */
    @Override
    @Transactional
    public RedeemedInvitation accept(String plainCode, String email,
                                     String password, String displayName) {
        Invitation invitation = invitationRepository.findByCodeHash(hash(plainCode))
                .orElseThrow(() -> new BusinessRuleException(
                        "INVALID_INVITATION", "La invitacion no es valida"));

        // Un solo mensaje para caducada, revocada y ya usada: distinguirlos
        // permitiria sondear que codigos existieron.
        if (!invitation.isUsable()) {
            throw new BusinessRuleException(
                    "INVALID_INVITATION", "La invitacion no es valida");
        }

        Space space = invitation.getSpace();
        if (space.getMaxMembers() != null
                && memberRepository.countBySpaceId(space.getId()) >= space.getMaxMembers()) {
            throw new BusinessRuleException("SPACE_FULL",
                    "El espacio ya tiene el maximo de miembros");
        }

        AppUser user = authService.createUser(email, password, displayName);
        memberRepository.save(new SpaceMember(space, user, invitation.getGrantedRole()));
        invitation.redeem(user);

        log.info("Invitacion {} canjeada: usuario {} entra en el espacio {}",
                invitation.getId(), user.getId(), space.getId());

        // Se devuelve sesion iniciada: obligar a un login inmediato despues de
        // acabar de escribir la contrasenya es friccion sin ganancia.
        TokenService.TokenPair tokens = authService.login(email, password);
        return new RedeemedInvitation(space.getId(), user.getId(), tokens);
    }

    @Override
    public List<Invitation> pending(Long spaceId) {
        return invitationRepository.findPendingBySpace(spaceId, Instant.now());
    }

    @Override
    @Transactional
    public void revoke(Long invitationId) {
        invitationRepository.findById(invitationId)
                .orElseThrow(() -> NotFoundException.of("Invitacion", invitationId))
                .revoke();
    }

    private String generateCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(ALPHABET.charAt(secureRandom.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }

    private String hash(String plainCode) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(plainCode.trim().toUpperCase()
                            .getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible en esta JVM", ex);
        }
    }
}
