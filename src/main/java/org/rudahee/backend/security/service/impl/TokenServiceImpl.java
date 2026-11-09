package org.rudahee.backend.security.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rudahee.backend.security.config.SecurityProperties;
import org.rudahee.backend.security.domain.AppUser;
import org.rudahee.backend.security.domain.RefreshToken;
import org.rudahee.backend.security.repository.RefreshTokenRepository;
import org.rudahee.backend.security.service.TokenService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private static final int REFRESH_TOKEN_BYTES = 32;

    private final JwtEncoder jwtEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SecurityProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public TokenPair issue(AppUser user) {
        return issue(user, RefreshToken.newFamilyId());
    }

    private TokenPair issue(AppUser user, String familyId) {
        Instant now = Instant.now();
        Instant accessExpiry = now.plus(properties.accessTokenTtl());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                // El subject es el id numerico y no el correo: si algun dia se
                // permite cambiar de correo, los tokens emitidos siguen valiendo.
                .subject(String.valueOf(user.getId()))
                .issuedAt(now)
                .expiresAt(accessExpiry)
                .claim("name", user.getDisplayName())
                .build();

        String accessToken = jwtEncoder.encode(
                JwtEncoderParameters.from(JwsHeader.with(() -> "HS256").build(), claims))
                .getTokenValue();

        String rawRefresh = generateRefreshToken();
        refreshTokenRepository.save(new RefreshToken(user, hash(rawRefresh), familyId,
                now.plus(properties.refreshTokenTtl())));

        return new TokenPair(accessToken, rawRefresh, properties.accessTokenTtl().toSeconds());
    }

    /**
     * Rotacion con deteccion de reutilizacion.
     *
     * Cada refresco invalida el token usado y emite otro de la misma familia.
     * Si llega uno ya revocado, alguien esta usando una copia antigua: puede
     * ser una condicion de carrera del cliente, pero tambien un token robado.
     * Ante la duda, cae la familia entera y toca volver a iniciar sesion.
     */
    @Override
    @Transactional
    public TokenPair rotate(String rawRefreshToken) {
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
                .orElseThrow(() -> new BadCredentialsException("Token de refresco no valido"));

        if (stored.isRevoked()) {
            log.warn("Reutilizacion de token de refresco del usuario {}; revocando familia {}",
                    stored.getUser().getId(), stored.getFamilyId());
            refreshTokenRepository.revokeFamily(stored.getFamilyId(), Instant.now());
            throw new BadCredentialsException("Token de refresco ya utilizado");
        }
        if (stored.isExpired()) {
            throw new BadCredentialsException("Token de refresco caducado");
        }
        if (!stored.getUser().isEnabled()) {
            throw new BadCredentialsException("Cuenta deshabilitada");
        }

        stored.revoke();
        return issue(stored.getUser(), stored.getFamilyId());
    }

    @Override
    @Transactional
    public void revoke(String rawRefreshToken) {
        // Cerrar sesion nunca falla: si el token no existe o ya estaba
        // revocado, el resultado deseado ya se cumple.
        refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
                .ifPresent(RefreshToken::revoke);
    }

    @Override
    @Transactional
    public void revokeAllSessions(Long userId) {
        int revoked = refreshTokenRepository.revokeAllForUser(userId, Instant.now());
        log.info("Revocadas {} sesiones del usuario {}", revoked, userId);
    }

    private String generateRefreshToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * SHA-256 sin sal ni coste: el token ya son 256 bits aleatorios, asi que no
     * es atacable por fuerza bruta ni por diccionario. Un hash lento aqui solo
     * anyadiria latencia a cada refresco.
     */
    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible en esta JVM", ex);
        }
    }
}
