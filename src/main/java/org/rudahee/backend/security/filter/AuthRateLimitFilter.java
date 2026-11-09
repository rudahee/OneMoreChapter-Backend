package org.rudahee.backend.security.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.rudahee.backend.security.config.SecurityProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limita los intentos de autenticacion por IP.
 *
 * Los limites de los proveedores externos ya se controlan en {@code metadata},
 * pero eso no protege de que alguien pruebe contrasenyas contra /api/auth.
 * Bucket4j en memoria basta: la aplicacion corre en una sola instancia. Si
 * algun dia se replica, hay que pasar el bucket a Redis o el limite sera de
 * N intentos por instancia.
 */
@Slf4j
public class AuthRateLimitFilter extends OncePerRequestFilter {

    /**
     * Rutas limitadas. Incluye el canje de invitaciones: es publico y cada
     * acierto por fuerza bruta vale una cuenta nueva, asi que necesita el mismo
     * trato que el login.
     */
    private static final List<String> PROTECTED_PREFIXES =
            List.of("/api/auth/", "/api/invitations/");

    private final SecurityProperties properties;
    private final Map<String, Bucket> bucketsByIp = new ConcurrentHashMap<>();

    public AuthRateLimitFilter(SecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        // El refresco es legitimo y frecuente; limitarlo cerraria la sesion a
        // quien tenga la app abierta en dos dispositivos.
        if (path.endsWith("/refresh")) {
            return true;
        }
        return PROTECTED_PREFIXES.stream().noneMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        Bucket bucket = bucketsByIp.computeIfAbsent(clientIp(request), ip -> newBucket());

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
            return;
        }

        log.warn("Limite de intentos de autenticacion superado desde {}", clientIp(request));
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setHeader(HttpHeaders.RETRY_AFTER,
                String.valueOf(properties.authRateWindow().toSeconds()));
        response.getWriter().write("""
                {"type":"about:blank","title":"Demasiados intentos",\
                "status":429,"detail":"Espera unos minutos antes de volver a intentarlo."}""");
    }

    private Bucket newBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(properties.authRateLimit())
                        .refillGreedy(properties.authRateLimit(), properties.authRateWindow())
                        .build())
                .build();
    }

    /**
     * Detras de Traefik la IP real viene en X-Forwarded-For. Requiere que el
     * proxy la sanee: si se acepta la cabecera de cualquiera, el limite se
     * salta con solo mandarla falsificada.
     */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
