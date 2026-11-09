package org.rudahee.backend.security.web.dto;

import org.rudahee.backend.security.service.TokenService;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn) {

    public static TokenResponse from(TokenService.TokenPair pair) {
        return new TokenResponse(pair.accessToken(), pair.refreshToken(),
                "Bearer", pair.expiresIn());
    }
}