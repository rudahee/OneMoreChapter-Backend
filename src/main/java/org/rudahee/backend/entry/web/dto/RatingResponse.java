package org.rudahee.backend.entry.web.dto;

import org.rudahee.backend.entry.domain.Rating;

import java.time.Instant;

public record RatingResponse(
        Long userId,
        String displayName,
        int score,
        String review,
        Instant updatedAt) {

    public static RatingResponse from(Rating rating) {
        return new RatingResponse(rating.getUser().getId(),
                rating.getUser().getDisplayName(), rating.getScore(),
                rating.getReview(), rating.getUpdatedAt());
    }
}
