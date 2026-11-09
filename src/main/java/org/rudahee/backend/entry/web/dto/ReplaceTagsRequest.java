package org.rudahee.backend.entry.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * Sustituye el juego completo de etiquetas. Las que no existen se crean solas.
 * Una lista vacia quita todas.
 */
public record ReplaceTagsRequest(
        @NotNull @Size(max = 20) Set<@Size(min = 1, max = 60) String> tags) {
}
