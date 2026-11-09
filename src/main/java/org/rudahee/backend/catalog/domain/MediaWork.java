package org.rudahee.backend.catalog.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.rudahee.backend.common.domain.Auditable;
import org.rudahee.backend.common.util.Texts;
import org.rudahee.backend.metadata.domain.WorkStatus;
import org.rudahee.backend.metadata.domain.WorkType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Obra del catalogo canonico. Compartida entre espacios: si otra pareja anyade
 * la misma serie, reutiliza esta fila.
 *
 * Reutiliza {@code WorkType} y {@code WorkStatus} de {@code metadata} en lugar
 * de declarar enums espejo. Un enum espejo obliga a mantener dos listas que
 * acaban divergiendo, y la regla de dependencias ya permite que
 * {@code catalog} conozca {@code metadata} (nunca al reves).
 */
@Getter
@Entity
@Table(
        name = "media_work",
        indexes = {
                @Index(name = "idx_media_work_normalized_title", columnList = "normalized_title"),
                @Index(name = "idx_media_work_type_status", columnList = "type, status")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MediaWork extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private WorkType type;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    /**
     * Titulo normalizado para la busqueda local. Se mantiene en columna propia
     * y no se calcula al vuelo porque una funcion sobre la columna en el WHERE
     * impide usar el indice.
     */
    @Column(name = "normalized_title", nullable = false, length = 500)
    private String normalizedTitle;

    @Setter
    @Column(name = "original_title", length = 500)
    private String originalTitle;

    /** {@code year} es palabra reservada en MariaDB; la columna se llama distinto. */
    @Setter
    @Column(name = "release_year")
    private Integer releaseYear;

    @Setter
    @Column(name = "cover_url", length = 1000)
    private String coverUrl;

    @Column(name = "episodic", nullable = false)
    private boolean episodic;

    @Setter
    @Column(name = "total_units")
    private Integer totalUnits;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WorkStatus status;

    @Setter
    @Column(name = "units_synced_at")
    private LocalDate unitsSyncedAt;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "media_work_external_id",
            joinColumns = @JoinColumn(name = "work_id",
                    foreignKey = @ForeignKey(name = "fk_external_id_work")),
            uniqueConstraints = @UniqueConstraint(
                    name = "uk_external_id_source_value",
                    columnNames = {"source", "external_id"}))
    private Set<ExternalId> externalIds = new LinkedHashSet<>();

    /**
     * Sin {@code orphanRemoval}: las unidades estan referenciadas por el
     * progreso del espacio. Que un refresco de metadatos borre en cascada un
     * episodio ya marcado como visto seria una perdida de datos silenciosa.
     * Las unidades solo se anyaden o se actualizan, nunca se eliminan aqui.
     */
    @OneToMany(mappedBy = "work", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @OrderBy("seasonNumber ASC, unitNumber ASC")
    private List<MediaUnit> units = new ArrayList<>();

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    public MediaWork(WorkType type, String title, boolean episodic, WorkStatus status) {
        this.type = type;
        setTitle(title);
        this.episodic = episodic;
        this.status = status == null ? WorkStatus.UNKNOWN : status;
    }

    /** El titulo y su forma normalizada se mueven siempre juntos. */
    public void setTitle(String title) {
        this.title = title;
        this.normalizedTitle = Texts.normalize(title);
    }

    public void addExternalId(String source, String externalId) {
        if (source != null && externalId != null) {
            this.externalIds.add(ExternalId.of(source, externalId));
        }
    }

    public Optional<String> externalIdFor(String source) {
        return externalIds.stream()
                .filter(id -> id.getSource().equals(source))
                .map(ExternalId::getExternalId)
                .findFirst();
    }

    /**
     * Anyade una unidad si no existe ya. Devuelve {@code true} si se anyadio,
     * para poder informar de cuantos episodios nuevos trajo un refresco.
     */
    public boolean addUnitIfAbsent(int seasonNumber, int unitNumber, String title,
                                   LocalDate airDate, Integer runtimeMinutes) {
        boolean exists = units.stream().anyMatch(unit ->
                unit.getSeasonNumber() == seasonNumber && unit.getUnitNumber() == unitNumber);
        if (exists) {
            return false;
        }
        units.add(new MediaUnit(this, seasonNumber, unitNumber, title, airDate, runtimeMinutes));
        return true;
    }

    public void markEpisodic(boolean value) {
        this.episodic = value;
    }
}
