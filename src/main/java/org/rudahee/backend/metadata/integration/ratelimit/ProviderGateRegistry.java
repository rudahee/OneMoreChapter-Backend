package org.rudahee.backend.metadata.integration.ratelimit;

import org.rudahee.backend.metadata.config.MetadataProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Una {@link ProviderGate} por proveedor, creada de forma perezosa. */
@Component
@RequiredArgsConstructor
public class ProviderGateRegistry {

    private final MetadataProperties properties;
    private final Map<String, ProviderGate> gates = new ConcurrentHashMap<>();

    public ProviderGate get(String providerKey) {
        return gates.computeIfAbsent(providerKey,
                key -> new ProviderGate(key, properties.requireProvider(key)));
    }

    public int priorityOf(String providerKey) {
        return properties.requireProvider(providerKey).priority();
    }
}
