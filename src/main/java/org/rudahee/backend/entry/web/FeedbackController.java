package org.rudahee.backend.entry.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.rudahee.backend.entry.service.FeedbackService;
import org.rudahee.backend.entry.web.dto.NoteRequest;
import org.rudahee.backend.entry.web.dto.NoteResponse;
import org.rudahee.backend.entry.web.dto.RateRequest;
import org.rudahee.backend.entry.web.dto.RatingResponse;
import org.rudahee.backend.security.context.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/spaces/{spaceId}/entries/{entryId}")
@PreAuthorize("@spaceAccess.isMember(#spaceId)")
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final CurrentUser currentUser;

    /** PUT y no POST: cada persona tiene una sola nota, y volver a puntuar la sustituye. */
    @PutMapping("/rating")
    public RatingResponse rate(@PathVariable Long spaceId,
                               @PathVariable Long entryId,
                               @Valid @RequestBody RateRequest request) {
        return RatingResponse.from(feedbackService.rate(entryId, currentUser.requireId(),
                request.score(), request.review()));
    }

    @GetMapping("/ratings")
    public List<RatingResponse> ratings(@PathVariable Long spaceId, @PathVariable Long entryId) {
        return feedbackService.ratingsOf(entryId).stream().map(RatingResponse::from).toList();
    }

    @DeleteMapping("/rating")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeRating(@PathVariable Long spaceId, @PathVariable Long entryId) {
        feedbackService.removeRating(entryId, currentUser.requireId());
    }

    /** @param unitId filtra las notas de una unidad concreta; null para todas */
    @GetMapping("/notes")
    public List<NoteResponse> notes(@PathVariable Long spaceId,
                                    @PathVariable Long entryId,
                                    @RequestParam(required = false) Long unitId) {
        return feedbackService.notesOf(entryId, unitId).stream()
                .map(NoteResponse::from).toList();
    }

    @PostMapping("/notes")
    @ResponseStatus(HttpStatus.CREATED)
    public NoteResponse addNote(@PathVariable Long spaceId,
                                @PathVariable Long entryId,
                                @Valid @RequestBody NoteRequest request) {
        return NoteResponse.from(feedbackService.addNote(entryId, request.unitId(),
                request.timestampSeconds(), currentUser.requireId(), request.body()));
    }

    @PatchMapping("/notes/{noteId}")
    public NoteResponse editNote(@PathVariable Long spaceId,
                                 @PathVariable Long entryId,
                                 @PathVariable Long noteId,
                                 @Valid @RequestBody NoteRequest request) {
        return NoteResponse.from(
                feedbackService.editNote(noteId, currentUser.requireId(), request.body()));
    }

    @DeleteMapping("/notes/{noteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeNote(@PathVariable Long spaceId,
                           @PathVariable Long entryId,
                           @PathVariable Long noteId) {
        feedbackService.removeNote(noteId, currentUser.requireId());
    }
}
