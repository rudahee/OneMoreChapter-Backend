package org.rudahee.backend.space.security;

import lombok.RequiredArgsConstructor;
import org.rudahee.backend.common.exception.ForbiddenException;
import org.rudahee.backend.security.context.CurrentUser;
import org.rudahee.backend.space.domain.SpaceRole;
import org.rudahee.backend.space.repository.SpaceMemberRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Autorizacion a nivel de espacio.
 *
 * Se registra como bean con nombre corto para poder usarlo en las expresiones
 * de seguridad de los controladores:
 *
 * <pre>{@code @PreAuthorize("@spaceAccess.isMember(#spaceId)")}</pre>
 *
 * Es el punto donde se concentra toda la comprobacion de pertenencia. Si esta
 * clase es correcta, no hay fuga entre espacios; si se comprueba a mano en cada
 * servicio, tarde o temprano falta en algun sitio.
 *
 * La consulta va a base de datos en cada llamada, pero devuelve una sola
 * columna sobre un indice unico: es mas barato que mantener una cache que
 * pueda quedarse obsoleta cuando alguien acepta una invitacion.
 */
@Component("spaceAccess")
@RequiredArgsConstructor
public class SpaceAccess {

    private final SpaceMemberRepository memberRepository;
    private final CurrentUser currentUser;

    @Transactional(readOnly = true)
    public boolean isMember(Long spaceId) {
        return roleOf(spaceId).isPresent();
    }

    @Transactional(readOnly = true)
    public boolean canManage(Long spaceId) {
        return roleOf(spaceId).filter(SpaceRole::canManage).isPresent();
    }

    /**
     * Version imperativa para los servicios, que no pasan por
     * {@code @PreAuthorize}. Lanza en vez de devolver un booleano para que
     * olvidarse de comprobar el resultado no sea un fallo silencioso.
     */
    @Transactional(readOnly = true)
    public SpaceRole requireMember(Long spaceId) {
        return roleOf(spaceId).orElseThrow(() ->
                new ForbiddenException("No perteneces a este espacio"));
    }

    @Transactional(readOnly = true)
    public SpaceRole requireManager(Long spaceId) {
        SpaceRole role = requireMember(spaceId);
        if (!role.canManage()) {
            throw new ForbiddenException("Solo el propietario puede hacer esto");
        }
        return role;
    }

    private Optional<SpaceRole> roleOf(Long spaceId) {
        return currentUser.id()
                .flatMap(userId -> memberRepository.findRole(spaceId, userId));
    }
}
