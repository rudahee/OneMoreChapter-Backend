package org.rudahee.backend.space.repository;

import org.rudahee.backend.space.domain.SpaceMember;
import org.rudahee.backend.space.domain.SpaceRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpaceMemberRepository extends JpaRepository<SpaceMember, Long> {

    /**
     * Consulta que respalda toda la autorizacion. Devuelve solo el rol y no la
     * entidad: se ejecuta en cada peticion sobre un espacio, asi que no debe
     * arrastrar el grafo entero.
     */
    @Query("""
            SELECT m.role FROM SpaceMember m
            WHERE m.space.id = :spaceId AND m.user.id = :userId
            """)
    Optional<SpaceRole> findRole(
            @Param("spaceId") Long spaceId, @Param("userId") Long userId);

    @Query("""
            SELECT m FROM SpaceMember m
            JOIN FETCH m.user
            WHERE m.space.id = :spaceId
            ORDER BY m.joinedAt
            """)
    List<SpaceMember> findAllBySpaceWithUser(@Param("spaceId") Long spaceId);

    Optional<SpaceMember> findBySpaceIdAndUserId(Long spaceId, Long userId);

    long countBySpaceId(Long spaceId);

    long countBySpaceIdAndRole(Long spaceId, SpaceRole role);
}
