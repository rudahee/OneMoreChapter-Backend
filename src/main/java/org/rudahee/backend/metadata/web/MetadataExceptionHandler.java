package org.rudahee.backend.metadata.web;

import org.rudahee.backend.metadata.integration.ratelimit.ProviderThrottledException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce los fallos del modulo a respuestas RFC 7807. Se limita a este paquete
 * para no interferir con el manejador global de la aplicacion.
 */
@Slf4j
@RestControllerAdvice(assignableTypes = MetadataController.class)
public class MetadataExceptionHandler {

    @ExceptionHandler(ProviderThrottledException.class)
    public ProblemDetail handleThrottled(ProviderThrottledException ex) {
        log.debug("Peticion rechazada por limite de proveedor", ex);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "El proveedor de metadatos esta temporalmente saturado. Reintenta en unos segundos.");
        problem.setTitle("Proveedor no disponible");
        problem.setProperty("provider", ex.getProvider());
        problem.setProperty("retryAfterSeconds", ex.getRetryAfter().toSeconds());
        return problem;
    }
}
