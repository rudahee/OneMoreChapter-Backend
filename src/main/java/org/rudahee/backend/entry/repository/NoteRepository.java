package org.rudahee.backend.entry.repository;

import org.rudahee.backend.entry.domain.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NoteRepository extends JpaRepository<Note, Long> {

    /**
     * Notas ordenadas por su punto de anclaje: primero las generales, luego por
     * unidad y dentro de cada una por instante. Es el orden en que se leen.
     */
    @Query("""
            SELECT n FROM Note n
            JOIN FETCH n.author
            LEFT JOIN FETCH n.unit u
            WHERE n.entry.id = :entryId
            ORDER BY u.seasonNumber NULLS FIRST, u.unitNumber NULLS FIRST,
                     n.timestampSeconds NULLS FIRST, n.id
            """)
    List<Note> findByEntryOrdered(@Param("entryId") Long entryId);

    @Query("""
            SELECT n FROM Note n
            JOIN FETCH n.author
            WHERE n.entry.id = :entryId AND n.unit.id = :unitId
            ORDER BY n.timestampSeconds NULLS FIRST, n.id
            """)
    List<Note> findByEntryAndUnit(@Param("entryId") Long entryId,
                                  @Param("unitId") Long unitId);
}
