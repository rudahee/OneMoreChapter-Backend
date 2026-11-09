package org.rudahee.backend.catalog.repository;

import org.rudahee.backend.catalog.domain.MediaUnit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MediaUnitRepository extends JpaRepository<MediaUnit, Long> {

    List<MediaUnit> findByWorkIdOrderBySeasonNumberAscUnitNumberAsc(Long workId);

    Optional<MediaUnit> findByWorkIdAndSeasonNumberAndUnitNumber(Long workId,
                                                                 int seasonNumber,
                                                                 int unitNumber);

    long countByWorkId(Long workId);
}
