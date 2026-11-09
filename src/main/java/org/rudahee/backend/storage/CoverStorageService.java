package org.rudahee.backend.storage;

import java.util.Optional;

/**
 * Descarga y custodia de portadas.
 *
 * Las URL de terceros caducan: AniList reorganiza su CDN, TVMaze retira
 * imagenes, iTunes cambia el patron de tamanyos. Una lista con la mitad de las
 * portadas rotas es exactamente lo que no queremos, asi que se guardan copias.
 */
public interface CoverStorageService {

    /**
     * Descarga la imagen y devuelve la ruta publica con la que servirla, o
     * vacio si no se pudo (URL insegura, tipo no permitido, demasiado grande,
     * proveedor caido). Nunca lanza: fallar aqui no debe romper nada.
     */
    Optional<String> store(String remoteUrl);

    void delete(String publicPath);

    /** true si la ruta ya apunta a una copia propia y no hay que descargar nada. */
    boolean isLocal(String url);
}
