package org.rudahee.backend.metadata.integration.client;

import org.rudahee.backend.metadata.config.MetadataProperties;
import org.rudahee.backend.metadata.config.ProviderProperties;
import org.rudahee.backend.metadata.integration.ratelimit.ProviderGateRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

/**
 * Construye un {@link RestClient} por proveedor: base URL propia, cabeceras
 * obligatorias y su interceptor de limites.
 *
 * Se inyecta el {@code RestClient.Builder} autoconfigurado por Spring Boot en
 * lugar de crear uno desde cero, de forma que cualquier personalizacion global
 * de la aplicacion (observabilidad, trazas, logging de peticiones) se aplique
 * tambien a estas llamadas.
 */
@Component
@RequiredArgsConstructor
public class ProviderRestClientFactory {

    private final RestClient.Builder builder;
    private final MetadataProperties properties;
    private final ProviderGateRegistry gates;

    public RestClient create(String providerKey) {
        ProviderProperties provider = properties.requireProvider(providerKey);

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());

        return builder.clone()
                .baseUrl(provider.baseUrl())
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.USER_AGENT, properties.userAgent())
                // La robot policy de Wikimedia exige gzip; al resto no le molesta.
                .defaultHeader(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate")
                .requestInterceptor(new RateLimitingInterceptor(
                        gates.get(providerKey),
                        properties.maxWaitForPermit(),
                        properties.maxRetries()))
                .build();
    }
}
