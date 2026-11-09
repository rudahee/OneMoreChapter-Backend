package org.rudahee.backend.entry.service;

import org.rudahee.backend.entry.domain.Entry;

import java.util.Collection;

/**
 * Progreso compartido.
 *
 * No hay parametro de usuario en las consultas: marcar es marcar para los dos.
 * {@code userId} solo se usa para registrar quien pulso el boton.
 */
public interface ProgressService {

    /** @param seenCount unidades marcadas; totalUnits puede ser null si no se conoce */
    record ProgressSummary(long seenCount, Integer totalUnits, Long currentUnitId,
                           Integer positionSeconds, boolean complete) { }

    Entry markSeen(Long entryId, Collection<Long> unitIds, Long userId);

    /**
     * Marca todo hasta esa unidad inclusive. Es el caso real: os haceis cinco
     * episodios del tiron y luego os acordais de marcar.
     */
    Entry markUpTo(Long entryId, Long unitId, Long userId);

    Entry unmarkSeen(Long entryId, Collection<Long> unitIds);

    /** Marca la siguiente unidad sin ver. El boton principal de la aplicacion. */
    Entry advance(Long entryId, Long userId);

    Entry savePosition(Long entryId, Integer positionSeconds);

    ProgressSummary summaryOf(Long entryId);
}
