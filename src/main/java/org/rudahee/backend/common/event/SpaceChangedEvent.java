package org.rudahee.backend.common.event;

/**
 * Aviso de que algo cambio en un espacio.
 *
 * Vive en {@code common} y no en {@code realtime} para que quien lo publica no
 * dependa de quien lo consume: {@code entry} solo conoce
 * {@code ApplicationEventPublisher}, y podria haber cero o diez oyentes sin
 * que le afecte.
 *
 * No lleva el dato que cambio, solo que cambio y donde. La alternativa seria
 * enviar el delta completo para que el cliente lo aplique sin volver a pedir
 * nada, pero eso obliga a que los servicios construyan DTO de la capa web y
 * abre la puerta a divergencias si dos mensajes llegan desordenados. Avisar y
 * que el cliente recargue el recurso afectado se corrige solo.
 *
 * @param version version de la entrada tras el cambio, para que el cliente
 *                pueda descartar avisos que ya ha superado
 * @param actorUserId quien lo provoco: permite al cliente ignorar el eco de sus
 *                    propias acciones, que ya aplico de forma optimista
 */
public record SpaceChangedEvent(
        Long spaceId,
        SpaceChangeType type,
        Long entryId,
        Long actorUserId,
        Long version) {

    public static SpaceChangedEvent of(Long spaceId, SpaceChangeType type,
                                       Long entryId, Long actorUserId, Long version) {
        return new SpaceChangedEvent(spaceId, type, entryId, actorUserId, version);
    }
}
