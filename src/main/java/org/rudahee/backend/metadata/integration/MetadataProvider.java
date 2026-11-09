package org.rudahee.backend.metadata.integration;

import org.rudahee.backend.metadata.domain.UnitDetail;
import org.rudahee.backend.metadata.domain.WorkCandidate;
import org.rudahee.backend.metadata.domain.WorkDetail;
import org.rudahee.backend.metadata.domain.WorkType;

import java.util.List;
import java.util.Optional;

/**
 * Contrato de una fuente externa de metadatos.
 *
 * Las implementaciones se registran como beans y el servicio las recibe por
 * inyeccion de lista, asi que anyadir un proveedor no requiere tocar nada mas.
 * Deben anotarse con {@code @ConditionalOnProperty} sobre su clave para poder
 * desactivarlas por configuracion.
 */
public interface MetadataProvider {

    /** Clave estable, la misma que en {@code metadata.providers.*}. */
    String getKey();

    /** {@code true} si merece la pena consultarlo para este tipo de obra. */
    boolean supports(WorkType type);

    List<WorkCandidate> search(String query, WorkType type);

    Optional<WorkDetail> findByExternalId(String externalId, WorkType type);

    /**
     * Lista real de unidades. Por defecto vacia: el servicio sintetiza unidades
     * numeradas a partir del total cuando el proveedor no la ofrece.
     */
    default List<UnitDetail> findUnits(String externalId, WorkType type) {
        return List.of();
    }
}
