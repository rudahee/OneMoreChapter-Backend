package org.rudahee.backend.metadata.integration.provider;

import org.rudahee.backend.metadata.domain.*;
import org.rudahee.backend.metadata.integration.MetadataProvider;
import org.rudahee.backend.metadata.integration.client.ProviderRestClientFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Wikidata: red de seguridad universal. Cubre cualquier obra cultural con
 * entrada en Wikipedia (novelas, teatro, opera, comic europeo, cortos,
 * videojuegos), que es lo que garantiza no tener que rellenar nada a mano.
 *
 * Hace dos peticiones por busqueda, y es deliberado:
 * <ol>
 *   <li>{@code wbsearchentities} para el texto libre. El endpoint SPARQL no
 *       sirve para busqueda difusa: usar {@code FILTER(REGEX(...))} alli es un
 *       antipatron documentado que ademas agota el timeout.</li>
 *   <li>SPARQL con {@code VALUES} para hidratar los Q-ID encontrados, que es
 *       una consulta acotada y rapida.</li>
 * </ol>
 *
 * La robot policy exige User-Agent identificable y gzip (los aplica el
 * {@code RestClient}) y limita a 5 consultas paralelas por IP, de ahi el
 * {@code max-concurrent} de la configuracion.
 *
 * Nota: usa dos hosts distintos, asi que necesita un segundo cliente para la
 * Action API. Ambos comparten la misma puerta de limites porque el limite es
 * de Wikimedia, no del host.
 */
