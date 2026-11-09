package org.rudahee.backend.metadata.integration.client;

import org.rudahee.backend.metadata.integration.ratelimit.ProviderGate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.NonNull;

import java.io.IOException;
import java.time.Duration;

/**
 * Aplica los limites del proveedor de forma transparente a nivel de transporte.
 *
 * Ponerlo como interceptor y no como envoltorio en cada llamada tiene una
 * consecuencia practica: un proveedor nuevo obtiene rate limiting por el simple
 * hecho de usar su {@code RestClient}, sin poder saltarselo por descuido.
 *
 * Tambien absorbe los 429 aqui, penalizando al proveedor entero antes de
 * reintentar y respetando {@code Retry-After} cuando viene.
 */
@Slf4j
@RequiredArgsConstructor
public class RateLimitingInterceptor implements ClientHttpRequestInterceptor {

    private final ProviderGate gate;
    private final Duration maxWaitForPermit;
    private final int maxRetries;

    @Override
    @NonNull
    public ClientHttpResponse intercept(@NonNull HttpRequest request,
                                        @NonNull byte[] body,
                                        @NonNull ClientHttpRequestExecution execution)
            throws IOException {

        for (int attempt = 0; ; attempt++) {
            ClientHttpResponse response = executeWithPermit(request, body, execution);
            if (!isThrottled(response) || attempt >= maxRetries) {
                return response;
            }
            Duration retryAfter = parseRetryAfter(response.getHeaders());
            response.close();
            gate.penalize(retryAfter);
        }
    }

    private ClientHttpResponse executeWithPermit(HttpRequest request,
                                                 byte[] body,
                                                 ClientHttpRequestExecution execution)
            throws IOException {
        try (ProviderGate.Permit ignored = gate.acquire(maxWaitForPermit)) {
            return execution.execute(request, body);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrumpido esperando permiso para "
                    + gate.getName(), ex);
        }
    }

    private boolean isThrottled(ClientHttpResponse response) throws IOException {
        int status = response.getStatusCode().value();
        return status == HttpStatus.TOO_MANY_REQUESTS.value()
                || status == HttpStatus.SERVICE_UNAVAILABLE.value();
    }

    /** {@code Retry-After} admite segundos o fecha HTTP; solo interpretamos segundos. */
    private Duration parseRetryAfter(HttpHeaders headers) {
        String raw = headers.getFirst(HttpHeaders.RETRY_AFTER);
        if (raw == null) {
            return null;
        }
        try {
            return Duration.ofSeconds(Long.parseLong(raw.trim()));
        } catch (NumberFormatException ex) {
            log.debug("Retry-After no numerico: {}", raw);
            return null;
        }
    }
}
