package org.rudahee.backend.metadata.service;

import org.rudahee.backend.metadata.domain.WorkCandidate;
import org.rudahee.backend.metadata.integration.ratelimit.ProviderGateRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Fusiona candidatos que distintos proveedores devuelven para la misma obra.
 *
 * Se separa del servicio porque es logica pura y sin dependencias de red, lo
 * que la hace trivial de probar con tests unitarios.
 */
@Component
@RequiredArgsConstructor
public class CandidateMerger {

    private final ProviderGateRegistry gates;

    public List<WorkCandidate> merge(List<WorkCandidate> candidates) {
        Map<String, WorkCandidate> byGroup = new LinkedHashMap<>();
        Map<String, String> groupByExternalId = new HashMap<>();

        for (WorkCandidate candidate : candidates) {
            String group = findGroup(candidate, groupByExternalId)
                    .orElseGet(candidate::dedupeKey);

            WorkCandidate winner = pickWinner(byGroup.get(group), candidate);
            byGroup.put(group, winner);
            indexExternalIds(winner, group, groupByExternalId);
        }

        return byGroup.values().stream()
                .sorted(Comparator.comparingDouble(this::rank).reversed())
                .toList();
    }

    /** Casar por identificador compartido es exacto; el titulo es el plan B. */
    private Optional<String> findGroup(WorkCandidate candidate, Map<String, String> index) {
        return candidate.externalIds().entrySet().stream()
                .map(this::compositeKey)
                .map(index::get)
                .filter(Objects::nonNull)
                .findFirst();
    }

    private void indexExternalIds(WorkCandidate candidate, String group,
                                  Map<String, String> index) {
        candidate.externalIds().entrySet()
                .forEach(entry -> index.put(compositeKey(entry), group));
    }

    private String compositeKey(Map.Entry<String, String> externalId) {
        return externalId.getKey() + '=' + externalId.getValue();
    }

    /**
     * Gana el proveedor de mayor prioridad, pero se acumulan los identificadores
     * de todos y se rellenan los huecos del ganador con los datos del perdedor:
     * es muy comun que Wikidata sepa el anyo y iTunes tenga la mejor portada.
     */
    private WorkCandidate pickWinner(WorkCandidate current, WorkCandidate incoming) {
        if (current == null) {
            return incoming;
        }

        boolean incomingWins = gates.priorityOf(incoming.source())
                > gates.priorityOf(current.source());
        WorkCandidate winner = incomingWins ? incoming : current;
        WorkCandidate loser = incomingWins ? current : incoming;

        Map<String, String> ids = new LinkedHashMap<>(winner.externalIds());
        loser.externalIds().forEach(ids::putIfAbsent);

        return new WorkCandidate(
                winner.source(),
                winner.externalId(),
                winner.type(),
                winner.title() != null ? winner.title() : loser.title(),
                winner.year() != null ? winner.year() : loser.year(),
                winner.coverUrl() != null ? winner.coverUrl() : loser.coverUrl(),
                Math.max(winner.score(), loser.score()),
                ids);
    }

    /** La prioridad del proveedor solo desempata; manda la relevancia. */
    private double rank(WorkCandidate candidate) {
        return candidate.score() + gates.priorityOf(candidate.source()) / 1000.0;
    }
}
