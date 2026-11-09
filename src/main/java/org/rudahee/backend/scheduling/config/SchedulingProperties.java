package org.rudahee.backend.scheduling.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * @param refreshBatchSize obras por ejecucion. El limitador de {@code metadata}
 *                         ya frena las llamadas, pero un lote acotado evita que
 *                         una tanda se alargue horas si el catalogo crece.
 * @param refreshInterval  antiguedad minima para volver a mirar una obra
 * @param tokenRetention   cuanto se conservan los tokens caducados antes de
 *                         borrarlos. No es cero: sirven para investigar un
 *                         incidente de reutilizacion despues de que ocurra.
 */
@Validated
@ConfigurationProperties(prefix = "scheduling")
public record SchedulingProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("50") @Positive int refreshBatchSize,
        @DefaultValue("P1D") Duration refreshInterval,
        @DefaultValue("P7D") Duration tokenRetention,
        @DefaultValue("P30D") Duration invitationRetention) {
}
