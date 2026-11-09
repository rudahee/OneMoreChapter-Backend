package org.rudahee.backend.metadata.domain;

/** Estado de publicacion. Solo {@link #ONGOING} dispara el refresco programado. */
public enum WorkStatus {
    ONGOING, FINISHED, UPCOMING, CANCELLED, HIATUS, UNKNOWN
}
