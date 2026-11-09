package org.rudahee.backend.entry.service.impl;

import lombok.RequiredArgsConstructor;
import org.rudahee.backend.catalog.domain.MediaUnit;
import org.rudahee.backend.catalog.repository.MediaUnitRepository;
import org.rudahee.backend.common.exception.BusinessRuleException;
import org.rudahee.backend.common.exception.ForbiddenException;
import org.rudahee.backend.common.exception.NotFoundException;
import org.rudahee.backend.entry.domain.Entry;
import org.rudahee.backend.entry.domain.Note;
import org.rudahee.backend.entry.domain.Rating;
import org.rudahee.backend.entry.repository.NoteRepository;
import org.rudahee.backend.entry.repository.RatingRepository;
import org.rudahee.backend.entry.service.EntryService;
import org.rudahee.backend.entry.service.FeedbackService;
import org.rudahee.backend.security.service.AuthService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedbackServiceImpl implements FeedbackService {

    private final RatingRepository ratingRepository;
    private final NoteRepository noteRepository;
    private final MediaUnitRepository unitRepository;
    private final EntryService entryService;
    private final AuthService authService;

    @Override
    @Transactional
    public Rating rate(Long entryId, Long userId, int score, String review) {
        if (score < Rating.MIN_SCORE || score > Rating.MAX_SCORE) {
            throw new BusinessRuleException("SCORE_OUT_OF_RANGE",
                    "La nota debe estar entre %d y %d"
                            .formatted(Rating.MIN_SCORE, Rating.MAX_SCORE));
        }

        return ratingRepository.findByEntryIdAndUserId(entryId, userId)
                .map(existing -> {
                    existing.setScore(score);
                    existing.setReview(review);
                    return existing;
                })
                .orElseGet(() -> ratingRepository.save(new Rating(
                        entryService.requireById(entryId),
                        authService.requireUser(userId), score, review)));
    }

    @Override
    public List<Rating> ratingsOf(Long entryId) {
        return ratingRepository.findByEntryWithUser(entryId);
    }

    @Override
    @Transactional
    public void removeRating(Long entryId, Long userId) {
        ratingRepository.findByEntryIdAndUserId(entryId, userId)
                .ifPresent(ratingRepository::delete);
    }

    @Override
    @Transactional
    public Note addNote(Long entryId, Long unitId, Integer timestampSeconds,
                        Long authorId, String body) {
        Entry entry = entryService.requireById(entryId);
        MediaUnit unit = resolveUnit(entry, unitId);

        return noteRepository.save(new Note(entry, unit, timestampSeconds,
                authService.requireUser(authorId), body));
    }

    @Override
    public List<Note> notesOf(Long entryId, Long unitId) {
        return unitId == null
                ? noteRepository.findByEntryOrdered(entryId)
                : noteRepository.findByEntryAndUnit(entryId, unitId);
    }

    /**
     * Solo el autor edita su nota. Ser miembro del espacio da acceso de
     * lectura, no permiso para reescribir lo que dijo el otro.
     */
    @Override
    @Transactional
    public Note editNote(Long noteId, Long userId, String body) {
        Note note = requireOwnNote(noteId, userId);
        note.setBody(body);
        return note;
    }

    @Override
    @Transactional
    public void removeNote(Long noteId, Long userId) {
        noteRepository.delete(requireOwnNote(noteId, userId));
    }

    private Note requireOwnNote(Long noteId, Long userId) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> NotFoundException.of("Nota", noteId));
        if (!note.getAuthor().getId().equals(userId)) {
            throw new ForbiddenException("Solo puedes editar tus propias notas");
        }
        return note;
    }

    private MediaUnit resolveUnit(Entry entry, Long unitId) {
        if (unitId == null) {
            return null;
        }
        MediaUnit unit = unitRepository.findById(unitId)
                .orElseThrow(() -> NotFoundException.of("Unidad", unitId));
        if (!unit.getWork().getId().equals(entry.getWork().getId())) {
            throw new BusinessRuleException("UNIT_WORK_MISMATCH",
                    "Esa unidad no pertenece a esta obra");
        }
        return unit;
    }
}
