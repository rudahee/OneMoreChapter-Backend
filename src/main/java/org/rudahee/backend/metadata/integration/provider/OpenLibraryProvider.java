package org.rudahee.backend.metadata.integration.provider;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.rudahee.backend.metadata.domain.*;
import org.rudahee.backend.metadata.integration.MetadataProvider;
import org.rudahee.backend.metadata.integration.client.ProviderRestClientFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Open Library: libros. Sin clave ni registro.
 *
 * Mejor que iTunes para literatura porque indexa la obra y no la edicion
 * comercial: buscar "El cero y el infinito" devuelve el libro, no catorce
 * ediciones de bolsillo distintas.
 */
@Component
@ConditionalOnProperty(prefix = "metadata.providers.openlibrary", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class OpenLibraryProvider implements MetadataProvider {

    public static final String KEY = "openlibrary";

    private final RestClient client;

    public OpenLibraryProvider(ProviderRestClientFactory clientFactory) {
        this.client = clientFactory.create(KEY);
    }

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public boolean supports(WorkType type) {
        return type == WorkType.BOOK;
    }

    @Override
    public List<WorkCandidate> search(String query, WorkType type) {
        SearchResponse response = client.get()
                .uri(uri -> uri.path("/search.json")
                        .queryParam("q", query)
                        .queryParam("limit", 10)
                        // Sin esto devuelve decenas de campos por resultado.
                        .queryParam("fields", "key,title,first_publish_year,cover_i,author_name")
                        .build())
                .retrieve()
                .body(SearchResponse.class);

        if (response == null || response.docs() == null) {
            return List.of();
        }
        return response.docs().stream()
                .filter(doc -> doc.key() != null)
                .map(doc -> new WorkCandidate(KEY, doc.workId(), WorkType.BOOK,
                        doc.title(), doc.firstPublishYear(), doc.coverUrl(),
                        0.75, Map.of(KEY, doc.workId())))
                .toList();
    }

    @Override
    public Optional<WorkDetail> findByExternalId(String externalId, WorkType type) {
        WorkResponse response = client.get()
                .uri("/works/{id}.json", externalId)
                .retrieve()
                .body(WorkResponse.class);

        return Optional.ofNullable(response).map(work -> {
            Map<String, String> ids = new LinkedHashMap<>();
            ids.put(KEY, externalId);

            return new WorkDetail(KEY, externalId, WorkType.BOOK, work.title(), null,
                    null, work.coverUrl(), false, null, WorkStatus.FINISHED,
                    ids, List.of());
        });
    }

    record SearchResponse(List<Doc> docs, Integer numFound) { }

    record Doc(String key, String title,
               @JsonProperty("first_publish_year") Integer firstPublishYear,
               @JsonProperty("cover_i") Integer coverId,
               @JsonProperty("author_name") List<String> authorNames) {

        /** La clave viene como "/works/OL123W"; guardamos solo el identificador. */
        String workId() {
            return key == null ? null : key.substring(key.lastIndexOf('/') + 1);
        }

        String coverUrl() {
            return coverId == null
                    ? null
                    : "https://covers.openlibrary.org/b/id/%d-L.jpg".formatted(coverId);
        }
    }

    record WorkResponse(String title, List<Integer> covers) {

        String coverUrl() {
            return covers == null || covers.isEmpty()
                    ? null
                    : "https://covers.openlibrary.org/b/id/%d-L.jpg".formatted(covers.get(0));
        }
    }
}
