package org.rudahee.backend.security.web.dto;

import org.rudahee.backend.security.domain.AppUser;

import java.time.Instant;

public record CurrentUserResponse(
        Long id,
        String email,
        String displayName,
        Instant lastLoginAt) {

    public static CurrentUserResponse from(AppUser user) {
        return new CurrentUserResponse(user.getId(), user.getEmail(),
                user.getDisplayName(), user.getLastLoginAt());
    }
}