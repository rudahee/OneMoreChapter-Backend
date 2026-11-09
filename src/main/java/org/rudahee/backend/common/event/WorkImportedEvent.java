package org.rudahee.backend.common.event;

/**
 * Una obra acaba de entrar en el catalogo o ha cambiado de portada.
 *
 * Existe para que {@code catalog} no dependa de {@code storage}: publica el
 * aviso y sigue. Si la descarga falla o el modulo esta desactivado, la
 * importacion no se entera.
 *
 * @param remoteCoverUrl URL en el proveedor externo, aun sin descargar
 */
public record WorkImportedEvent(Long workId, String remoteCoverUrl) {
}
