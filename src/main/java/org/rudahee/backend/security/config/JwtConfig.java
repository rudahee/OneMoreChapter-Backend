package org.rudahee.backend.security.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Firma y verificacion de JWT con HMAC.
 *
 * Simetrico y no par de claves RSA porque el mismo servicio emite y valida los
 * tokens: no hay terceros que necesiten verificar sin poder firmar. Si algun
 * dia hay mas de un servicio, esto se cambia por RSA y el resto sigue igual.
 */
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class JwtConfig {

    private final SecurityProperties properties;

    private SecretKeySpec secretKey() {
        return new SecretKeySpec(
                properties.jwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey()));
    }

    @Bean
    JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(secretKey())
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }
}
