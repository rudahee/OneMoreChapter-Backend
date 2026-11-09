package org.rudahee.backend.metadata.integration.ratelimit;

import org.rudahee.backend.metadata.config.ProviderProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Control de acceso a un proveedor externo. Combina tres mecanismos que suelen
 * implementarse por separado y luego no encajan:
 *
 * <ol>
 *   <li>Token bucket con refill continuo, para no generar rafagas al inicio de
 *       cada periodo.</li>
 *   <li>Semaforo de concurrencia, porque algunos servicios limitan peticiones
 *       simultaneas ademas del ritmo.</li>
 *   <li>Enfriamiento global: si un hilo recibe un 429, el resto se detiene
 *       tambien. Sin esto, los demas hilos siguen golpeando una API ya
 *       penalizada y el bloqueo se prolonga solo.</li>
 * </ol>
 *
 * No es un bean: se instancia una por proveedor desde {@link ProviderGateRegistry}.
 * Es thread-safe.
 */
@Slf4j
public final class ProviderGate {

    private final String name;
    private final Bucket bucket;
    private final Semaphore concurrency;
    private final Duration defaultCooldown;
    private final AtomicReference<Instant> cooldownUntil = new AtomicReference<>(Instant.EPOCH);

    public ProviderGate(String name, ProviderProperties properties) {
        this.name = name;
        this.defaultCooldown = properties.defaultCooldown();
        this.concurrency = new Semaphore(properties.maxConcurrent(), true);
        this.bucket = Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(properties.capacity())
                        // refillGreedy reparte los tokens de forma continua. Con
                        // refillIntervally se liberarian de golpe y provocariamos
                        // justo la rafaga que queremos evitar.
                        .refillGreedy(properties.capacity(), properties.period())
                        .build())
                .build();
    }

    /**
     * Reserva un permiso. El {@link Permit} devuelto debe cerrarse siempre,
     * preferiblemente con try-with-resources.
     *
     * @throws ProviderThrottledException si el proveedor esta enfriando o no hay
     *                                    permiso dentro de {@code maxWait}
     * @throws InterruptedException       si el hilo se interrumpe esperando
     */
    public Permit acquire(Duration maxWait) throws InterruptedException {
        Instant until = cooldownUntil.get();
        Instant now = Instant.now();
        if (now.isBefore(until)) {
            throw new ProviderThrottledException(name, Duration.between(now, until));
        }
        if (!concurrency.tryAcquire(maxWait.toMillis(), TimeUnit.MILLISECONDS)) {
            throw new ProviderThrottledException(name, maxWait);
        }
        try {
            if (!bucket.asBlocking().tryConsume(1, maxWait.toNanos())) {
                concurrency.release();
                throw new ProviderThrottledException(name, maxWait);
            }
        } catch (InterruptedException ex) {
            concurrency.release();
            throw ex;
        }
        return new Permit();
    }

    /**
     * Cierra el proveedor tras un 429 y vacia el bucket, para que al reabrir no
     * salgan en tromba los tokens acumulados durante el enfriamiento.
     */
    public void penalize(Duration retryAfter) {
        Duration wait = (retryAfter == null || retryAfter.isNegative() || retryAfter.isZero())
                ? defaultCooldown
                : retryAfter;
        Instant until = Instant.now().plus(wait);
        cooldownUntil.updateAndGet(previous -> previous.isAfter(until) ? previous : until);
        bucket.tryConsumeAsMuchAsPossible();
        log.warn("Proveedor {} penalizado durante {} s tras respuesta 429", name, wait.toSeconds());
    }

    public boolean isCoolingDown() {
        return Instant.now().isBefore(cooldownUntil.get());
    }

    public String getName() {
        return name;
    }

    /** Slot de concurrencia reservado. */
    public final class Permit implements AutoCloseable {

        @Override
        public void close() {
            concurrency.release();
        }
    }
}
