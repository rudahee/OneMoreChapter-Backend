package org.rudahee.backend.entry.service;

import org.rudahee.backend.entry.domain.Entry;
import org.rudahee.backend.entry.domain.EntryStatus;
import org.rudahee.backend.entry.domain.Tag;
import org.rudahee.backend.metadata.domain.WorkType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;

public interface EntryService {

    /** Anyade una obra ya presente en el catalogo. */
    Entry add(Long spaceId, Long workId, Long userId);

    /**
     * Anyade importando del proveedor si hace falta. Ahorra el viaje extra
     * cuando la obra viene recien buscada en {@code metadata}.
     */
    Entry addFromProvider(Long spaceId, String source, String externalId,
                          WorkType type, Long userId);

    Page<Entry> search(Long spaceId, EntryStatus status, WorkType type,
                       Long tagId, Pageable pageable);

    Entry requireById(Long entryId);

    Entry changeStatus(Long entryId, EntryStatus status);

    Entry setPriority(Long entryId, int priority);

    Entry replaceTags(Long spaceId, Long entryId, Set<String> tagNames);

    List<Tag> tagsOf(Long spaceId);

    void remove(Long entryId);
}
