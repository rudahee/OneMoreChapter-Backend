package org.rudahee.backend.realtime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rudahee.backend.common.event.SpaceChangedEvent;
import org.rudahee.backend.realtime.web.dto.SpaceEventMessage;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Unico punto que empuja mensajes a los clientes.
 *
 * {@code AFTER_COMMIT} no es un detalle: publicar antes del commit avisaria de
 * cambios que luego hacen rollback, y el otro dispositivo recargaria para
 * encontrarse el estado anterior. Con este listener, si la transaccion falla,
 * el evento simplemente no se emite.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpaceEventBroadcaster {

    private static final String TOPIC_TEMPLATE = "/topic/spaces/%d";

    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void broadcast(SpaceChangedEvent event) {
        String destination = TOPIC_TEMPLATE.formatted(event.spaceId());
        try {
            messagingTemplate.convertAndSend(destination, SpaceEventMessage.from(event));
        } catch (RuntimeException ex) {
            // Un fallo al emitir nunca debe tumbar la peticion: el cambio ya
            // esta guardado y lo peor que pasa es que el otro dispositivo tarde
            // en enterarse hasta la siguiente recarga.
            log.warn("No se pudo emitir {} en {}", event.type(), destination, ex);
        }
    }
}
