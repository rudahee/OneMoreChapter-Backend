package org.rudahee.backend.common.event;

/**
 * El refresco nocturno ha encontrado unidades nuevas en una obra.
 *
 * Todavia no lo escucha nadie. Existe porque es el enganche natural de las
 * notificaciones push: cuando ese modulo llegue, solo tendra que suscribirse
 * aqui para avisar de que ha salido capitulo nuevo.
 */
public record UnitsAddedEvent(Long workId, String title, int addedUnits) {
}
