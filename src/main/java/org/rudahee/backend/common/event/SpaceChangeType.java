package org.rudahee.backend.common.event;

/** Que ha cambiado dentro de un espacio. */
public enum SpaceChangeType {

    ENTRY_ADDED,
    ENTRY_UPDATED,
    ENTRY_REMOVED,
    PROGRESS_CHANGED,
    RATING_CHANGED,
    NOTE_CHANGED,
    TAGS_CHANGED
}
