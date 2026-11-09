package org.rudahee.backend.common.exception;

import org.springframework.http.HttpStatus;

import java.io.Serial;

/**
 * Choque con el estado actual del recurso.
 *
 * En esta aplicacion el caso habitual es el bloqueo optimista: los dos editais
 * la misma ficha a la vez y una de las dos escrituras pierde. El cliente debe
 * recargar y reintentar, no reenviar a ciegas.
 */
public class ConflictException extends ApiException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ConflictException(String detail) {
        super(HttpStatus.CONFLICT, "Conflicto de estado", detail);
    }

    public ConflictException(String detail, Throwable cause) {
        super(HttpStatus.CONFLICT, "Conflicto de estado", detail, cause);
    }

    public static ConflictException staleVersion(String resource) {
        ConflictException exception = new ConflictException(
                "%s fue modificado por otra persona. Recarga y vuelve a intentarlo."
                        .formatted(resource));
        exception.addProperty("resource", resource);
        exception.addProperty("reason", "STALE_VERSION");
        return exception;
    }
}
