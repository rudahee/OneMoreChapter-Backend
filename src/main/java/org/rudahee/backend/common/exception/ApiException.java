package org.rudahee.backend.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.io.Serial;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Base de los errores de negocio. Cada subclase conoce su codigo HTTP, de forma
 * que el manejador global no necesita una cadena de {@code instanceof} para
 * traducirlas.
 *
 * Solo para errores esperados y explicables al usuario. Un fallo de
 * infraestructura no se envuelve aqui: debe llegar al manejador como un 500 con
 * su traza.
 */
@Getter
public abstract class ApiException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final HttpStatus status;
    private final String title;
    private final transient Map<String, Object> properties = new LinkedHashMap<>();

    protected ApiException(HttpStatus status, String title, String detail) {
        super(detail);
        this.status = status;
        this.title = title;
    }

    protected ApiException(HttpStatus status, String title, String detail, Throwable cause) {
        super(detail, cause);
        this.status = status;
        this.title = title;
    }

    /** Campos extra que acompanyan al problema (recurso afectado, id, etc.). */
    protected void addProperty(String key, Object value) {
        properties.put(key, value);
    }

    public ProblemDetail toProblemDetail() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, getMessage());
        problem.setTitle(title);
        properties.forEach(problem::setProperty);
        return problem;
    }
}
