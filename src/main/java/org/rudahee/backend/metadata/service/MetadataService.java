package org.rudahee.backend.metadata.service;

import org.rudahee.backend.metadata.domain.WorkCandidate;
import org.rudahee.backend.metadata.domain.WorkDetail;
import org.rudahee.backend.metadata.domain.WorkType;

import java.util.List;
import java.util.Optional;

/** Fachada del modulo de metadatos. */
public interface MetadataService {

    /** Busca en todos los proveedores aplicables y devuelve candidatos fusionados. */
    List<WorkCandidate> search(String query, WorkType type);

    /** Resuelve una obra concreta con sus unidades, lista para persistir. */
    Optional<WorkDetail> resolve(String source, String externalId, WorkType type);
}
