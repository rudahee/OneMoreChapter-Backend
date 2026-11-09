package org.rudahee.backend.catalog.service;

import org.rudahee.backend.catalog.domain.MediaWork;
import org.rudahee.backend.metadata.domain.WorkDetail;
import org.rudahee.backend.metadata.domain.WorkType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Gestion del catalogo canonico de obras.
 *
 * Es la frontera entre los metadatos externos y la persistencia: {@code entry}
 * nunca habla con {@code metadata}, siempre pasa por aqui.
 */
public interface CatalogService {

    /**
     * Devuelve la obra si ya esta en el catalogo o la importa del proveedor.
     * Es el metodo que usa {@code entry} al anyadir algo a una lista.
     */
    MediaWork findOrImport(String source, String externalId, WorkType type);

    /** Inserta o actualiza a partir de un detalle ya resuelto. Idempotente. */
    MediaWork importWork(WorkDetail detail);

    /**
     * Vuelve a pedir las unidades al proveedor y anyade las nuevas.
     *
     * @return numero de unidades anyadidas
     */
    int syncUnits(Long workId);

    Optional<MediaWork> findById(Long id);

    Page<MediaWork> search(String query, WorkType type, Pageable pageable);
}
