package org.rudahee.backend.entry.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rudahee.backend.catalog.domain.MediaWork;
import org.rudahee.backend.catalog.service.CatalogService;
import org.rudahee.backend.common.exception.BusinessRuleException;
import org.rudahee.backend.common.exception.NotFoundException;
import org.rudahee.backend.common.util.Texts;
import org.rudahee.backend.entry.domain.Entry;
import org.rudahee.backend.entry.domain.EntryStatus;
import org.rudahee.backend.entry.domain.Tag;
import org.rudahee.backend.entry.repository.EntryRepository;
import org.rudahee.backend.entry.repository.TagRepository;
import org.rudahee.backend.entry.service.EntryService;
import org.rudahee.backend.metadata.domain.WorkType;
import org.rudahee.backend.security.service.AuthService;
import org.rudahee.backend.space.domain.Space;
import org.rudahee.backend.space.service.SpaceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EntryServiceImpl implements EntryService {

    private final EntryRepository entryRepository;
    private final TagRepository tagRepository;
    private final CatalogService catalogService;
    private final SpaceService spaceService;
    private final AuthService authService;

    @Override
    @Transactional
    public Entry add(Long spaceId, Long workId, Long userId) {
        MediaWork work = catalogService.findById(workId)
                .orElseThrow(() -> NotFoundException.of("Obra", workId));
        return createEntry(spaceId, work, userId);
    }

    @Override
    @Transactional
    public Entry addFromProvider(Long spaceId, String source, String externalId,
                                 WorkType type, Long userId) {
        return createEntry(spaceId, catalogService.findOrImport(source, externalId, type), userId);
    }

    private Entry createEntry(Long spaceId, MediaWork work, Long userId) {
        // Devolver la existente en vez de fallar: reanyadir algo que ya esta en
        // la lista no es un error, es que no te acordabas.
        return entryRepository.findBySpaceIdAndWorkId(spaceId, work.getId())
                .orElseGet(() -> {
                    Space space = spaceService.requireById(spaceId);
                    Entry entry = entryRepository.save(
                            new Entry(space, work, authService.requireUser(userId)));
                    log.info("Obra {} anyadida al espacio {} por el usuario {}",
                            work.getId(), spaceId, userId);
                    return entry;
                });
    }

    @Override
    public Page<Entry> search(Long spaceId, EntryStatus status, WorkType type,
                              Long tagId, Pageable pageable) {
        return entryRepository.search(spaceId, status, type, tagId, pageable);
    }

    @Override
    public Entry requireById(Long entryId) {
        return entryRepository.findByIdWithWork(entryId)
                .orElseThrow(() -> NotFoundException.of("Entrada", entryId));
    }

    @Override
    @Transactional
    public Entry changeStatus(Long entryId, EntryStatus status) {
        Entry entry = requireById(entryId);

        if (status == EntryStatus.DONE) {
            entry.markFinished();
        } else if (entry.getStatus() == EntryStatus.DONE) {
            entry.reopen();
            entry.setStatus(status);
        } else {
            entry.setStatus(status);
        }
        return entry;
    }

    @Override
    @Transactional
    public Entry setPriority(Long entryId, int priority) {
        Entry entry = requireById(entryId);
        entry.setPriority(priority);
        return entry;
    }

    /**
     * Sustituye el juego de etiquetas. Las que no existen se crean sobre la
     * marcha: obligar a darlas de alta aparte solo anyade pasos.
     */
    @Override
    @Transactional
    public Entry replaceTags(Long spaceId, Long entryId, Set<String> tagNames) {
        Entry entry = requireById(entryId);

        if (!entry.getSpace().getId().equals(spaceId)) {
            throw new BusinessRuleException("ENTRY_SPACE_MISMATCH",
                    "La entrada no pertenece a este espacio");
        }

        Set<Tag> resolved = new LinkedHashSet<>();
        for (String name : tagNames) {
            String clean = Texts.blankToNull(name);
            if (clean == null) {
                continue;
            }
            String slug = Texts.normalize(clean);
            resolved.add(tagRepository.findBySpaceIdAndSlug(spaceId, slug)
                    .orElseGet(() -> tagRepository.save(new Tag(entry.getSpace(), clean))));
        }

        entry.getTags().clear();
        entry.getTags().addAll(resolved);
        return entry;
    }

    @Override
    public List<Tag> tagsOf(Long spaceId) {
        return tagRepository.findBySpaceIdOrderByName(spaceId);
    }

    @Override
    @Transactional
    public void remove(Long entryId) {
        // La cascada se lleva el progreso, valoraciones y notas de ESTA lista.
        // El catalogo no se toca: la obra sigue existiendo para otros espacios.
        entryRepository.delete(requireById(entryId));
    }
}
