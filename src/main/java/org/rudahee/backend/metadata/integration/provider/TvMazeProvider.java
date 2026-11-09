package org.rudahee.backend.metadata.integration.provider;

import org.rudahee.backend.metadata.domain.*;
import org.rudahee.backend.metadata.integration.MetadataProvider;
import org.rudahee.backend.metadata.integration.client.ProviderRestClientFactory;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * TVMaze: series y documentales de television.
 *
 * Sin clave ni registro. Limite documentado: al menos 20 llamadas cada 10
 * segundos por IP. Es la unica fuente gratuita de esta lista que da la lista
 * completa de episodios con temporada y fecha de emision.
 */
@Component
@ConditionalOnProperty(prefix = "metadata.providers.tvmaze", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class TvMazeProvider implements MetadataProvider {

    public static final String KEY = "tvmaze";

    private static final ParameterizedTypeReference<List<SearchHit>> SEARCH_HITS =
            new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<List<Episode>> EPISODES =
            new ParameterizedTypeReference<>() { };

    private final RestClient client;

    public TvMazeProvider(ProviderRestClientFactory clientFactory) {
        this.client = clientFactory.create(KEY);
    }

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public boolean supports(WorkType type) {
        return type == WorkType.SERIES || type == WorkType.DOCUMENTARY
                || type == WorkType.ANIME;
    }

    @Override
    public List<WorkCandidate> search(String query, WorkType type) {
        List<SearchHit> hits = client.get()
                .uri(uri -> uri.path("/search/shows").queryParam("q", query).build())
                .retrieve()
                .body(SEARCH_HITS);

        if (hits == null) {
            return List.of();
        }
        return hits.stream()
                .filter(hit -> hit.show() != null)
                .map(hit -> toCandidate(hit.show(), hit.score(), type))
                .toList();
    }

    @Override
    public Optional<WorkDetail> findByExternalId(String externalId, WorkType type) {
        Show show = client.get()
                .uri("/shows/{id}", externalId)
                .retrieve()
                .body(Show.class);
        return Optional.ofNullable(show).map(s -> toDetail(s, type));
    }

    @Override
    public List<UnitDetail> findUnits(String externalId, WorkType type) {
        List<Episode> episodes = client.get()
                .uri(uri -> uri.path("/shows/{id}/episodes")
                        .queryParam("specials", 1)
                        .build(externalId))
                .retrieve()
                .body(EPISODES);

        if (episodes == null) {
            return List.of();
        }
        return episodes.stream()
                .map(ep -> new UnitDetail(ep.season(), ep.number() == null ? 0 : ep.number(),
                        ep.name(), parseDate(ep.airdate()), ep.runtime()))
                .toList();
    }

    private WorkCandidate toCandidate(Show show, Double score, WorkType requested) {
        return new WorkCandidate(KEY, String.valueOf(show.id()), resolveType(show, requested),
                show.name(), extractYear(show), extractImage(show),
                // TVMaze puntua por relevancia sin techo fijo; acotamos a 0..1.
                score == null ? 0.5 : Math.min(1.0, score),
                extractExternalIds(show));
    }

    private WorkDetail toDetail(Show show, WorkType requested) {
        return new WorkDetail(KEY, String.valueOf(show.id()), resolveType(show, requested),
                show.name(), null, extractYear(show), extractImage(show), true,
                null, mapStatus(show.status()), extractExternalIds(show), List.of());
    }

    private WorkType resolveType(Show show, WorkType requested) {
        if (requested != null && requested != WorkType.OTHER) {
            return requested;
        }
        List<String> genres = show.genres() == null ? List.of() : show.genres();
        if (genres.stream().anyMatch("Anime"::equalsIgnoreCase)) {
            return WorkType.ANIME;
        }
        if (genres.stream().anyMatch("Documentary"::equalsIgnoreCase)) {
            return WorkType.DOCUMENTARY;
        }
        return WorkType.SERIES;
    }

    private Map<String, String> extractExternalIds(Show show) {
        Map<String, String> ids = new LinkedHashMap<>();
        ids.put(KEY, String.valueOf(show.id()));
        if (show.externals() != null) {
            if (show.externals().imdb() != null) {
                ids.put("imdb", show.externals().imdb());
            }
            if (show.externals().thetvdb() != null) {
                ids.put("thetvdb", String.valueOf(show.externals().thetvdb()));
            }
        }
        return ids;
    }

    private WorkStatus mapStatus(String status) {
        if (status == null) {
            return WorkStatus.UNKNOWN;
        }
        return switch (status) {
            case "Running", "In Development" -> WorkStatus.ONGOING;
            case "Ended" -> WorkStatus.FINISHED;
            case "To Be Determined" -> WorkStatus.HIATUS;
            default -> WorkStatus.UNKNOWN;
        };
    }

    private String extractImage(Show show) {
        if (show.image() == null) {
            return null;
        }
        return show.image().original() != null ? show.image().original() : show.image().medium();
    }

    private Integer extractYear(Show show) {
        LocalDate premiered = parseDate(show.premiered());
        return premiered == null ? null : premiered.getYear();
    }

    private LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(raw);
        } catch (java.time.format.DateTimeParseException ex) {
            return null;
        }
    }

    // Respuestas de la API. Spring Boot desactiva FAIL_ON_UNKNOWN_PROPERTIES por
    // defecto, asi que basta con declarar los campos que usamos.

    record SearchHit(Double score, Show show) { }

    record Show(Integer id, String name, String premiered, String status,
                List<String> genres, Image image, Externals externals) { }

    record Image(String medium, String original) { }

    record Externals(String imdb, @JsonProperty("thetvdb") Integer thetvdb) { }

    record Episode(Integer season, Integer number, String name,
                   String airdate, Integer runtime) { }
}
