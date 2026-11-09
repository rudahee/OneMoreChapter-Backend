package org.rudahee.backend.metadata.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.Map;

/**
 * Configuracion del modulo. Binding por constructor (record inmutable): no hay
 * setters que permitan mutar la configuracion en caliente, que era el problema
 * de la version anterior.
 *
 * @param userAgent identificacion obligatoria. Wikidata devuelve resultados
 *                  vacios sin el y puede bloquear la IP; MusicBrainz y MangaDex
 *                  lo exigen igualmente. Debe incluir un contacto real.
 */
@Validated
@ConfigurationProperties(prefix = "metadata")
public record MetadataProperties(
        @NotBlank String userAgent,
        @DefaultValue("3s") Duration connectTimeout,
        @DefaultValue("10s") Duration readTimeout,
        @DefaultValue("5s") Duration maxWaitForPermit,
        @DefaultValue("6s") Duration searchTimeout,
        @DefaultValue("2") @PositiveOrZero int maxRetries,
        @Valid Map<String, ProviderProperties> providers) {

    public MetadataProperties {
        providers = providers == null ? Map.of() : Map.copyOf(providers);
    }

    /**
     * @throws IllegalStateException si el proveedor no esta configurado. Fallar
     *         al arrancar es preferible a descubrir en produccion que un
     *         proveedor corre sin limites por una clave mal escrita en el YAML.
     */
    public ProviderProperties requireProvider(String key) {
        ProviderProperties provider = providers.get(key);
        if (provider == null) {
            throw new IllegalStateException(
                    "Falta configuracion para metadata.providers." + key);
        }
        return provider;
    }
}
