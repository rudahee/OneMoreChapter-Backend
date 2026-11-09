package org.rudahee.backend.space.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.rudahee.backend.security.context.CurrentUser;
import org.rudahee.backend.space.service.SpaceService;
import org.rudahee.backend.space.web.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/spaces")
public class SpaceController {

    private final SpaceService spaceService;
    private final CurrentUser currentUser;

    /** Sin comprobacion de espacio: devuelve solo los del usuario autenticado. */
    @GetMapping
    public List<SpaceResponse> findMine() {
        return spaceService.findMine(currentUser.requireId()).stream()
                .map(SpaceResponse::from)
                .toList();
    }

    @PostMapping
    public ResponseEntity<SpaceResponse> create(@Valid @RequestBody CreateSpaceRequest request,
                                                UriComponentsBuilder uriBuilder) {
        var space = spaceService.create(request.name(), request.maxMembers(),
                currentUser.requireId());

        return ResponseEntity
                .created(uriBuilder.path("/api/spaces/{id}")
                        .buildAndExpand(space.getId()).toUri())
                .body(SpaceResponse.from(space));
    }

    @GetMapping("/{spaceId}")
    @PreAuthorize("@spaceAccess.isMember(#spaceId)")
    public SpaceResponse findOne(@PathVariable Long spaceId) {
        return SpaceResponse.from(spaceService.requireById(spaceId));
    }

    @PatchMapping("/{spaceId}")
    @PreAuthorize("@spaceAccess.canManage(#spaceId)")
    public SpaceResponse rename(@PathVariable Long spaceId,
                                @Valid @RequestBody RenameSpaceRequest request) {
        return SpaceResponse.from(spaceService.rename(spaceId, request.name()));
    }

    @GetMapping("/{spaceId}/members")
    @PreAuthorize("@spaceAccess.isMember(#spaceId)")
    public List<SpaceMemberResponse> members(@PathVariable Long spaceId) {
        return spaceService.members(spaceId).stream()
                .map(SpaceMemberResponse::from)
                .toList();
    }

    @DeleteMapping("/{spaceId}/members/{userId}")
    @PreAuthorize("@spaceAccess.canManage(#spaceId)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable Long spaceId, @PathVariable Long userId) {
        spaceService.removeMember(spaceId, userId);
    }
}
