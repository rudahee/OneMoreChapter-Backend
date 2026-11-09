package org.rudahee.backend.space.repository;

import org.rudahee.backend.space.domain.Invitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    @Query("SELECT i FROM Invitation i JOIN FETCH i.space WHERE i.codeHash = :hash")
    Optional<Invitation> findByCodeHash(@Param("hash") String hash);

    @Query("""
            SELECT i FROM Invitation i
            WHERE i.space.id = :spaceId
              AND i.redeemedAt IS NULL
              AND i.revokedAt IS NULL
              AND i.expiresAt > :now
            ORDER BY i.createdAt DESC
            """)
    List<Invitation> findPendingBySpace(@Param("spaceId") Long spaceId,
                                        @Param("now") Instant now);

    @Modifying
    @Query("DELETE FROM Invitation i WHERE i.expiresAt < :before AND i.redeemedAt IS NULL")
    int deleteExpiredBefore(@Param("before") Instant before);
}
