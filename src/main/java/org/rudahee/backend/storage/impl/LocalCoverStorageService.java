package org.rudahee.backend.storage.impl;

import lombok.extern.slf4j.Slf4j;
import org.rudahee.backend.storage.CoverStorageService;
import org.rudahee.backend.storage.RemoteUrlValidator;
import org.rudahee.backend.storage.config.StorageProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Almacenamiento en disco local.
 *
 * Suficiente para un VPS con un volumen montado. La interfaz existe para que
 * cambiar a S3 o MinIO sea sustituir esta clase, no tocar el catalogo.
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "storage.covers", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class LocalCoverStorageService implements CoverStorageService {

    private final StorageProperties properties;
    private final RemoteUrlValidator urlValidator;
    private final HttpClient httpClient;
    private final Path baseDirectory;

    public LocalCoverStorageService(StorageProperties properties,
                                    RemoteUrlValidator urlValidator) throws IOException {
        this.properties = properties;
        this.urlValidator = urlValidator;
        this.baseDirectory = Path.of(properties.basePath()).toAbsolutePath().normalize();
        Files.createDirectories(baseDirectory);

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.timeout())
                // NEVER y no NORMAL: seguir redirecciones anularia la
                // validacion SSRF, porque el destino final no se comprueba.
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Override
    public Optional<String> store(String remoteUrl) {
        if (isLocal(remoteUrl) || !urlValidator.isSafe(remoteUrl)) {
            return Optional.empty();
        }
        try {
            return download(remoteUrl);
        } catch (IOException ex) {
            log.debug("No se pudo descargar la portada {}: {}", remoteUrl, ex.getMessage());
            return Optional.empty();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    private Optional<String> download(String remoteUrl) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(remoteUrl))
                .timeout(properties.timeout())
                .header("User-Agent", "UnCapituloMas/1.0")
                .GET()
                .build();

        HttpResponse<InputStream> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

        try (InputStream body = response.body()) {
            if (response.statusCode() != 200) {
                return Optional.empty();
            }

            List<String> contentTypes = response.headers().allValues("content-type");
            if (contentTypes.size() != 1) {
                return Optional.empty();
            }

            String value = contentTypes.getFirst();
            int semicolon = value.indexOf(';');

            String contentType = (semicolon >= 0 ? value.substring(0, semicolon) : value)
                    .trim()
                    .toLowerCase(Locale.ROOT);

            if (!properties.allowedContentTypes().contains(contentType)) {
                log.debug("Tipo no permitido ({}) en {}", contentType, remoteUrl);
                return Optional.empty();
            }

            // Se lee acotado y no con readAllBytes: la cabecera Content-Length
            // la pone el servidor remoto y puede mentir.
            byte[] bytes = body.readNBytes((int) properties.maxBytes() + 1);
            if (bytes.length > properties.maxBytes()) {
                log.debug("Portada demasiado grande en {}", remoteUrl);
                return Optional.empty();
            }

            return Optional.of(write(remoteUrl, bytes, extensionFor(contentType)));
        }
    }

    /**
     * Reparte los ficheros en subdirectorios por los dos primeros caracteres
     * del hash. Con miles de portadas en un solo directorio, cualquier
     * operacion del sistema de ficheros se vuelve lenta.
     */
    private String write(String remoteUrl, byte[] bytes, String extension) throws IOException {
        String hash = sha256(remoteUrl);
        String shard = hash.substring(0, 2);
        String fileName = hash + extension;

        Path directory = baseDirectory.resolve(shard);
        Files.createDirectories(directory);

        Path target = directory.resolve(fileName);
        Path temporary = directory.resolve(fileName + ".part");

        // Escritura en dos pasos: si el proceso muere a mitad, no queda un
        // fichero truncado que luego se sirva como imagen corrupta.
        Files.write(temporary, bytes, StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
        Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE);

        return "%s/%s/%s".formatted(properties.publicPath(), shard, fileName);
    }

    @Override
    public void delete(String publicPath) {
        if (!isLocal(publicPath)) {
            return;
        }
        try {
            Path relative = Path.of(publicPath.substring(properties.publicPath().length() + 1));
            Path target = baseDirectory.resolve(relative).normalize();

            // Sin esta comprobacion, un publicPath con ".." borraria ficheros
            // fuera del directorio de portadas.
            if (!target.startsWith(baseDirectory)) {
                log.warn("Intento de borrado fuera del directorio: {}", publicPath);
                return;
            }
            Files.deleteIfExists(target);
        } catch (IOException | IllegalArgumentException ex) {
            log.debug("No se pudo borrar {}", publicPath, ex);
        }
    }

    @Override
    public boolean isLocal(String url) {
        return url != null && url.startsWith(properties.publicPath());
    }

    private String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/avif" -> ".avif";
            default -> ".jpg";
        };
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible en esta JVM", ex);
        }
    }
}
