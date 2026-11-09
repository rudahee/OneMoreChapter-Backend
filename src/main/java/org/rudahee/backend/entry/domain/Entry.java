package org.rudahee.backend.entry.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.rudahee.backend.catalog.domain.MediaUnit;
import org.rudahee.backend.catalog.domain.MediaWork;
import org.rudahee.backend.common.domain.Auditable;
import org.rudahee.backend.security.domain.AppUser;
import org.rudahee.backend.space.domain.Space;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Una obra dentro de un espacio.
 *
 * El progreso es compartido: no hay una fila por persona. {@code currentUnit}
 * es el puntero de "por donde ibamos" y {@code seenUnits} la verdad completa,
 * porque no siempre se ve en orden (un OVA despues, un relleno saltado).
 */
@Getter
@Entity
@Table(
        name = "entry",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_entry_space_work", columnNames = {"space_id", "work_id"}),
        indexes = {
                @Index(name = "idx_entry_space_status", columnList = "space_id, status"),
                @Index(name = "idx_entry_work", columnList = "work_id")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Entry extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "space_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_entry_space"))
    private Space space;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_entry_work"))
    private MediaWork work;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private EntryStatus status = EntryStatus.BACKLOG;

    /** Ultima unidad marcada. Es el "continuar por aqui" de la interfaz. */
    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_unit_id",
            foreignKey = @ForeignKey(name = "fk_entry_current_unit"))
    private MediaUnit currentUnit;

    /** Minuto exacto donde se dejo, para retomar a mitad de episodio. */
    @Setter
    @Column(name = "position_seconds")
    private Integer positionSeconds;

    /** 0 normal, mayor mas ganas. Pondera la ruleta. */
    @Setter
    @Column(name = "priority", nullable = false)
    private int priority;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "added_by", nullable = false,
            foreignKey = @ForeignKey(name = "fk_entry_added_by"))
    private AppUser addedBy;

    @Setter
    @Column(name = "started_at")
    private Instant startedAt;

    @Setter
    @Column(name = "finished_at")
    private Instant finishedAt;

    /**
     * Estas si son propiedad de la entrada: si borrais la serie de la lista,
     * el progreso se va con ella. Distinto de las unidades del catalogo, que
     * son compartidas y nunca se borran en cascada.
     */
    @OneToMany(mappedBy = "entry", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<EntryUnitSeen> seenUnits = new LinkedHashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "entry_tag",
            joinColumns = @JoinColumn(name = "entry_id",
                    foreignKey = @ForeignKey(name = "fk_entry_tag_entry")),
            inverseJoinColumns = @JoinColumn(name = "tag_id",
                    foreignKey = @ForeignKey(name = "fk_entry_tag_tag")))
    private Set<Tag> tags = new LinkedHashSet<>();

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    public Entry(Space space, MediaWork work, AppUser addedBy) {
        this.space = space;
        this.work = work;
        this.addedBy = addedBy;
        this.status = EntryStatus.BACKLOG;
    }

    /** Marca el inicio la primera vez, sin pisarlo si ya se habia empezado. */
    public void markStarted() {
        if (startedAt == null) {
            startedAt = Instant.now();
        }
        if (status == EntryStatus.BACKLOG || status == EntryStatus.PAUSED) {
            status = EntryStatus.IN_PROGRESS;
        }
    }

    public void markFinished() {
        this.status = EntryStatus.DONE;
        this.finishedAt = Instant.now();
        this.positionSeconds = null;
    }

    /** Al reabrir algo terminado, se limpia la fecha de fin. */
    public void reopen() {
        this.status = EntryStatus.IN_PROGRESS;
        this.finishedAt = null;
    }
}
