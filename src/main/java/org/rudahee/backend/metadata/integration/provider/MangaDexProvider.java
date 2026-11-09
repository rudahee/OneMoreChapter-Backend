package org.rudahee.backend.metadata.integration.provider;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.rudahee.backend.metadata.domain.*;
import org.rudahee.backend.metadata.integration.MetadataProvider;
import org.rudahee.backend.metadata.integration.client.ProviderRestClientFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * MangaDex: manga con lista real de capitulos.
 *
 * Complementa a AniList en vez de sustituirlo. AniList tiene mejores titulos y
 * portadas pero solo da el total de capitulos; MangaDex da el listado uno a
 * uno, que es lo que permite anotar "capitulo 112" en lugar de una unidad
 * sintetica sin nombre. Para un club de lectura, esa diferencia importa.
 *
 * Sin autenticacion para metadatos, limite de unas 5 peticiones por segundo por
 * IP. La lectura de capitulos si esta restringida desde 2026, pero no la
 * tocamos: solo pedimos titulos y numeracion.
 */
@Component
@ConditionalOnProperty(prefix = "metadata.providers.mangadex", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class MangaDexProvider implements MetadataProvider {

    public static final String KEY = "mangadex";

    /** Tope de capitulos a traer. Berserk ronda los 400; 2000 cubre cualquier cosa. */
    private static final int MAX_CHAPTERS = 2000;
    private static final int PAGE_SIZE = 100;

    private final RestClient client;

    public MangaDexProvider(ProviderRestClientFactory clientFactory) {
        this.client = clientFactory.create(KEY);
    }

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public boolean supports(WorkType type) {
        return type == WorkType.MANGA || type == WorkType.COMIC;
    }

    @Override
    public List<WorkCandidate> search(String query, WorkType type) {
        MangaListResponse response = client.get()
                .uri(uri -> uri.path("/manga")
                        .queryParam("title", query)
                        .queryParam("limit", 10)
                        .queryParam("contentRating[]", "safe", "suggestive")
                        .queryParam("includes[]", "cover_art")
                        .build())
                .retrieve()
                .body(MangaListResponse.class);

        if (response == null || response.data() == null) {
            return List.of();
        }
        return response.data().stream()
                .map(manga -> new WorkCandidate(KEY, manga.id(), WorkType.MANGA,
                        manga.resolveTitle(), manga.year(), manga.coverUrl(),
                        0.8, Map.of(KEY, manga.id())))
                .toList();
    }

    @Override
    public Optional<WorkDetail> findByExternalId(String externalId, WorkType type) {
        MangaResponse response = client.get()
                .uri(uri -> uri.path("/manga/{id}")
                        .queryParam("includes[]", "cover_art")
                        .build(externalId))
                .retrieve()
                .body(MangaResponse.class);

        return Optional.ofNullable(response)
                .map(MangaResponse::data)
                .map(manga -> new WorkDetail(KEY, manga.id(), WorkType.MANGA,
                        manga.resolveTitle(), manga.nativeTitle(), manga.year(),
                        manga.coverUrl(), true, manga.lastChapterNumber(),
                        mapStatus(manga.attributes() == null ? null : manga.attributes().status()),
                        Map.of(KEY, manga.id()), List.of()));
    }

    /**
     * Recorre el feed paginado quedandose con un capitulo por numero.
     *
     * MangaDex devuelve el mismo capitulo repetido por cada grupo de traduccion
     * y cada idioma. Sin deduplicar, un manga de 300 capitulos genera miles de
     * unidades y el progreso se vuelve inservible.
     */
    @Override
    public List<UnitDetail> findUnits(String externalId, WorkType type) {
        Map<Double, UnitDetail> byNumber = new TreeMap<>();

        for (int offset = 0; offset < MAX_CHAPTERS; offset += PAGE_SIZE) {
            final int currentOffset = offset;
            ChapterListResponse page = client.get()
                    .uri(uri -> uri.path("/manga/{id}/feed")
                            .queryParam("limit", PAGE_SIZE)
                            .queryParam("offset", currentOffset)
                            .queryParam("translatedLanguage[]", "es", "es-la", "en")
                            .queryParam("order[chapter]", "asc")
                            .queryParam("contentRating[]", "safe", "suggestive")
                            .build(externalId))
                    .retrieve()
                    .body(ChapterListResponse.class);

            if (page == null || page.data() == null || page.data().isEmpty()) {
                break;
            }

            for (ChapterData chapter : page.data()) {
                Double number = chapter.chapterNumber();
                if (number == null) {
                    continue; // oneshots y extras sin numeracion
                }
                byNumber.putIfAbsent(number, new UnitDetail(
                        null, (int) Math.floor(number),
                        chapter.attributes().title(),
                        chapter.publishDate(), null));
            }

            if (page.total() != null && currentOffset + PAGE_SIZE >= page.total()) {
                break;
            }
        }
        return List.copyOf(byNumber.values());
    }

    private WorkStatus mapStatus(String status) {
        if (status == null) {
            return WorkStatus.UNKNOWN;
        }
        return switch (status) {
            case "ongoing" -> WorkStatus.ONGOING;
            case "completed" -> WorkStatus.FINISHED;
            case "hiatus" -> WorkStatus.HIATUS;
            case "cancelled" -> WorkStatus.CANCELLED;
            default -> WorkStatus.UNKNOWN;
        };
    }

    // --- Respuestas de la API ---

    record MangaListResponse(List<MangaData> data, Integer total) { }

    record MangaResponse(MangaData data) { }

    record MangaData(String id, MangaAttributes attributes, List<Relationship> relationships) {

        /**
         * Los titulos vienen como mapa de idioma a texto. Se prefiere el
         * castellano, luego ingles, y en ultimo caso el primero que haya.
         */
        String resolveTitle() {
            if (attributes == null || attributes.title() == null) {
                return null;
            }
            Map<String, String> titles = attributes.title();
            for (String language : new String[]{"es", "es-la", "en", "ja-ro"}) {
                if (titles.containsKey(language)) {
                    return titles.get(language);
                }
            }
            return titles.values().stream().findFirst().orElse(null);
        }

        String nativeTitle() {
            return attributes == null || attributes.title() == null
                    ? null : attributes.title().get("ja");
        }

        Integer year() {
            return attributes == null ? null : attributes.year();
        }

        Integer lastChapterNumber() {
            if (attributes == null || attributes.lastChapter() == null) {
                return null;
            }
            try {
                return (int) Math.floor(Double.parseDouble(attributes.lastChapter()));
            } catch (NumberFormatException ex) {
                return null;
            }
        }

        /**
         * La portada llega como relacion con solo el nombre del fichero; hay
         * que componer la URL del CDN a mano. El sufijo .512.jpg pide la
         * version reducida en vez del original a tamanyo completo.
         */
        String coverUrl() {
            if (relationships == null) {
                return null;
            }
            return relationships.stream()
                    .filter(rel -> "cover_art".equals(rel.type()))
                    .map(Relationship::attributes)
                    .filter(Objects::nonNull)
                    .map(RelationshipAttributes::fileName)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .map(file -> "https://uploads.mangadex.org/covers/%s/%s.512.jpg"
                            .formatted(id, file))
                    .orElse(null);
        }
    }

    record MangaAttributes(Map<String, String> title, Integer year,
                           String status, String lastChapter) { }

    record Relationship(String id, String type, RelationshipAttributes attributes) { }

    record RelationshipAttributes(String fileName) { }

    record ChapterListResponse(List<ChapterData> data, Integer total) { }

    record ChapterData(String id, ChapterAttributes attributes) {

        Double chapterNumber() {
            if (attributes == null || attributes.chapter() == null) {
                return null;
            }
            try {
                return Double.valueOf(attributes.chapter());
            } catch (NumberFormatException ex) {
                return null;
            }
        }

        LocalDate publishDate() {
            if (attributes == null || attributes.publishAt() == null) {
                return null;
            }
            try {
                return OffsetDateTime.parse(attributes.publishAt()).toLocalDate();
            } catch (DateTimeParseException ex) {
                return null;
            }
        }
    }

    record ChapterAttributes(String chapter, String title,
                             @JsonProperty("publishAt") String publishAt) { }
}
