package org.rudahee.backend.entry.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.rudahee.backend.entry.service.ProgressService;
import org.rudahee.backend.entry.web.dto.MarkUnitsRequest;
import org.rudahee.backend.entry.web.dto.ProgressResponse;
import org.rudahee.backend.entry.web.dto.SavePositionRequest;
import org.rudahee.backend.security.context.CurrentUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/spaces/{spaceId}/entries/{entryId}/progress")
@PreAuthorize("@spaceAccess.isMember(#spaceId)")
public class ProgressController {

    private final ProgressService progressService;
    private final CurrentUser currentUser;

    @GetMapping
    public ProgressResponse summary(@PathVariable Long spaceId, @PathVariable Long entryId) {
        return ProgressResponse.from(progressService.summaryOf(entryId));
    }

    /** El boton principal: marca la siguiente unidad sin ver. */
    @PostMapping("/advance")
    public ProgressResponse advance(@PathVariable Long spaceId, @PathVariable Long entryId) {
        progressService.advance(entryId, currentUser.requireId());
        return ProgressResponse.from(progressService.summaryOf(entryId));
    }

    @PostMapping("/seen")
    public ProgressResponse markSeen(@PathVariable Long spaceId,
                                     @PathVariable Long entryId,
                                     @Valid @RequestBody MarkUnitsRequest request) {
        Long userId = currentUser.requireId();
        if (request.upToUnitId() != null) {
            progressService.markUpTo(entryId, request.upToUnitId(), userId);
        } else {
            progressService.markSeen(entryId, request.unitIds(), userId);
        }
        return ProgressResponse.from(progressService.summaryOf(entryId));
    }

    @DeleteMapping("/seen")
    public ProgressResponse unmarkSeen(@PathVariable Long spaceId,
                                       @PathVariable Long entryId,
                                       @Valid @RequestBody MarkUnitsRequest request) {
        progressService.unmarkSeen(entryId, request.unitIds());
        return ProgressResponse.from(progressService.summaryOf(entryId));
    }

    @PutMapping("/position")
    public ProgressResponse savePosition(@PathVariable Long spaceId,
                                         @PathVariable Long entryId,
                                         @Valid @RequestBody SavePositionRequest request) {
        progressService.savePosition(entryId, request.positionSeconds());
        return ProgressResponse.from(progressService.summaryOf(entryId));
    }
}
