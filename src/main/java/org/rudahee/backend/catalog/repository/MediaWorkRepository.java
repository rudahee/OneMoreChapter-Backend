package org.rudahee.backend.catalog.repository;

import org.rudahee.backend.catalog.domain.MediaWork;
import org.rudahee.backend.metadata.domain.WorkStatus;
import org.rudahee.backend.metadata.domain.WorkType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MediaWorkRepository extends JpaRepository<MediaWork, Long> {

    /**
     * Busca por un identificador externo concreto. Es el camino normal al
     * importar: antes de crear nada, se comprueba si la obra ya existe.
     */
    @Query("""
            SELECT w FROM MediaWork w
            JOIN w.externalIds e
            WHERE e.source = :source AND e.externalId = :externalId
            """)
    Optional<MediaWork> findByExternalId(@Param("source") String source,
                                         @Param("externalId") String externalId);

    /**
     * Busca por cualquiera de los identificadores conocidos.
     *
     * Importa porque el merge de {@code metadata} acumula ids de varios
     * proveedores: si la obra entro por AniList y ahora llega la misma vía
     * Wikidata, hay que reconocerla en lugar de duplicarla.
     *
     * Las parejas se pasan concatenadas ("anilist=21") para no necesitar una
     * tupla en el IN, que JPQL no admite de forma portable.
     */
    @Query("""
            SELECT DISTINCT w FROM MediaWork w
            JOIN w.externalIds e
            WHERE CONCAT(e.source, '=', e.externalId) IN :pairs
            """)
    List<MediaWork> findByAnyExternalId(@Param("pairs") Collection<String> pairs);

    /**
     * Busqueda local por titulo normalizado.
     *
     * El parametro llega ya normalizado desde el servicio: normalizar dentro
     * del SQL impediria usar el indice sobre normalized_title.
     */
    @Query("""
            SELECT w FROM MediaWork w
            WHERE w.normalizedTitle LIKE CONCAT('%', :normalizedQuery, '%')
              AND (:type IS NULL OR w.type = :type)
            """)
    Page<MediaWork> searchByNormalizedTitle(@Param("normalizedQuery") String normalizedQuery,
                                            @Param("type") WorkType type,
                                            Pageable pageable);

    /**
     * Candidatas al refresco nocturno: en emision y sin sincronizar desde una
     * fecha. Solo se tocan estas; una pelicula de 1979 no cambia.
     */
    @Query("""
            SELECT w FROM MediaWork w
            WHERE w.status = :status
              AND w.episodic = true
              AND (w.unitsSyncedAt IS NULL OR w.unitsSyncedAt < :before)
            """)
    List<MediaWork> findStaleByStatus(@Param("status") WorkStatus status,
                                      @Param("before") LocalDate before,
                                      Pageable pageable);

    /** Carga la obra con sus unidades en una consulta, evitando el N+1. */
    @EntityGraph(attributePaths = {"units", "externalIds"})
    @Query("SELECT w FROM MediaWork w WHERE w.id = :id")
    Optional<MediaWork> findByIdWithUnits(@Param("id") Long id);
}
