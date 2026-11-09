package org.rudahee.backend.security.service;

import org.rudahee.backend.security.domain.AppUser;

/** Emision, rotacion y revocacion de tokens. */
public interface TokenService {

    /**
     * @param accessToken  JWT firmado, corto
     * @param refreshToken token opaco en claro; solo se devuelve aqui, en
     *                     la base de datos se guarda su hash
     * @param expiresIn    segundos de validez del access token
     */
    record TokenPair(String accessToken, String refreshToken, long expiresIn) { }

    TokenPair issue(AppUser user);

    /** Rota el par. Si el token ya fue usado, invalida la familia entera. */
    TokenPair rotate(String rawRefreshToken);

    void revoke(String rawRefreshToken);

    void revokeAllSessions(Long userId);
}
