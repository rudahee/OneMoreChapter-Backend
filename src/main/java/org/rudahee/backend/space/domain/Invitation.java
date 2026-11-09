package org.rudahee.backend.space.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.rudahee.backend.security.domain.AppUser;

import java.time.Instant;

/**
 * Invitacion de un solo uso. Es el unico camino de alta: no hay registro
 * abierto, y eso elimina de golpe casi toda la superficie de abuso de una API
 * publica.
 *
 * Se guarda el hash del codigo, no el codigo. El endpoint de canje es publico
 * por necesidad, asi que quien leyese la tabla podria crear cuentas; con el
 * hash, no. La contrapartida es que el codigo se muestra una sola vez, al
 * crearlo, igual que una clave de API.
 */
@Getter
@Entity
@Table(
        name = "invitation",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_invitation_code_hash", columnNames = "code_hash"),
        indexes = @Index(name = "idx_invitation_space", columnList = "space_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Invitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "space_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_invitation_space"))
    private Space space;

    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "granted_role", nullable = false, length = 16)
    private SpaceRole grantedRole;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false,
            foreignKey = @ForeignKey(name = "fk_invitation_creator"))
    private AppUser createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "redeemed_at")
    private Instant redeemedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "redeemed_by",
            foreignKey = @ForeignKey(name = "fk_invitation_redeemer"))
    private AppUser redeemedBy;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    public Invitation(Space space, String codeHash, SpaceRole grantedRole,
                      AppUser createdBy, Instant expiresAt) {
        this.space = space;
        this.codeHash = codeHash;
        this.grantedRole = grantedRole;
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
        this.expiresAt = expiresAt;
    }

    public void redeem(AppUser user) {
        this.redeemedAt = Instant.now();
        this.redeemedBy = user;
    }

    public void revoke() {
        if (this.revokedAt == null) {
            this.revokedAt = Instant.now();
        }
    }

    public boolean isRedeemed() {
        return redeemedAt != null;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isUsable() {
        return redeemedAt == null && revokedAt == null && !isExpired();
    }
}
