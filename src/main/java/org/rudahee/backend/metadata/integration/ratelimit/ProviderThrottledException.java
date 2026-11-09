package org.rudahee.backend.metadata.integration.ratelimit;

import lombok.Getter;

import java.io.Serial;
import java.time.Duration;

/** El proveedor esta limitado o enfriando; no se llego a emitir la peticion. */
@Getter
public class ProviderThrottledException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String provider;
    private final transient Duration retryAfter;

    public ProviderThrottledException(String provider, Duration retryAfter) {
        super("Proveedor %s limitado, reintentar en %d s"
                .formatted(provider, retryAfter.toSeconds()));
        this.provider = provider;
        this.retryAfter = retryAfter;
    }
}
