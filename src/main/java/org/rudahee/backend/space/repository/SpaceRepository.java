package org.rudahee.backend.space.repository;

import org.rudahee.backend.space.domain.Space;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpaceRepository extends JpaRepository<Space, Long> {

    /** Espacios a los que pertenece una persona. Normalmente uno. */
    @Query("""
            SELECT s FROM Space s
            JOIN s.members m
            WHERE m.user.id = :userId
            ORDER BY s.name
            """)
    List<Space> findAllByMember(@Param("userId") Long userId);

    @EntityGraph(attributePaths = {"members", "members.user"})
    @Query("SELECT s FROM Space s WHERE s.id = :id")
    Optional<Space> findByIdWithMembers(@Param("id") Long id);
}
