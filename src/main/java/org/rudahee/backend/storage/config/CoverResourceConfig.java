package org.rudahee.backend.storage.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.time.Duration;

/**
 * Sirve las portadas desde disco.
 *
 * Van sin autenticacion, y no es un descuido: una etiqueta {@code <img>} no
 * puede anyadir la cabecera Authorization, asi que protegerlas con el token
 * obligaria a descargarlas por fetch y convertirlas a blob, lo que rompe la
 * cache del navegador y multiplica la memoria en un listado largo.
 *
 * El riesgo es asumible: son carteles publicos de TMDB, AniList o Wikimedia, y
 * el nombre del fichero es el SHA-256 de la URL de origen, asi que no se pueden
 * enumerar ni revelan que hay en la lista de nadie.
 */
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class CoverResourceConfig implements WebMvcConfigurer {

    private final StorageProperties properties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Path.of(properties.basePath())
                .toAbsolutePath().normalize().toUri().toString();

        registry.addResourceHandler(properties.publicPath() + "/**")
                .addResourceLocations(location)
                // El contenido nunca cambia bajo el mismo nombre, porque el
                // nombre ES el hash. Cache agresiva sin riesgo de servir viejo.
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(365))
                        .cachePublic()
                        .immutable());
    }
}
