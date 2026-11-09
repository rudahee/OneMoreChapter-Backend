package org.rudahee.backend.space.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** @param maxMembers null para no poner limite */
public record CreateSpaceRequest(
        @NotBlank @Size(min = 2, max = 80) String name,
        @Min(2) Integer maxMembers) {
}
