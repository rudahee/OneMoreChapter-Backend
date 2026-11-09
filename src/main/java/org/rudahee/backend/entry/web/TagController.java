package org.rudahee.backend.entry.web;

import lombok.RequiredArgsConstructor;
import org.rudahee.backend.entry.service.EntryService;
import org.rudahee.backend.entry.web.dto.TagResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Las etiquetas cuelgan del espacio, no de una entrada, asi que van en su
 * propio controlador y no colgando de /entries.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/spaces/{spaceId}/tags")
@PreAuthorize("@spaceAccess.isMember(#spaceId)")
public class TagController {

    private final EntryService entryService;

    @GetMapping
    public List<TagResponse> findAll(@PathVariable Long spaceId) {
        return entryService.tagsOf(spaceId).stream().map(TagResponse::from).toList();
    }
}
