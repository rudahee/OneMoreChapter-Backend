package org.rudahee.backend.security.config;

import lombok.RequiredArgsConstructor;
import org.rudahee.backend.security.filter.AuthRateLimitFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Cadena de filtros.
 *
 * Escrita con el DSL de lambdas: Spring Security 7 elimino el encadenado con
 * {@code .and()}, asi que los ejemplos antiguos de internet ya no compilan.
 */
@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final SecurityProperties properties;

    @Bean
    SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        return http
                // Sin cookies de sesion y con el token en cabecera, no hay
                // vector CSRF. Si algun dia el refresh viaja en cookie, esto
                // hay que revisarlo.
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/login", "/api/auth/refresh").permitAll()
                        // Canjear invitacion crea la cuenta: es el unico camino
                        // de alta y por eso debe ser publico.
                        .requestMatchers(HttpMethod.POST, "/api/invitations/*/accept").permitAll()
                        // El apreton de manos es HTTP y no lleva token: la
                        // autenticacion ocurre despues, en el frame CONNECT de
                        // STOMP. Ver StompAuthChannelInterceptor.
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/actuator/health/**").permitAll()
                        .requestMatchers("/actuator/**").denyAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")
                            .permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .addFilterBefore(new AuthRateLimitFilter(properties),
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * Argon2id, la recomendacion actual para contrasenyas. Requiere BouncyCastle
     * en el classpath: sin esa dependencia el bean revienta al arrancar.
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    /**
     * Capacitor sirve la app desde capacitor:// o http://localhost, no desde el
     * dominio. Sin estos origenes la version movil no puede llamar a la API.
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "https://uncapitulomas.tudominio.es",
                "http://localhost:4200",
                "http://localhost",
                "capacitor://localhost"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
