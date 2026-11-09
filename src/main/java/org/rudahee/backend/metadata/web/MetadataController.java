package org.rudahee.backend.metadata.web;

import org.rudahee.backend.metadata.domain.WorkType;
import org.rudahee.backend.metadata.service.MetadataService;
import org.rudahee.backend.metadata.web.dto.WorkCandidateResponse;
import org.rudahee.backend.metadata.web.dto.WorkDetailResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Nada de esto debe ser accesible sin autenticar: cada peticion consume cuota
 * compartida de APIs externas limitadas por IP, la nuestra. Conviene ademas un
 * limite por usuario en la cadena de seguridad.
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/metadata")
public class MetadataController {

    private final MetadataService metadataService;

    @GetMapping("/search")
    public List<WorkCandidateResponse> search(
            @RequestParam @NotBlank @Size(min = 2, max = 120) String query,
            @RequestParam(defaultValue = "OTHER") WorkType type) {

        return metadataService.search(query.trim(), type).stream()
                .map(WorkCandidateResponse::from)
                .toList();
    }

    @GetMapping("/works/{source}/{externalId}")
    public ResponseEntity<WorkDetailResponse> findOne(
            @PathVariable String source,
            @PathVariable String externalId,
            @RequestParam(defaultValue = "OTHER") WorkType type) {

        return metadataService.resolve(source, externalId, type)
                .map(WorkDetailResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
