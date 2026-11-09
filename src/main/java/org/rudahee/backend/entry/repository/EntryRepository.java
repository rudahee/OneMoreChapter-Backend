package org.rudahee.backend.entry.repository;

import org.rudahee.backend.entry.domain.Entry;
import org.rudahee.backend.entry.domain.EntryStatus;
import org.rudahee.backend.metadata.domain.WorkType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EntryRepository extends JpaRepository<Entry, Long> {

    /**
     * Listado filtrado del espacio.
     *
     * El JOIN FETCH de work no es opcional: sin el, pintar veinte entradas con
     * su titulo y portada dispara veintiuna consultas.
     */
    @Query(value = """
            SELECT DISTINCT e FROM Entry e
            JOIN FETCH e.work w
            LEFT JOIN e.tags t
            WHERE e.space.id = :spaceId
              AND (:status IS NULL OR e.status = :status)
              AND (:type IS NULL OR w.type = :type)
              AND (:tagId IS NULL OR t.id = :tagId)
            """,
            countQuery = """
            SELECT COUNT(DISTINCT e) FROM Entry e
            JOIN e.work w
            LEFT JOIN e.tags t
            WHERE e.space.id = :spaceId
              AND (:status IS NULL OR e.status = :status)
              AND (:type IS NULL OR w.type = :type)
              AND (:tagId IS NULL OR t.id = :tagId)
            """)
    Page<Entry> search(@Param("spaceId") Long spaceId,
                       @Param("status") EntryStatus status,
                       @Param("type") WorkType type,
                       @Param("tagId") Long tagId,
                       Pageable pageable);

    @Query("""
            SELECT e FROM Entry e
            JOIN FETCH e.work
            LEFT JOIN FETCH e.tags
            WHERE e.id = :id
            """)
    Optional<Entry> findByIdWithWork(@Param("id") Long id);

    Optional<Entry> findBySpaceIdAndWorkId(Long spaceId, Long workId);

    boolean existsBySpaceIdAndWorkId(Long spaceId, Long workId);

    /**
     * Candidatas para la ruleta. Se traen todas las que pasan el filtro y el
     * sorteo se hace en memoria: con listas de cientos de entradas es
     * irrelevante, y hacerlo en SQL con ORDER BY RAND() obliga a MariaDB a
     * ordenar la tabla entera.
     */
    @Query("""
            SELECT e FROM Entry e
            JOIN FETCH e.work w
            WHERE e.space.id = :spaceId
              AND e.status IN :statuses
              AND (:type IS NULL OR w.type = :type)
              AND (:episodic IS NULL OR w.episodic = :episodic)
            """)
    List<Entry> findCandidates(@Param("spaceId") Long spaceId,
                               @Param("statuses") List<EntryStatus> statuses,
                               @Param("type") WorkType type,
                               @Param("episodic") Boolean episodic);

    long countBySpaceIdAndStatus(Long spaceId, EntryStatus status);
}