@Component
@ConditionalOnProperty(prefix = "metadata.providers.wikidata", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class WikidataProvider implements MetadataProvider {

    public static final String KEY = "wikidata";
    private static final String ACTION_API_KEY = "wikidata-action";

    private static final Pattern QID = Pattern.compile("Q\\d+");

    /** P31 (instancia de) mapeado a nuestros tipos. Ampliable desde aqui. */
    private static final Map<String, WorkType> TYPE_BY_QID = Map.ofEntries(
            Map.entry("Q11424", WorkType.MOVIE),
            Map.entry("Q24869", WorkType.MOVIE),
            Map.entry("Q93204", WorkType.DOCUMENTARY),
            Map.entry("Q5398426", WorkType.SERIES),
            Map.entry("Q581714", WorkType.SERIES),
            Map.entry("Q63952888", WorkType.ANIME),
            Map.entry("Q220898", WorkType.ANIME),
            Map.entry("Q8274", WorkType.MANGA),
            Map.entry("Q1004", WorkType.COMIC),
            Map.entry("Q1760610", WorkType.COMIC),
            Map.entry("Q571", WorkType.BOOK),
            Map.entry("Q7725634", WorkType.BOOK),
            Map.entry("Q8261", WorkType.BOOK),
            Map.entry("Q482994", WorkType.ALBUM),
            Map.entry("Q134556", WorkType.SONG),
            Map.entry("Q7889", WorkType.GAME),
            Map.entry("Q24634210", WorkType.PODCAST));

    private static final String HYDRATE_QUERY = """
            SELECT ?work ?workLabel ?type ?year ?image ?imdb ?tmdb ?anilist ?mal ?mbid WHERE {
              VALUES ?work { %s }
              OPTIONAL { ?work wdt:P31 ?type }
              OPTIONAL { ?work wdt:P577 ?date . BIND(YEAR(?date) AS ?year) }
              OPTIONAL { ?work wdt:P18 ?image }
              OPTIONAL { ?work wdt:P345 ?imdb }
              OPTIONAL { ?work wdt:P4947 ?tmdb }
              OPTIONAL { ?work wdt:P8731 ?anilist }
              OPTIONAL { ?work wdt:P4086 ?mal }
              OPTIONAL { ?work wdt:P436 ?mbid }
              SERVICE wikibase:label { bd:serviceParam wikibase:language "es,en". }
            }
            """;

    private final RestClient sparqlClient;
    private final RestClient actionClient;

    public WikidataProvider(ProviderRestClientFactory clientFactory) {
        this.sparqlClient = clientFactory.create(KEY);
        this.actionClient = clientFactory.create(ACTION_API_KEY);
    }

    @Override
    public String getKey() {
        return KEY;
    }

    /** Soporta todo: es el ultimo recurso. Prioridad minima en el YAML. */
    @Override
    public boolean supports(WorkType type) {
        return true;
    }

    @Override
    public List<WorkCandidate> search(String query, WorkType type) {
        List<String> qids = searchEntities(query);
        if (qids.isEmpty()) {
            return List.of();
        }
        Map<String, WorkCandidate> hydrated = hydrate(qids, type);
        // Conservamos el orden de relevancia de wbsearchentities: SPARQL no
        // garantiza orden y el primer resultado suele ser el bueno.
        return qids.stream().map(hydrated::get).filter(Objects::nonNull).toList();
    }

    @Override
    public Optional<WorkDetail> findByExternalId(String externalId, WorkType type) {
        if (!QID.matcher(externalId).matches()) {
            return Optional.empty();
        }
        return Optional.ofNullable(hydrate(List.of(externalId), type).get(externalId))
                .map(candidate -> new WorkDetail(KEY, candidate.externalId(), candidate.type(),
                        candidate.title(), null, candidate.year(), candidate.coverUrl(),
                        candidate.type().isEpisodic(), null, WorkStatus.UNKNOWN,
                        candidate.externalIds(), List.of()));
    }

    private List<String> searchEntities(String query) {
        SearchEntitiesResponse response = actionClient.get()
                .uri(uri -> uri
                        .queryParam("action", "wbsearchentities")
                        .queryParam("search", query)
                        .queryParam("language", "es")
                        .queryParam("uselang", "es")
                        .queryParam("type", "item")
                        .queryParam("limit", 10)
                        .queryParam("format", "json")
                        .build())
                .retrieve()
                .body(SearchEntitiesResponse.class);

        if (response == null || response.search() == null) {
            return List.of();
        }
        return response.search().stream()
                .map(SearchEntity::id)
                .filter(Objects::nonNull)
                .filter(id -> QID.matcher(id).matches())
                .toList();
    }

    private Map<String, WorkCandidate> hydrate(List<String> qids, WorkType requested) {
        String values = qids.stream()
                .filter(id -> QID.matcher(id).matches())   // nunca interpolar sin validar
                .map(id -> "wd:" + id)
                .reduce((a, b) -> a + ' ' + b)
                .orElse("");
        if (values.isBlank()) {
            return Map.of();
        }

        SparqlResponse response = sparqlClient.get()
                .uri(uri -> uri
                        .queryParam("format", "json")
                        .queryParam("query", HYDRATE_QUERY.formatted(values))
                        .build())
                .retrieve()
                .body(SparqlResponse.class);

        if (response == null || response.results() == null) {
            return Map.of();
        }

        // Una obra puede producir varias filas (varios P31, varias imagenes).
        // Fusionamos por Q-ID quedandonos con el primer valor no nulo de cada campo.
        Map<String, WorkCandidate> byQid = new LinkedHashMap<>();
        for (Map<String, Binding> row : response.results().bindings()) {
            String qid = extractQid(value(row, "work"));
            if (qid == null) {
                continue;
            }
            WorkCandidate previous = byQid.get(qid);
            byQid.put(qid, mergeRow(previous, row, qid, requested));
        }
        return byQid;
    }

    private WorkCandidate mergeRow(WorkCandidate previous, Map<String, Binding> row,
                                   String qid, WorkType requested) {
        WorkType rowType = TYPE_BY_QID.getOrDefault(extractQid(value(row, "type")),
                requested == null ? WorkType.OTHER : requested);

        Map<String, String> ids = new LinkedHashMap<>();
        if (previous != null) {
            ids.putAll(previous.externalIds());
        }
        ids.put(KEY, qid);
        putIfPresent(ids, "imdb", value(row, "imdb"));
        putIfPresent(ids, "tmdb", value(row, "tmdb"));
        putIfPresent(ids, "anilist", value(row, "anilist"));
        putIfPresent(ids, "myanimelist", value(row, "mal"));
        putIfPresent(ids, "musicbrainz", value(row, "mbid"));

        return new WorkCandidate(KEY, qid,
                previous != null && previous.type() != WorkType.OTHER ? previous.type() : rowType,
                firstNonNull(previous == null ? null : previous.title(), value(row, "workLabel")),
                previous != null && previous.year() != null
                        ? previous.year() : parseInteger(value(row, "year")),
                firstNonNull(previous == null ? null : previous.coverUrl(),
                        commonsUrl(value(row, "image"))),
                0.5, ids);
    }

    /**
     * P18 devuelve el nombre del fichero en Commons, no una URL servible.
     * Special:FilePath resuelve el hash interno y permite pedir un ancho
     * concreto, lo que evita descargar TIFF de decenas de megas.
     */
    private String commonsUrl(String raw) {
        if (raw == null) {
            return null;
        }
        String fileName = raw.substring(raw.lastIndexOf('/') + 1);
        return "https://commons.wikimedia.org/wiki/Special:FilePath/" + fileName + "?width=600";
    }

    private static String extractQid(String uri) {
        if (uri == null) {
            return null;
        }
        String tail = uri.substring(uri.lastIndexOf('/') + 1);
        return QID.matcher(tail).matches() ? tail : null;
    }

    private static String value(Map<String, Binding> row, String variable) {
        Binding binding = row.get(variable);
        if (binding == null || binding.value() == null || binding.value().isBlank()) {
            return null;
        }
        return binding.value();
    }

    private static void putIfPresent(Map<String, String> target, String key, String value) {
        if (value != null) {
            target.putIfAbsent(key, value);
        }
    }

    private static String firstNonNull(String first, String second) {
        return first != null ? first : second;
    }

    private static Integer parseInteger(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return Integer.valueOf(raw);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    record SearchEntitiesResponse(List<SearchEntity> search) { }

    record SearchEntity(String id, String label, String description) { }

    record SparqlResponse(Results results) { }

    record Results(List<Map<String, Binding>> bindings) { }

    record Binding(String type, String value) { }
}
