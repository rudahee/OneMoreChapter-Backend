package org.rudahee.backend.common.exception;

import org.springframework.http.HttpStatus;

import java.io.Serial;

/**
 * El usuario esta autenticado pero no pertenece al espacio o carece del rol.
 *
 * Se usa cuando revelar la existencia del recurso no supone una fuga. Si si la
 * supone, lanzar {@link NotFoundException} en su lugar.
 */
public class ForbiddenException extends ApiException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ForbiddenException(String detail) {
        super(HttpStatus.FORBIDDEN, "Acceso denegado", detail);
    }
}
