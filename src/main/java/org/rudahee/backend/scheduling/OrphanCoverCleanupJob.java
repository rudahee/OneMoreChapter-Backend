package org.rudahee.backend.scheduling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Portadas en disco que ya no referencia ninguna obra.
 *
 * Se acumulan cuando un proveedor cambia la imagen de una obra y la copia
 * anterior se queda sin dueño. No es urgente: son kilobytes y el borrado en
 * caliente ya cubre el caso normal, asi que va semanal.
 *
 * Sin implementar todavia. Requiere cruzar el listado del directorio con las
 * rutas en media_work, y hacerlo bien exige cuidado: si el cruce se hace mal se
 * borran portadas en uso. Mientras tanto, el disco crece despacio y de forma
 * acotada, que es preferible a una limpieza agresiva mal hecha.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "scheduling.orphan-covers", name = "enabled",
        havingValue = "true")
public class OrphanCoverCleanupJob {

    @Scheduled(cron = "0 0 5 * * SUN", zone = "Europe/Madrid")
    public void purgeOrphanCovers() {
        log.debug("Limpieza de portadas huerfanas pendiente de implementar");
    }
}
