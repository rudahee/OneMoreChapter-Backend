package org.rudahee.backend.security.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Parametros de autenticacion.
 *
 * @param jwtSecret        clave HMAC. Minimo 32 caracteres porque HS256 exige
 *                         256 bits; Nimbus falla al arrancar si es mas corta.
 *                         Nunca en el repositorio: variable de entorno.
 * @param accessTokenTtl   corto a proposito. Si se filtra, la ventana de abuso
 *                         es de minutos, y el refresh permite revocar de verdad.
 * @param refreshTokenTtl  cuanto puede estar la app sin abrirse antes de pedir
 *                         contrasenya otra vez.
 * @param authRateLimit    intentos permitidos por IP en {@code authRateWindow}
 */
@Validated
@ConfigurationProperties(prefix = "security")
public record SecurityProperties(
        @NotBlank @Size(min = 32) String jwtSecret,
        @DefaultValue("un-capitulo-mas") String issuer,
        @DefaultValue("15m") Duration accessTokenTtl,
        @DefaultValue("30d") Duration refreshTokenTtl,
        @DefaultValue("10") int authRateLimit,
        @DefaultValue("5m") Duration authRateWindow) {
}
