package org.rudahee.backend.storage.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

/**
 * @param basePath   directorio en disco. En Docker, un volumen montado: si vive
 *                   dentro del contenedor, se pierde en cada despliegue.
 * @param publicPath prefijo con el que se sirven. Debe estar en permitAll:
 *                   una etiqueta img no puede mandar cabecera Authorization.
 * @param maxBytes   corta descargas absurdas. Wikimedia sirve TIFF de decenas
 *                   de megas para algunas obras.
 */
@Validated
@ConfigurationProperties(prefix = "storage.covers")
public record StorageProperties(
        @DefaultValue("true") boolean enabled,
        @NotBlank String basePath,
        @DefaultValue("/media/covers") String publicPath,
        @DefaultValue("5242880") @Positive long maxBytes,
        @DefaultValue("10s") Duration timeout,
        @DefaultValue({"image/jpeg", "image/png", "image/webp", "image/avif"})
        List<String> allowedContentTypes) {
}
