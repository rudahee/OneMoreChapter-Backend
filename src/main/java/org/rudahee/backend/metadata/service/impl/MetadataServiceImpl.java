package org.rudahee.backend.metadata.service.impl;

import org.rudahee.backend.metadata.config.MetadataProperties;
import org.rudahee.backend.metadata.domain.*;
import org.rudahee.backend.metadata.integration.MetadataProvider;
import org.rudahee.backend.metadata.integration.ratelimit.ProviderThrottledException;
import org.rudahee.backend.metadata.service.CandidateMerger;
import org.rudahee.backend.metadata.service.MetadataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MetadataServiceImpl implements MetadataService {

    public static final String SEARCH_CACHE = "metadataSearch";

    private final Map<String, MetadataProvider> providersByKey;
    private final List<MetadataProvider> providers;
    private final CandidateMerger merger;
    private final MetadataProperties properties;

    /**
     * Spring inyecta todas las implementaciones de {@link MetadataProvider}
     * registradas como beans. Anyadir un proveedor no requiere tocar esta clase.
     */
    public MetadataServiceImpl(List<MetadataProvider> providers,
                               CandidateMerger merger,
                               MetadataProperties properties) {
        this.providers = List.copyOf(providers);
        this.providersByKey = providers.stream()
                .collect(Collectors.toUnmodifiableMap(MetadataProvider::getKey, p -> p));
        this.merger = merger;
        this.properties = properties;
        log.info("Proveedores de metadatos activos: {}", providersByKey.keySet());
    }

    @Override
    @Cacheable(cacheNames = SEARCH_CACHE, key = "#type.name() + ':' + #query.toLowerCase()")
    public List<WorkCandidate> search(String query, WorkType type) {
        List<MetadataProvider> applicable = providers.stream()
                .filter(provider -> provider.supports(type))
                .toList();

        if (applicable.isEmpty()) {
            return List.of();
        }
        return merger.merge(searchInParallel(applicable, query, type));
    }

    /**
     * Hilos virtuales: son llamadas de red bloqueantes, no hay motivo para
     * ocupar hilos de plataforma ni dimensionar un pool. Un proveedor caido o
     * penalizado degrada el resultado, nunca tumba la busqueda.
     */
    private List<WorkCandidate> searchInParallel(List<MetadataProvider> applicable,
                                                 String query, WorkType type) {
        List<Callable<List<WorkCandidate>>> tasks = applicable.stream()
                .map(provider -> (Callable<List<WorkCandidate>>) () -> safeSearch(provider, query, type))
                .toList();

        List<WorkCandidate> collected = new ArrayList<>();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<List<WorkCandidate>>> futures = executor.invokeAll(
                    tasks, properties.searchTimeout().toMillis(), TimeUnit.MILLISECONDS);

            for (Future<List<WorkCandidate>> future : futures) {
                // Los cancelados son los que agotaron searchTimeout. resultNow
                // solo es seguro sobre futuros en estado SUCCESS.
                if (future.state() == Future.State.SUCCESS) {
                    collected.addAll(future.resultNow());
                }
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        return collected;
    }

    private List<WorkCandidate> safeSearch(MetadataProvider provider, String query, WorkType type) {
        try {
            return provider.search(query, type);
        } catch (ProviderThrottledException ex) {
            log.debug("Proveedor {} omitido: {}", provider.getKey(), ex.getMessage());
        } catch (RestClientException ex) {
            log.warn("Proveedor {} fallo en la busqueda", provider.getKey(), ex);
        }
        return List.of();
    }

    @Override
    public Optional<WorkDetail> resolve(String source, String externalId, WorkType type) {
        MetadataProvider provider = providersByKey.get(source);
        if (provider == null) {
            log.debug("Proveedor desconocido o desactivado: {}", source);
            return Optional.empty();
        }

        try {
            return provider.findByExternalId(externalId, type).map(detail -> attachUnits(provider, detail));
        } catch (ProviderThrottledException | RestClientException ex) {
            log.warn("No se pudo resolver {}:{}", source, externalId, ex);
            return Optional.empty();
        }
    }

    /**
     * Lista real si el proveedor la ofrece; en caso contrario, unidades
     * sinteticas numeradas a partir del total conocido.
     */
    private WorkDetail attachUnits(MetadataProvider provider, WorkDetail detail) {
        if (!detail.episodic()) {
            return detail;
        }

        List<UnitDetail> units = provider.findUnits(detail.externalId(), detail.type());
        if (units.isEmpty() && detail.totalUnits() != null && detail.totalUnits() > 0) {
            units = UnitDetail.synthesize(detail.totalUnits());
        }

        Integer total = detail.totalUnits() != null
                ? detail.totalUnits()
                : (units.isEmpty() ? null : units.size());

        return detail.withUnits(units, total);
    }
}
