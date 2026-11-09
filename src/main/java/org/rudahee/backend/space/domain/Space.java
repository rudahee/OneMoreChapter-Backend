package org.rudahee.backend.space.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.rudahee.backend.common.domain.Auditable;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Espacio compartido: el contenedor de listas de un grupo de personas.
 *
 * Todo lo que hay en {@code entry} cuelga de aqui, y toda comprobacion de
 * permisos se reduce a "pertenece esta persona a este espacio".
 */
@Getter
@Entity
@Table(name = "space")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Space extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Column(name = "name", nullable = false, length = 80)
    private String name;

    /**
     * Limite de miembros. {@code null} significa sin limite.
     *
     * Se deja configurable en vez de fijar 2 en el codigo: la aplicacion es
     * para una pareja, pero nada en el modelo lo exige y cablearlo obligaria a
     * una migracion el dia que querais compartir una lista con alguien mas.
     */
    @Setter
    @Column(name = "max_members")
    private Integer maxMembers;

    @OneToMany(mappedBy = "space", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<SpaceMember> members = new LinkedHashSet<>();

    public Space(String name, Integer maxMembers) {
        this.name = name;
        this.maxMembers = maxMembers;
    }

    public boolean isFull() {
        return maxMembers != null && members.size() >= maxMembers;
    }
}
