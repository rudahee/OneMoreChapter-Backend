package org.rudahee.backend.entry.repository;

import org.rudahee.backend.entry.domain.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    Optional<Rating> findByEntryIdAndUserId(Long entryId, Long userId);

    @Query("SELECT r FROM Rating r JOIN FETCH r.user WHERE r.entry.id = :entryId")
    List<Rating> findByEntryWithUser(@Param("entryId") Long entryId);
}
