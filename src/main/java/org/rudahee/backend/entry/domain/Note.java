package org.rudahee.backend.entry.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.rudahee.backend.catalog.domain.MediaUnit;
import org.rudahee.backend.common.domain.Auditable;
import org.rudahee.backend.security.domain.AppUser;

/**
 * Comentario anclado a un punto concreto de la obra.
 *
 * Puede colgar de la obra entera, de una unidad ({@code unit}) o de un
 * instante dentro de ella ({@code timestampSeconds}). Para un club de lectura
 * que comenta el capitulo 112, esto es la herramienta de trabajo.
 */
@Getter
@Entity
@Table(
        name = "note",
        indexes = {
                @Index(name = "idx_note_entry", columnList = "entry_id"),
                @Index(name = "idx_note_unit", columnList = "unit_id")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Note extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entry_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_note_entry"))
    private Entry entry;

    /** null si la nota es sobre la obra en general. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id", foreignKey = @ForeignKey(name = "fk_note_unit"))
    private MediaUnit unit;

    /** Segundo dentro de la unidad. null si la nota no apunta a un instante. */
    @Setter
    @Column(name = "timestamp_seconds")
    private Integer timestampSeconds;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_note_author"))
    private AppUser author;

    @Setter
    @Column(name = "body", nullable = false, length = 4000)
    private String body;

    public Note(Entry entry, MediaUnit unit, Integer timestampSeconds,
                AppUser author, String body) {
        this.entry = entry;
        this.unit = unit;
        this.timestampSeconds = timestampSeconds;
        this.author = author;
        this.body = body;
    }
}
