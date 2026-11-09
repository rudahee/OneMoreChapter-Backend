package org.rudahee.backend.entry.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.rudahee.backend.common.web.PageResponse;
import org.rudahee.backend.entry.domain.Entry;
import org.rudahee.backend.entry.domain.EntryStatus;
import org.rudahee.backend.entry.repository.EntryUnitSeenRepository;
import org.rudahee.backend.entry.service.EntryService;
import org.rudahee.backend.entry.service.FeedbackService;
import org.rudahee.backend.entry.service.ProgressService;
import org.rudahee.backend.entry.web.dto.*;
import org.rudahee.backend.metadata.domain.WorkType;
import org.rudahee.backend.security.context.CurrentUser;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;


/**
 * Todas las rutas cuelgan del espacio y llevan
 * {@code @PreAuthorize("@spaceAccess.isMember(#spaceId)")}: es lo que impide
 * que alguien lea la lista de otra pareja cambiando un id en la URL.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/spaces/{spaceId}/entries")
@PreAuthorize("@spaceAccess.isMember(#spaceId)")
public class EntryController {

    private final EntryService entryService;
    private final ProgressService progressService;
    private final FeedbackService feedbackService;
    private final EntryUnitSeenRepository seenRepository;
    private final CurrentUser currentUser;

    @GetMapping
    public PageResponse<EntrySummaryResponse> search(
            @PathVariable Long spaceId,
            @RequestParam(required = false) EntryStatus status,
            @RequestParam(required = false) WorkType type,
            @RequestParam(required = false) Long tagId,
            @PageableDefault(size = 30) Pageable pageable) {

        return PageResponse.from(
                entryService.search(spaceId, status, type, tagId, pageable),
                EntrySummaryResponse::from);
    }

    @PostMapping
    public ResponseEntity<EntrySummaryResponse> add(@PathVariable Long spaceId,
                                                    @Valid @RequestBody AddEntryRequest request,
                                                    UriComponentsBuilder uriBuilder) {
        Long userId = currentUser.requireId();
        Entry entry = request.isFromCatalog()
                ? entryService.add(spaceId, request.workId(), userId)
                : entryService.addFromProvider(spaceId, request.source(),
                        request.externalId(), request.type(), userId);

        return ResponseEntity
                .created(uriBuilder.path("/api/spaces/{spaceId}/entries/{id}")
                        .buildAndExpand(spaceId, entry.getId()).toUri())
                .body(EntrySummaryResponse.from(entry));
    }

    @GetMapping("/{entryId}")
    public EntryResponse findOne(@PathVariable Long spaceId, @PathVariable Long entryId) {
        Entry entry = entryService.requireById(entryId);
        return EntryResponse.from(entry,
                ProgressResponse.from(progressService.summaryOf(entryId)),
                seenRepository.findSeenUnitIds(entryId),
                feedbackService.ratingsOf(entryId).stream().map(RatingResponse::from).toList());
    }

    @PatchMapping("/{entryId}")
    public EntrySummaryResponse update(@PathVariable Long spaceId,
                                       @PathVariable Long entryId,
                                       @Valid @RequestBody UpdateEntryRequest request) {
        Entry entry = entryService.requireById(entryId);
        if (request.status() != null) {
            entry = entryService.changeStatus(entryId, request.status());
        }
        if (request.priority() != null) {
            entry = entryService.setPriority(entryId, request.priority());
        }
        return EntrySummaryResponse.from(entry);
    }

    @PutMapping("/{entryId}/tags")
    public EntrySummaryResponse replaceTags(@PathVariable Long spaceId,
                                            @PathVariable Long entryId,
                                            @Valid @RequestBody ReplaceTagsRequest request) {
        return EntrySummaryResponse.from(
                entryService.replaceTags(spaceId, entryId, request.tags()));
    }

    @DeleteMapping("/{entryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Long spaceId, @PathVariable Long entryId) {
        entryService.remove(entryId);
    }
}
