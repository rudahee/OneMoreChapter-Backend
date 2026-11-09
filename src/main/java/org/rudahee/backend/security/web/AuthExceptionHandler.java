package org.rudahee.backend.security.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce fallos de credenciales a 401.
 *
 * Se limita a este controlador para no capturar excepciones de autenticacion
 * que se produzcan en otras partes de la cadena de filtros.
 */
@Slf4j
@RestControllerAdvice(assignableTypes = AuthController.class)
public class AuthExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex) {
        // El motivo real se registra, pero nunca se devuelve: distinguir
        // "correo desconocido" de "contrasenya incorrecta" permite enumerar
        // cuentas registradas.
        log.debug("Fallo de autenticacion: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED, "Credenciales no validas.");
        problem.setTitle("Autenticacion fallida");
        return problem;
    }
}
