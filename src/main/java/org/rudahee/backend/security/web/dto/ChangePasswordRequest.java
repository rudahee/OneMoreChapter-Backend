package org.rudahee.backend.security.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        // Longitud minima y nada de exigir simbolos: las reglas de composicion
        // empujan a contrasenyas peores. La longitud es lo que aporta entropia.
        @NotBlank @Size(min = 12, max = 200) String newPassword) {
}