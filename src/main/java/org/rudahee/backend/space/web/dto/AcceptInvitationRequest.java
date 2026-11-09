package org.rudahee.backend.space.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Alta de cuenta. Es el unico DTO que crea usuarios, y vive aqui y no en
 * {@code security} porque el endpoint es de invitaciones: {@code security} no
 * sabe que las invitaciones existen.
 */
public record AcceptInvitationRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 12, max = 200) String password,
        @NotBlank @Size(min = 2, max = 60) String displayName) {
}