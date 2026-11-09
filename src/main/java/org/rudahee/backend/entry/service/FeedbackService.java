package org.rudahee.backend.entry.service;

import org.rudahee.backend.entry.domain.Note;
import org.rudahee.backend.entry.domain.Rating;

import java.util.List;

/**
 * Valoraciones y notas: lo que cada persona opina de una obra.
 *
 * Van juntos porque comparten la misma regla de propiedad (solo su autor los
 * edita) y siempre se consultan a la vez al abrir una ficha.
 */
public interface FeedbackService {

    Rating rate(Long entryId, Long userId, int score, String review);

    List<Rating> ratingsOf(Long entryId);

    void removeRating(Long entryId, Long userId);

    Note addNote(Long entryId, Long unitId, Integer timestampSeconds,
                 Long authorId, String body);

    List<Note> notesOf(Long entryId, Long unitId);

    Note editNote(Long noteId, Long userId, String body);

    void removeNote(Long noteId, Long userId);
}
