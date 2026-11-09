package org.rudahee.backend.entry.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.rudahee.backend.common.domain.Auditable;
import org.rudahee.backend.security.domain.AppUser;

/**
 * Valoracion personal.
 *
 * A diferencia del progreso, esta si es por persona: comparar notas es media
 * gracia de llevar la lista entre dos. Una divergencia de cuatro puntos sobre
 * una pelicula es material de conversacion.
 */
@Getter
@Entity
@Table(
        name = "rating",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_rating_entry_user", columnNames = {"entry_id", "user_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Rating extends Auditable {

    public static final int MIN_SCORE = 1;
    public static final int MAX_SCORE = 10;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entry_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_rating_entry"))
    private Entry entry;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_rating_user"))
    private AppUser user;

    @Setter
    @Column(name = "score", nullable = false)
    private int score;

    @Setter
    @Column(name = "review", length = 2000)
    private String review;

    public Rating(Entry entry, AppUser user, int score, String review) {
        this.entry = entry;
        this.user = user;
        this.score = score;
        this.review = review;
    }
}
