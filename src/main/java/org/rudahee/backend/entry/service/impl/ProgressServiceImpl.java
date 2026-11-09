package org.rudahee.backend.entry.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rudahee.backend.catalog.domain.MediaUnit;
import org.rudahee.backend.catalog.repository.MediaUnitRepository;
import org.rudahee.backend.common.exception.BusinessRuleException;
import org.rudahee.backend.common.exception.NotFoundException;
import org.rudahee.backend.entry.domain.Entry;
import org.rudahee.backend.entry.domain.EntryStatus;
import org.rudahee.backend.entry.domain.EntryUnitSeen;
import org.rudahee.backend.entry.repository.EntryUnitSeenRepository;
import org.rudahee.backend.entry.service.EntryService;
import org.rudahee.backend.entry.service.ProgressService;
import org.rudahee.backend.security.domain.AppUser;
import org.rudahee.backend.security.service.AuthService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.Comparator;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProgressServiceImpl implements ProgressService {

    private static final Comparator<MediaUnit> UNIT_ORDER =
            Comparator.comparingInt(MediaUnit::getSeasonNumber)
                    .thenComparingInt(MediaUnit::getUnitNumber);

    private final EntryService entryService;
    private final EntryUnitSeenRepository seenRepository;
    private final MediaUnitRepository unitRepository;
    private final AuthService authService;

    @Override
    @Transactional
    public Entry markSeen(Long entryId, Collection<Long> unitIds, Long userId) {
        Entry entry = entryService.requireById(entryId);
        AppUser user = authService.requireUser(userId);

        Set<Long> alreadySeen = new HashSet<>(seenRepository.findSeenUnitIds(entryId));
        List<MediaUnit> toMark = unitsOf(entry, unitIds).stream()
                .filter(unit -> !alreadySeen.contains(unit.getId()))
                .toList();

        // Marcar algo ya marcado no es un error: los dos podeis pulsar a la vez
        // desde dispositivos distintos. Se ignora en silencio.
        if (toMark.isEmpty()) {
            return entry;
        }

        toMark.forEach(unit -> seenRepository.save(new EntryUnitSeen(entry, unit, user)));
        entry.markStarted();
        refreshPointer(entry);
        return entry;
    }

    @Override
    @Transactional
    public Entry markUpTo(Long entryId, Long unitId, Long userId) {
        Entry entry = entryService.requireById(entryId);
        MediaUnit target = requireUnitOfWork(entry, unitId);

        List<Long> upTo = allUnitsOf(entry).stream()
                .filter(unit -> UNIT_ORDER.compare(unit, target) <= 0)
                .map(MediaUnit::getId)
                .toList();

        return markSeen(entryId, upTo, userId);
    }

    @Override
    @Transactional
    public Entry unmarkSeen(Long entryId, Collection<Long> unitIds) {
        Entry entry = entryService.requireById(entryId);
        seenRepository.deleteByEntryAndUnits(entryId, unitIds);

        // Desmarcar algo de una obra terminada la reabre: si ya no esta todo
        // visto, no puede seguir en DONE.
        if (entry.getStatus() == EntryStatus.DONE) {
            entry.reopen();
        }
        refreshPointer(entry);
        return entry;
    }

    @Override
    @Transactional
    public Entry advance(Long entryId, Long userId) {
        Entry entry = entryService.requireById(entryId);

        if (!entry.getWork().isEpisodic()) {
            throw new BusinessRuleException("NOT_EPISODIC",
                    "Esta obra no tiene episodios que avanzar");
        }

        Set<Long> seen = new HashSet<>(seenRepository.findSeenUnitIds(entryId));
        MediaUnit next = allUnitsOf(entry).stream()
                .filter(unit -> !seen.contains(unit.getId()))
                .min(UNIT_ORDER)
                .orElseThrow(() -> new BusinessRuleException("ALREADY_COMPLETE",
                        "Ya habeis visto todas las unidades"));

        return markSeen(entryId, List.of(next.getId()), userId);
    }

    @Override
    @Transactional
    public Entry savePosition(Long entryId, Integer positionSeconds) {
        Entry entry = entryService.requireById(entryId);
        entry.setPositionSeconds(positionSeconds);
        return entry;
    }

    @Override
    public ProgressSummary summaryOf(Long entryId) {
        Entry entry = entryService.requireById(entryId);
        long seenCount = seenRepository.countByEntryId(entryId);
        long total = unitRepository.countByWorkId(entry.getWork().getId());

        return new ProgressSummary(
                seenCount,
                total == 0 ? entry.getWork().getTotalUnits() : (int) total,
                entry.getCurrentUnit() == null ? null : entry.getCurrentUnit().getId(),
                entry.getPositionSeconds(),
                total > 0 && seenCount >= total);
    }

    /**
     * Recalcula el puntero y el estado despues de cada cambio.
     *
     * El puntero es la ultima unidad vista en orden, no la ultima marcada en el
     * tiempo: si marcais el episodio 3 despues del 5 porque se os habia
     * olvidado, "continuar" debe seguir llevando al 6.
     */
    private void refreshPointer(Entry entry) {
        Set<Long> seen = new HashSet<>(seenRepository.findSeenUnitIds(entry.getId()));
        List<MediaUnit> units = allUnitsOf(entry);

        units.stream()
                .filter(unit -> seen.contains(unit.getId()))
                .max(UNIT_ORDER)
                .ifPresentOrElse(entry::setCurrentUnit, () -> entry.setCurrentUnit(null));

        if (!units.isEmpty() && seen.size() >= units.size()) {
            entry.markFinished();
            log.info("Entrada {} completada", entry.getId());
        }
    }

    private List<MediaUnit> allUnitsOf(Entry entry) {
        return unitRepository.findByWorkIdOrderBySeasonNumberAscUnitNumberAsc(
                entry.getWork().getId());
    }

    private List<MediaUnit> unitsOf(Entry entry, Collection<Long> unitIds) {
        Long workId = entry.getWork().getId();
        return unitRepository.findAllById(unitIds).stream()
                // Nunca marcar unidades de otra obra: sin esta comprobacion,
                // un id manipulado ensuciaria el progreso de cualquier lista.
                .filter(unit -> unit.getWork().getId().equals(workId))
                .toList();
    }

    private MediaUnit requireUnitOfWork(Entry entry, Long unitId) {
        return unitsOf(entry, List.of(unitId)).stream()
                .findFirst()
                .orElseThrow(() -> NotFoundException.of("Unidad de esta obra", unitId));
    }
}
