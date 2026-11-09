package org.rudahee.backend.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rudahee.backend.catalog.repository.MediaWorkRepository;
import org.rudahee.backend.common.event.WorkImportedEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Descarga la portada en segundo plano al importar una obra.
 *
 * {@code @Async} porque bajar una imagen puede tardar segundos y quien esta
 * anyadiendo algo a la lista no tiene por que esperar: la ficha aparece al
 * momento con la URL del proveedor y la copia propia la sustituye despues.
 *
 * {@code AFTER_COMMIT} porque si la importacion hace rollback, no hay obra a la
 * que asignar la portada.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CoverDownloadListener {

    private final CoverStorageService coverStorage;
    private final MediaWorkRepository workRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onWorkImported(WorkImportedEvent event) {
        if (event.remoteCoverUrl() == null || coverStorage.isLocal(event.remoteCoverUrl())) {
            return;
        }

        coverStorage.store(event.remoteCoverUrl()).ifPresent(localPath ->
                workRepository.findById(event.workId()).ifPresent(work -> {
                    // Se relee la obra en vez de recibirla en el evento: entre
                    // el commit y esta linea puede haber pasado cualquier cosa,
                    // y una entidad de otra transaccion aqui estaria desligada.
                    String previous = work.getCoverUrl();
                    work.setCoverUrl(localPath);

                    if (coverStorage.isLocal(previous) && !previous.equals(localPath)) {
                        coverStorage.delete(previous);
                    }
                    log.debug("Portada guardada para la obra {}", event.workId());
                }));
    }
}
