package org.rudahee.backend.metadata.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Limites de un proveedor externo. Todos estos servicios limitan por IP, no por
 * clave, asi que los valores son deliberadamente conservadores.
 *
 * @param capacity        permisos disponibles por {@code period}
 * @param maxConcurrent   peticiones simultaneas; algunos servicios limitan el
 *                        paralelismo ademas del ritmo (Wikidata: 5 por IP)
 * @param defaultCooldown penalizacion cuando llega un 429 sin {@code Retry-After}
 * @param priority        desempate en el merge de candidatos; mayor gana
 */
public record ProviderProperties(
        @DefaultValue("true") boolean enabled,
        @NotBlank String baseUrl,
        @DefaultValue("10") @Positive int capacity,
        @DefaultValue("1s") Duration period,
        @DefaultValue("3") @Positive int maxConcurrent,
        @DefaultValue("30s") Duration defaultCooldown,
        @DefaultValue("50") int priority) {
}
