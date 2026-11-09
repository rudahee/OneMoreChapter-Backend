package org.rudahee.backend.entry.domain;

/**
 * Estado de una obra dentro de la lista.
 *
 * Las transiciones entre BACKLOG, IN_PROGRESS y DONE las hace sola
 * {@code ProgressService} al marcar unidades. PAUSED y DROPPED son siempre
 * decision explicita: nadie quiere que la aplicacion decida por su cuenta que
 * habeis abandonado una serie.
 */
public enum EntryStatus {

    BACKLOG,
    IN_PROGRESS,
    PAUSED,
    DONE,
    DROPPED;

    public boolean isActive() {
        return this == BACKLOG || this == IN_PROGRESS || this == PAUSED;
    }
}
