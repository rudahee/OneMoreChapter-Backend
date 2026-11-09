package org.rudahee.backend.security.context;

import org.rudahee.backend.common.exception.ForbiddenException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Acceso al usuario autenticado.
 *
 * Existe para que {@code space} y {@code entry} no tengan que hurgar en el
 * {@link SecurityContextHolder} ni conocer la forma del JWT. El dia que cambie
 * el formato del token, solo se toca esta clase.
 */
@Component
public class CurrentUser {

    public Optional<Long> id() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            return parseSubject(jwt.getSubject());
        }
        return Optional.empty();
    }

    /**
     * @throws ForbiddenException si no hay usuario autenticado. Se lanza en vez
     *         de devolver null para que un fallo de configuracion en la cadena
     *         de filtros se manifieste como un 403 claro y no como un NPE.
     */
    public Long requireId() {
        return id().orElseThrow(() ->
                new ForbiddenException("No hay usuario autenticado en la peticion"));
    }

    private Optional<Long> parseSubject(String subject) {
        try {
            return Optional.of(Long.valueOf(subject));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }
}
