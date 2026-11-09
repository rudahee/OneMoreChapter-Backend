package org.rudahee.backend.entry.service;

import org.rudahee.backend.entry.domain.Entry;
import org.rudahee.backend.metadata.domain.WorkType;

import java.util.List;

/**
 * La ruleta. Resuelve el problema real de tener una lista de doscientas cosas:
 * no es llevarla, es decidir.
 */
public interface PickerService {

    /**
     * @param type      limitar a un tipo de obra, o null
     * @param episodic  true solo series, false solo cosas de una sentada, null ambas
     * @param onlyStarted solo lo ya empezado, para terminar antes de empezar mas
     * @param count     cuantas propuestas devolver
     */
    record PickCriteria(WorkType type, Boolean episodic, boolean onlyStarted, int count) { }

    List<Entry> pick(Long spaceId, PickCriteria criteria);
}
