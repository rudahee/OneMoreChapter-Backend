package org.rudahee.backend.entry.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.rudahee.backend.catalog.domain.MediaUnit;
import org.rudahee.backend.security.domain.AppUser;

import java.time.Instant;

/**
 * Marca de unidad vista.
 *
 * El progreso es del espacio, no de cada persona: no hay {@code user_id} en la
 * clave. {@code markedBy} solo registra quien pulso el boton, para poder decir
 * "lo marcaste tu el martes", y no participa en ninguna comprobacion.
 */
@Getter
@Entity
@Table(
        name = "entry_unit_seen",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_entry_unit_seen", columnNames = {"entry_id", "unit_id"}),
        indexes = @Index(name = "idx_entry_unit_seen_entry", columnList = "entry_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EntryUnitSeen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entry_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_entry_unit_seen_entry"))
    private Entry entry;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_entry_unit_seen_unit"))
    private MediaUnit unit;

    @Column(name = "seen_at", nullable = false)
    private Instant seenAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "marked_by", nullable = false,
            foreignKey = @ForeignKey(name = "fk_entry_unit_seen_user"))
    private AppUser markedBy;

    public EntryUnitSeen(Entry entry, MediaUnit unit, AppUser markedBy) {
        this.entry = entry;
        this.unit = unit;
        this.markedBy = markedBy;
        this.seenAt = Instant.now();
    }
}
