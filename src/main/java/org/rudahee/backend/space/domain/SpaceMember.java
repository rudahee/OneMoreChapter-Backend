package org.rudahee.backend.space.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.rudahee.backend.security.domain.AppUser;

import java.time.Instant;

/**
 * Pertenencia de una persona a un espacio.
 *
 * Referencia {@link AppUser} directamente: la dependencia space -> security es
 * la permitida, y una clave ajena real evita miembros apuntando a cuentas
 * borradas.
 */
@Getter
@Entity
@Table(
        name = "space_member",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_space_member_space_user", columnNames = {"space_id", "user_id"}),
        indexes = @Index(name = "idx_space_member_user", columnList = "user_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpaceMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "space_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_space_member_space"))
    private Space space;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_space_member_user"))
    private AppUser user;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 16)
    private SpaceRole role;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    public SpaceMember(Space space, AppUser user, SpaceRole role) {
        this.space = space;
        this.user = user;
        this.role = role;
        this.joinedAt = Instant.now();
    }
}
