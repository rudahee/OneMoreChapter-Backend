package org.rudahee.backend.metadata.integration.provider;

import org.rudahee.backend.metadata.domain.*;
import org.rudahee.backend.metadata.integration.MetadataProvider;
import org.rudahee.backend.metadata.integration.client.ProviderRestClientFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * iTunes Search: cine, musica, podcasts, audiolibros y ebooks.
 *
 * No hay nada que registrar ni cabecera de autenticacion que enviar. El limite
 * documentado ronda las 20 llamadas por minuto y es por IP, no por clave, asi
 * que no tenemos identidad ante Apple ni forma de pedir mas cuota: la cache no
 * es una optimizacion aqui, es un requisito de funcionamiento.
 *
 * Tampoco envia cabeceras CORS, pero es irrelevante porque el frontend nunca
 * habla directamente con proveedores externos.
 */
@Component
@ConditionalOnProperty(prefix = "metadata.providers.itunes", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class ITunesProvider implements MetadataProvider {

    public static final String KEY = "itunes";

    /** Tienda por defecto: afecta al idioma de los titulos. */
    private static final String COUNTRY = "ES";

    private final RestClient client;

    public ITunesProvider(ProviderRestClientFactory clientFactory) {
        this.client = clientFactory.create(KEY);
    }

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public boolean supports(WorkType type) {
        return switch (type) {
            case MOVIE, DOCUMENTARY, SONG, ALBUM, PODCAST, BOOK -> true;
            default -> false;
        };
    }

    @Override
    public List<WorkCandidate> search(String query, WorkType type) {
        SearchResponse response = client.get()
                .uri(uri -> uri.path("/search")
                        .queryParam("term", query)
                        .queryParam("media", mediaParam(type))
                        .queryParam("entity", entityParam(type))
                        .queryParam("country", COUNTRY)
                        .queryParam("limit", 10)
                        .build())
                .retrieve()
                .body(SearchResponse.class);

        return results(response).stream()
                .filter(result -> result.resolveId() != null)
                .map(result -> toCandidate(result, type))
                .toList();
    }

    @Override
    public Optional<WorkDetail> findByExternalId(String externalId, WorkType type) {
        SearchResponse response = client.get()
                .uri(uri -> uri.path("/lookup")
                        .queryParam("id", externalId)
                        .queryParam("country", COUNTRY)
                        .build())
                .retrieve()
                .body(SearchResponse.class);

        return results(response).stream()
                .filter(result -> result.resolveId() != null)
                .findFirst()
                .map(result -> toDetail(result, type));
    }

    private List<Result> results(SearchResponse response) {
        return response == null || response.results() == null ? List.of() : response.results();
    }

    private WorkCandidate toCandidate(Result result, WorkType type) {
        String id = result.resolveId();
        return new WorkCandidate(KEY, id, type, result.resolveName(), result.releaseYear(),
                result.artwork(), 0.7, Map.of(KEY, id));
    }

    private WorkDetail toDetail(Result result, WorkType type) {
        String id = result.resolveId();
        return new WorkDetail(KEY, id, type, result.resolveName(), null, result.releaseYear(),
                result.artwork(), type.isEpisodic(), result.trackCount(), WorkStatus.UNKNOWN,
                Map.of(KEY, id), List.of());
    }

    private String mediaParam(WorkType type) {
        return switch (type) {
            case MOVIE, DOCUMENTARY -> "movie";
            case SONG, ALBUM -> "music";
            case PODCAST -> "podcast";
            case BOOK -> "ebook";
            default -> "all";
        };
    }

    private String entityParam(WorkType type) {
        return switch (type) {
            case MOVIE, DOCUMENTARY -> "movie";
            case SONG -> "musicTrack";
            case ALBUM -> "album";
            case PODCAST -> "podcast";
            case BOOK -> "ebook";
            default -> "all";
        };
    }

    record SearchResponse(Integer resultCount, List<Result> results) { }

    record Result(Long trackId, Long collectionId, Long artistId,
                  String trackName, String collectionName, String artistName,
                  String releaseDate, String artworkUrl100, String artworkUrl60,
                  Integer trackCount) {

        String resolveId() {
            if (trackId != null) {
                return String.valueOf(trackId);
            }
            if (collectionId != null) {
                return String.valueOf(collectionId);
            }
            return artistId == null ? null : String.valueOf(artistId);
        }

        String resolveName() {
            if (trackName != null) {
                return trackName;
            }
            return collectionName != null ? collectionName : artistName;
        }

        Integer releaseYear() {
            if (releaseDate == null || releaseDate.length() < 4) {
                return null;
            }
            try {
                return Integer.valueOf(releaseDate.substring(0, 4));
            } catch (NumberFormatException ex) {
                return null;
            }
        }

        /**
         * La API devuelve miniaturas de 100 px, pero el patron de URL admite
         * pedir cualquier tamanyo sustituyendo el segmento.
         */
        String artwork() {
            String url = artworkUrl100 != null ? artworkUrl100 : artworkUrl60;
            if (url == null) {
                return null;
            }
            return url.replace("100x100bb", "600x600bb").replace("60x60bb", "600x600bb");
        }
    }
}
