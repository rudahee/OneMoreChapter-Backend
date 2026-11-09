package org.rudahee.backend.common.exception;

import org.springframework.http.HttpStatus;

import java.io.Serial;

/**
 * Regla de negocio incumplida con datos sintacticamente validos.
 *
 * Bean Validation cubre el formato; esto cubre lo que solo se sabe consultando
 * el estado: invitacion caducada, marcar un episodio de una obra no episodica,
 * unirse a un espacio que ya esta completo.
 */
public class BusinessRuleException extends ApiException {

    @Serial
    private static final long serialVersionUID = 1L;

    public BusinessRuleException(String detail) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "Regla de negocio incumplida", detail);
    }

    public BusinessRuleException(String rule, String detail) {
        this(detail);
        addProperty("rule", rule);
    }
}
