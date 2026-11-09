package org.rudahee.backend.metadata.integration.provider;

import org.rudahee.backend.metadata.domain.*;
import org.rudahee.backend.metadata.integration.MetadataProvider;
import org.rudahee.backend.metadata.integration.client.ProviderRestClientFactory;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * AniList: anime y manga. GraphQL sin clave para datos publicos.
 *
 * Limite documentado: 90 peticiones por minuto por IP, y superarlo cuesta un
 * minuto completo de timeout. De ahi que el enfriamiento configurado sea de 60
 * segundos: reintentar antes es tiempo perdido.
 *
 * No expone la lista de episodios o capitulos uno a uno, solo el total. El
 * servicio sintetiza las unidades a partir de ese numero.
 */
@Component
@ConditionalOnProperty(prefix = "metadata.providers.anilist", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class AniListProvider implements MetadataProvider {

    public static final String KEY = "anilist";

    private static final String MEDIA_FIELDS = """
            id idMal type status
            title { romaji english native }
            startDate { year }
            episodes chapters
            coverImage { extraLarge large }
            """;

    private static final String SEARCH_QUERY = """
            query ($search: String, $type: MediaType) {
              Page(page: 1, perPage: 10) {
                media(search: $search, type: $type, sort: SEARCH_MATCH) { %s }
              }
            }
            """.formatted(MEDIA_FIELDS);

    private static final String BY_ID_QUERY = """
            query ($id: Int) {
              Media(id: $id) { %s }
            }
            """.formatted(MEDIA_FIELDS);

    private final RestClient client;

    public AniListProvider(ProviderRestClientFactory clientFactory) {
        this.client = clientFactory.create(KEY);
    }

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public boolean supports(WorkType type) {
        return type == WorkType.ANIME || type == WorkType.MANGA;
    }

    @Override
    public List<WorkCandidate> search(String query, WorkType type) {
        GraphQlRequest request = new GraphQlRequest(SEARCH_QUERY,
                Map.of("search", query, "type", type == WorkType.MANGA ? "MANGA" : "ANIME"));

        GraphQlResponse response = client.post().uri("/").body(request)
                .retrieve().body(GraphQlResponse.class);

        List<Media> media = Optional.ofNullable(response)
                .map(GraphQlResponse::data)
                .map(Data::page)
                .map(Page::media)
                .orElse(List.of());

        return media.stream().map(this::toCandidate).toList();
    }

    @Override
    public Optional<WorkDetail> findByExternalId(String externalId, WorkType type) {
        int id;
        try {
            id = Integer.parseInt(externalId);
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }

        GraphQlRequest request = new GraphQlRequest(BY_ID_QUERY, Map.of("id", id));
        GraphQlResponse response = client.post().uri("/").body(request)
                .retrieve().body(GraphQlResponse.class);

        return Optional.ofNullable(response)
                .map(GraphQlResponse::data)
                .map(Data::media)
                .map(this::toDetail);
    }

    private WorkCandidate toCandidate(Media media) {
        return new WorkCandidate(KEY, String.valueOf(media.id()), mapType(media),
                resolveTitle(media), media.year(), resolveCover(media),
                // AniList ordena por SEARCH_MATCH pero no expone una puntuacion.
                0.9, extractExternalIds(media));
    }

    private WorkDetail toDetail(Media media) {
        WorkType type = mapType(media);
        Integer total = type == WorkType.MANGA ? media.chapters() : media.episodes();
        return new WorkDetail(KEY, String.valueOf(media.id()), type, resolveTitle(media),
                media.title() == null ? null : media.title().nativeTitle(), media.year(),
                resolveCover(media), true, total, mapStatus(media.status()),
                extractExternalIds(media), List.of());
    }

    private WorkType mapType(Media media) {
        return "MANGA".equals(media.type()) ? WorkType.MANGA : WorkType.ANIME;
    }

    private WorkStatus mapStatus(String status) {
        if (status == null) {
            return WorkStatus.UNKNOWN;
        }
        return switch (status) {
            case "RELEASING" -> WorkStatus.ONGOING;
            case "FINISHED" -> WorkStatus.FINISHED;
            case "NOT_YET_RELEASED" -> WorkStatus.UPCOMING;
            case "CANCELLED" -> WorkStatus.CANCELLED;
            case "HIATUS" -> WorkStatus.HIATUS;
            default -> WorkStatus.UNKNOWN;
        };
    }

    /** Romaji primero: es como se conocen estas obras en la practica. */
    private String resolveTitle(Media media) {
        if (media.title() == null) {
            return null;
        }
        Title title = media.title();
        if (title.romaji() != null) {
            return title.romaji();
        }
        return title.english() != null ? title.english() : title.nativeTitle();
    }

    private String resolveCover(Media media) {
        if (media.coverImage() == null) {
            return null;
        }
        return media.coverImage().extraLarge() != null
                ? media.coverImage().extraLarge()
                : media.coverImage().large();
    }

    private Map<String, String> extractExternalIds(Media media) {
        Map<String, String> ids = new LinkedHashMap<>();
        ids.put(KEY, String.valueOf(media.id()));
        if (media.idMal() != null) {
            ids.put("myanimelist", String.valueOf(media.idMal()));
        }
        return ids;
    }

    record GraphQlRequest(String query, Map<String, Object> variables) { }

    record GraphQlResponse(Data data) { }

    /** GraphQL devuelve las claves con la capitalizacion del esquema. */
    record Data(@JsonProperty("Page") Page page, @JsonProperty("Media") Media media) { }

    record Page(List<Media> media) { }

    record Media(Integer id, Integer idMal, String type, String status,
                 Title title, StartDate startDate, Integer episodes,
                 Integer chapters, CoverImage coverImage) {

        Integer year() {
            return startDate == null ? null : startDate.year();
        }
    }

    record Title(String romaji, String english,
                 @JsonProperty("native") String nativeTitle) { }

    record StartDate(Integer year) { }

    record CoverImage(String extraLarge, String large) { }
}
