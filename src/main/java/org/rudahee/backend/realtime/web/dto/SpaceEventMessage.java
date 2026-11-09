package org.rudahee.backend.realtime.web.dto;

import org.rudahee.backend.common.event.SpaceChangeType;
import org.rudahee.backend.common.event.SpaceChangedEvent;

import java.time.Instant;

/**
 * Mensaje que viaja por el socket. Deliberadamente diminuto: es un aviso, no
 * un volcado de estado.
 */
public record SpaceEventMessage(
        SpaceChangeType type,
        Long entryId,
        Long actorUserId,
        Long version,
        Instant occurredAt) {

    public static SpaceEventMessage from(SpaceChangedEvent event) {
        return new SpaceEventMessage(event.type(), event.entryId(),
                event.actorUserId(), event.version(), Instant.now());
    }
}
