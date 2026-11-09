package org.rudahee.backend.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

/**
 * Defensa contra SSRF.
 *
 * Las URL de portada vienen de proveedores externos, asi que son entrada no
 * confiable aunque el usuario no las escriba. Sin esta comprobacion, un
 * proveedor comprometido (o un registro DNS manipulado) podria hacer que el
 * backend pidiese http://169.254.169.254/ o cualquier servicio interno de la
 * red del VPS, y el resultado se guardaria como si fuera una imagen.
 *
 * La validacion es por IP resuelta y no por nombre de host: comprobar solo el
 * dominio se salta con un DNS que apunte a una direccion privada.
 */
@Slf4j
@Component
public class RemoteUrlValidator {

    public boolean isSafe(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return false;
        }
        try {
            URI uri = URI.create(rawUrl);

            // Solo HTTPS: en HTTP la respuesta es manipulable en transito y
            // ningun proveedor serio sirve portadas sin cifrar.
            if (!"https".equalsIgnoreCase(uri.getScheme())) {
                log.debug("Esquema no permitido en {}", rawUrl);
                return false;
            }
            if (uri.getHost() == null) {
                return false;
            }
            return isPublicAddress(InetAddress.getByName(uri.getHost()));

        } catch (IllegalArgumentException | UnknownHostException ex) {
            log.debug("URL descartada: {}", rawUrl);
            return false;
        }
    }

    private boolean isPublicAddress(InetAddress address) {
        boolean internal = address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isAnyLocalAddress()
                || address.isMulticastAddress();

        if (internal) {
            log.warn("Descartada URL que resuelve a direccion interna: {}",
                    address.getHostAddress());
        }
        return !internal;
    }
}
