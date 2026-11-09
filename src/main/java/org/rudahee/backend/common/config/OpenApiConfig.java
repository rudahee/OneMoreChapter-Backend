package org.rudahee.backend.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentacion de la API con springdoc.
 *
 * Solo declara el esquema de seguridad, que es lo unico que springdoc no puede
 * deducir por si solo; sin esto el "Authorize" de Swagger UI no aparece y la
 * documentacion queda inservible para probar endpoints autenticados.
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    OpenAPI watchlistOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Watchlist API")
                        .version("v1")
                        .description("Listas compartidas de cine, series, manga y musica."))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
