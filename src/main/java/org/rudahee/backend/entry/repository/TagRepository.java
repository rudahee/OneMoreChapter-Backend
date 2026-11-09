package org.rudahee.backend.entry.repository;

import org.rudahee.backend.entry.domain.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findBySpaceIdAndSlug(Long spaceId, String slug);

    List<Tag> findBySpaceIdOrderByName(Long spaceId);
}
