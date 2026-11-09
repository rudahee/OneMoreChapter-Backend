package org.rudahee.backend.scheduling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rudahee.backend.catalog.domain.MediaWork;
import org.rudahee.backend.catalog.repository.MediaWorkRepository;
import org.rudahee.backend.catalog.service.CatalogService;
import org.rudahee.backend.common.event.UnitsAddedEvent;
import org.rudahee.backend.metadata.domain.WorkStatus;
import org.rudahee.backend.scheduling.config.SchedulingProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Busca capitulos y episodios nuevos en las obras en emision.
 *
 * Solo mira obras {@code ONGOING} y episodicas: una pelicula de 1979 no cambia,
 * y recorrer el catalogo entero cada noche gastaria la cuota de los proveedores
 * en consultas que nunca devuelven nada.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "scheduling", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class OngoingWorkRefreshJob {

    private final MediaWorkRepository workRepository;
    private final CatalogService catalogService;
    private final SchedulingProperties properties;
    private final ApplicationEventPublisher events;

    /**
     * De madrugada y en hora local. Es cuando los proveedores estan mas
     * tranquilos y cuando menos molesta que la aplicacion vaya algo mas lenta.
     */
    @Scheduled(cron = "0 30 4 * * *", zone = "Europe/Madrid")
    public void refreshOngoingWorks() {
        LocalDate threshold = LocalDate.now().minusDays(properties.refreshInterval().toDays());

        List<MediaWork> stale = workRepository.findStaleByStatus(
                WorkStatus.ONGOING, threshold,
                PageRequest.of(0, properties.refreshBatchSize()));

        if (stale.isEmpty()) {
            log.debug("Nada que refrescar");
            return;
        }
        log.info("Refrescando {} obras en emision", stale.size());

        int totalAdded = 0;
        int failures = 0;

        for (MediaWork work : stale) {
            try {
                // syncUnits corre en su propia transaccion: si una obra falla,
                // las ya procesadas conservan lo que trajeron.
                int added = catalogService.syncUnits(work.getId());
                totalAdded += added;

                if (added > 0) {
                    events.publishEvent(
                            new UnitsAddedEvent(work.getId(), work.getTitle(), added));
                }
            } catch (RuntimeException ex) {
                // Un proveedor caido no debe cortar la tanda entera.
                failures++;
                log.warn("Fallo al refrescar la obra {} ({})",
                        work.getId(), work.getTitle(), ex);
            }
        }

        log.info("Refresco terminado: {} unidades nuevas, {} fallos de {} obras",
                totalAdded, failures, stale.size());
    }
}
