package org.rudahee.backend.entry.web.dto;

import org.rudahee.backend.entry.domain.Note;

import java.time.Instant;

/**
 * @param unitId           null si la nota es sobre la obra entera
 * @param unitLabel        etiqueta legible tipo "T1E03", para no obligar al
 *                         cliente a cruzar la nota con la lista de unidades
 */
public record NoteResponse(
        Long id,
        Long unitId,
        String unitLabel,
        Integer timestampSeconds,
        Long authorId,
        String authorDisplayName,
        String body,
        Instant createdAt,
        Instant updatedAt) {

    public static NoteResponse from(Note note) {
        String label = null;
        if (note.getUnit() != null) {
            label = note.getUnit().getSeasonNumber() == 0
                    ? "#" + note.getUnit().getUnitNumber()
                    : "T%dE%02d".formatted(note.getUnit().getSeasonNumber(),
                            note.getUnit().getUnitNumber());
        }
        return new NoteResponse(note.getId(),
                note.getUnit() == null ? null : note.getUnit().getId(), label,
                note.getTimestampSeconds(), note.getAuthor().getId(),
                note.getAuthor().getDisplayName(), note.getBody(),
                note.getCreatedAt(), note.getUpdatedAt());
    }
}
