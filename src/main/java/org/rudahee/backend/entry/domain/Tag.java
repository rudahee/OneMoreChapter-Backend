package org.rudahee.backend.entry.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.rudahee.backend.common.util.Texts;
import org.rudahee.backend.space.domain.Space;

/**
 * Etiqueta libre dentro de un espacio: "noche de domingo", "para cuando
 * estemos cansados", "club de lectura".
 *
 * Son por espacio y no globales: son vocabulario privado de cada pareja, y
 * compartirlas entre espacios filtraria informacion de unos a otros.
 */
@Getter
@Entity
@Table(
        name = "tag",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_tag_space_slug", columnNames = {"space_id", "slug"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "space_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_tag_space"))
    private Space space;

    @Column(name = "name", nullable = false, length = 60)
    private String name;

    /** Forma normalizada. Evita que "Domingo" y "domingo" sean dos etiquetas. */
    @Column(name = "slug", nullable = false, length = 60)
    private String slug;

    public Tag(Space space, String name) {
        this.space = space;
        this.name = name;
        this.slug = Texts.normalize(name);
    }
}
