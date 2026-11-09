package org.rudahee.backend.entry.repository;

import org.rudahee.backend.entry.domain.EntryUnitSeen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface EntryUnitSeenRepository extends JpaRepository<EntryUnitSeen, Long> {

    @Query("""
            SELECT s FROM EntryUnitSeen s
            JOIN FETCH s.unit
            WHERE s.entry.id = :entryId
            ORDER BY s.unit.seasonNumber, s.unit.unitNumber
            """)
    List<EntryUnitSeen> findByEntryWithUnit(@Param("entryId") Long entryId);

    /** Solo los ids: para calcular que falta sin traer el grafo entero. */
    @Query("SELECT s.unit.id FROM EntryUnitSeen s WHERE s.entry.id = :entryId")
    List<Long> findSeenUnitIds(@Param("entryId") Long entryId);

    long countByEntryId(Long entryId);

    @Modifying
    @Query("DELETE FROM EntryUnitSeen s WHERE s.entry.id = :entryId AND s.unit.id IN :unitIds")
    int deleteByEntryAndUnits(@Param("entryId") Long entryId,
                              @Param("unitIds") Collection<Long> unitIds);

    @Modifying
    @Query("DELETE FROM EntryUnitSeen s WHERE s.entry.id = :entryId")
    int deleteAllByEntry(@Param("entryId") Long entryId);
}
