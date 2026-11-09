package org.rudahee.backend.catalog.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Episodio, capitulo o pista de una obra.
 *
 * Ojo con {@code seasonNumber}: es 0 y no NULL cuando la obra no tiene
 * temporadas. En MariaDB los NULL no colisionan dentro de un indice UNIQUE, asi
 * que con NULL podrias insertar dos veces el capitulo 5 del mismo manga sin que
 * la restriccion se queje. El centinela 0 evita ese agujero.
 */
@Getter
@Setter
@Entity
@Table(
        name = "media_unit",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_media_unit_work_season_number",
                columnNames = {"work_id", "season_number", "unit_number"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MediaUnit {

    /** Valor de {@code seasonNumber} para obras sin temporadas. */
    public static final int NO_SEASON = 0;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_media_unit_work"))
    private MediaWork work;

    @Column(name = "season_number", nullable = false)
    private int seasonNumber;

    @Column(name = "unit_number", nullable = false)
    private int unitNumber;

    @Column(name = "title", length = 500)
    private String title;

    @Column(name = "air_date")
    private LocalDate airDate;

    @Column(name = "runtime_minutes")
    private Integer runtimeMinutes;

    MediaUnit(MediaWork work, int seasonNumber, int unitNumber,
              String title, LocalDate airDate, Integer runtimeMinutes) {
        this.work = work;
        this.seasonNumber = seasonNumber;
        this.unitNumber = unitNumber;
        this.title = title;
        this.airDate = airDate;
        this.runtimeMinutes = runtimeMinutes;
    }

    /** Clave natural dentro de la obra, para casar unidades al refrescar. */
    public String naturalKey() {
        return seasonNumber + "-" + unitNumber;
    }
}
