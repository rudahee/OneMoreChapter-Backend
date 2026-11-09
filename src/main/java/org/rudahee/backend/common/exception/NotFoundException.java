package org.rudahee.backend.common.exception;

import org.springframework.http.HttpStatus;

import java.io.Serial;

/** El recurso solicitado no existe o el usuario no tiene por que saber que existe. */
public class NotFoundException extends ApiException {

    @Serial
    private static final long serialVersionUID = 1L;

    public NotFoundException(String detail) {
        super(HttpStatus.NOT_FOUND, "Recurso no encontrado", detail);
    }

    /**
     * @param resource nombre legible del recurso, no el de la clase Java: el
     *                 mensaje llega al cliente y no debe filtrar la implementacion
     */
    public static NotFoundException of(String resource, Object id) {
        NotFoundException exception = new NotFoundException(
                "%s con identificador %s no encontrado".formatted(resource, id));
        exception.addProperty("resource", resource);
        exception.addProperty("id", String.valueOf(id));
        return exception;
    }
}
