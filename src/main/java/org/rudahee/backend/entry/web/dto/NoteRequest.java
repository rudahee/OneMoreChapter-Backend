package org.rudahee.backend.entry.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param unitId           null si la nota es sobre la obra en general
 * @param timestampSeconds segundo dentro de la unidad, null si no aplica
 */
public record NoteRequest(
        Long unitId,
        @Min(0) Integer timestampSeconds,
        @NotBlank @Size(max = 4000) String body) {
}
