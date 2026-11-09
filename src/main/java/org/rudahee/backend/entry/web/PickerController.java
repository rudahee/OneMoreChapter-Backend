package org.rudahee.backend.entry.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.rudahee.backend.entry.service.PickerService;
import org.rudahee.backend.entry.web.dto.EntrySummaryResponse;
import org.rudahee.backend.entry.web.dto.PickRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/spaces/{spaceId}/picker")
@PreAuthorize("@spaceAccess.isMember(#spaceId)")
public class PickerController {

    private final PickerService pickerService;

    /**
     * POST y no GET aunque no modifique nada: el resultado es distinto en cada
     * llamada, y con GET los navegadores y proxies lo cachearian.
     */
    @PostMapping
    public List<EntrySummaryResponse> pick(@PathVariable Long spaceId,
                                           @Valid @RequestBody PickRequest request) {
        return pickerService.pick(spaceId, request.toCriteria()).stream()
                .map(EntrySummaryResponse::from)
                .toList();
    }
}
