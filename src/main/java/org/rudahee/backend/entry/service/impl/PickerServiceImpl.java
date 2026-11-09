package org.rudahee.backend.entry.service.impl;

import lombok.RequiredArgsConstructor;
import org.rudahee.backend.entry.domain.Entry;
import org.rudahee.backend.entry.domain.EntryStatus;
import org.rudahee.backend.entry.repository.EntryRepository;
import org.rudahee.backend.entry.service.PickerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PickerServiceImpl implements PickerService {

    private static final int MAX_PICKS = 5;

    private final EntryRepository entryRepository;
    private final SecureRandom random = new SecureRandom();

    @Override
    public List<Entry> pick(Long spaceId, PickCriteria criteria) {
        List<EntryStatus> statuses = criteria.onlyStarted()
                ? List.of(EntryStatus.IN_PROGRESS)
                : List.of(EntryStatus.BACKLOG, EntryStatus.IN_PROGRESS, EntryStatus.PAUSED);

        List<Entry> candidates = new ArrayList<>(entryRepository.findCandidates(
                spaceId, statuses, criteria.type(), criteria.episodic()));

        int count = Math.clamp(criteria.count(), 1, MAX_PICKS);
        return weightedSample(candidates, count);
    }

    /**
     * Muestreo sin reemplazo ponderado por prioridad.
     *
     * La prioridad influye pero no manda: {@code peso = 1 + prioridad}, asi que
     * algo marcado con prioridad 3 sale cuatro veces mas que lo normal, pero lo
     * normal sigue saliendo. Si la prioridad decidiese sola, la ruleta dejaria
     * de ser una ruleta y volveriais a ver siempre lo mismo.
     */
    private List<Entry> weightedSample(List<Entry> candidates, int count) {
        List<Entry> pool = new ArrayList<>(candidates);
        List<Entry> picks = new ArrayList<>(Math.min(count, pool.size()));

        while (!pool.isEmpty() && picks.size() < count) {
            int totalWeight = pool.stream().mapToInt(this::weightOf).sum();
            int target = random.nextInt(totalWeight);

            int cumulative = 0;
            for (int i = 0; i < pool.size(); i++) {
                cumulative += weightOf(pool.get(i));
                if (target < cumulative) {
                    picks.add(pool.remove(i));
                    break;
                }
            }
        }
        return picks;
    }

    private int weightOf(Entry entry) {
        return 1 + Math.max(0, entry.getPriority());
    }
}
