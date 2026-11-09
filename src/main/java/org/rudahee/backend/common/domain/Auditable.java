package org.rudahee.backend.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Marcas de tiempo automaticas para cualquier entidad persistente.
 *
 * Deliberadamente no incluye {@code createdBy}: obtener el usuario actual
 * obligaria a que {@code common} dependiese de {@code security}, invirtiendo la
 * regla de dependencias. Donde la autoria importa como dato de negocio
 * ({@code Entry.addedBy}, {@code EntryUnitSeen.markedBy}) se declara de forma
 * explicita en la entidad, que ademas deja mas claro que ahi si significa algo.
 *
 * Se usa {@link Instant} y no {@code LocalDateTime}: son instantes absolutos y
 * la presentacion en horario local es cosa del cliente.
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class Auditable {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
