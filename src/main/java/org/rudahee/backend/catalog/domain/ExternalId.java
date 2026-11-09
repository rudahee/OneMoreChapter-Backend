package org.rudahee.backend.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Identificador de una obra en un proveedor externo.
 *
 * Se guardan todos los que se conocen, no solo el del proveedor que resolvio la
 * obra. El dia que AniList cambie su politica o TVMaze deje de responder,
 * seguimos teniendo el Q-ID de Wikidata para la misma obra.
 *
 * Es un objeto de valor: no tiene identidad propia, vive dentro de MediaWork.
 * De ahi {@code @Embeddable} y no una entidad con su propio id.
 */
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class ExternalId {

    /** Clave del proveedor: "tvmaze", "anilist", "imdb", "wikidata"... */
    @Column(name = "source", nullable = false, length = 32)
    private String source;

    @Column(name = "external_id", nullable = false, length = 128)
    private String externalId;

    public static ExternalId of(String source, String externalId) {
        return new ExternalId(source, externalId);
    }
}
