package org.rudahee.backend.space.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenameSpaceRequest(@NotBlank @Size(min = 2, max = 80) String name) {
}