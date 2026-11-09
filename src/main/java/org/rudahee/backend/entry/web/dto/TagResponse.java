package org.rudahee.backend.entry.web.dto;

import org.rudahee.backend.entry.domain.Tag;

public record TagResponse(Long id, String name) {

    public static TagResponse from(Tag tag) {
        return new TagResponse(tag.getId(), tag.getName());
    }
}
