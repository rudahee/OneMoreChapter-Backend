package org.rudahee.backend.common.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Traduce excepciones a respuestas RFC 7807.
 *
 * Extiende {@link ResponseEntityExceptionHandler} para no reimplementar el
 * manejo de las excepciones estandar de Spring MVC (metodo no permitido, tipo
 * de contenido no soportado, parametro ausente); con
 * {@code spring.mvc.problemdetails.enabled: true} ya devuelven ProblemDetail.
 * Aqui solo se sobreescribe lo que aporta algo.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String TRACE_ID = "traceId";

    @ExceptionHandler(ApiException.class)
    public ProblemDetail handleApiException(ApiException ex) {
        // Errores esperados: nivel debug. Llenar el log de warnings por un 404
        // legitimo hace que los warnings dejen de significar nada.
        log.debug("Error de negocio: {}", ex.getMessage());
        return ex.toProblemDetail();
    }

    /**
     * Los dos habeis tocado la misma ficha a la vez. No se reintenta en el
     * servidor porque el cliente tiene que ver el estado nuevo antes de decidir.
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLocking(OptimisticLockingFailureException ex) {
        log.debug("Bloqueo optimista", ex);
        return ConflictException.staleVersion("El recurso").toProblemDetail();
    }

    /**
     * Restriccion de la base de datos. El mensaje original menciona nombres de
     * tabla e indice, asi que nunca se propaga al cliente.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        String traceId = newTraceId();
        log.warn("Violacion de integridad [{}]", traceId, ex);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "La operacion entra en conflicto con datos existentes.");
        problem.setTitle("Conflicto de datos");
        problem.setProperty(TRACE_ID, traceId);
        return problem;
    }

    /** Violaciones de @Validated sobre parametros de metodo (no cuerpo). */
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(violation ->
                errors.put(violation.getPropertyPath().toString(), violation.getMessage()));

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Parametros no validos.");
        problem.setTitle("Validacion fallida");
        problem.setProperty("errors", errors);
        return problem;
    }

    /**
     * Se maneja aqui porque el metodo de seguridad se evalua dentro de la
     * invocacion del controlador, ya pasado el ExceptionTranslationFilter.
     * No se detalla que fallo: seria un oraculo de permisos.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        log.debug("Acceso denegado", ex);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,
                "No tienes permiso para realizar esta operacion.");
        problem.setTitle("Acceso denegado");
        return problem;
    }

    /**
     * Red de seguridad. Devuelve un identificador correlacionable con el log en
     * lugar del mensaje real, que puede contener rutas, SQL o nombres de clase.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        String traceId = newTraceId();
        log.error("Error no controlado [{}]", traceId, ex);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Error interno. Si persiste, indica el identificador de traza.");
        problem.setTitle("Error interno");
        problem.setProperty(TRACE_ID, traceId);
        return problem;
    }

    /** Errores de validacion del cuerpo, campo a campo. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.merge(error.getField(),
                        error.getDefaultMessage() == null ? "valor no valido" : error.getDefaultMessage(),
                        (first, second) -> first + "; " + second));
        ex.getBindingResult().getGlobalErrors().forEach(error ->
                errors.put(error.getObjectName(), error.getDefaultMessage()));

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Revisa los campos marcados.");
        problem.setTitle("Validacion fallida");
        problem.setProperty("errors", errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    private String newTraceId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
