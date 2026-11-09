package org.rudahee.backend.catalog.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.rudahee.backend.catalog.domain.MediaWork;
import org.rudahee.backend.catalog.service.CatalogService;
import org.rudahee.backend.catalog.web.dto.ImportWorkRequest;
import org.rudahee.backend.catalog.web.dto.MediaWorkResponse;
import org.rudahee.backend.catalog.web.dto.MediaWorkSummaryResponse;
import org.rudahee.backend.common.exception.NotFoundException;
import org.rudahee.backend.common.web.PageResponse;
import org.rudahee.backend.metadata.domain.WorkType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/catalog")
public class CatalogController {

    private final CatalogService catalogService;

    /** Busqueda en el catalogo local, no en los proveedores externos. */
    @GetMapping("/works")
    public PageResponse<MediaWorkSummaryResponse> search(
            @RequestParam @NotBlank @Size(min = 2, max = 120) String query,
            @RequestParam(required = false) WorkType type,
            @PageableDefault(size = 20) Pageable pageable) {

        return PageResponse.from(
                catalogService.search(query, type, pageable),
                MediaWorkSummaryResponse::from);
    }

    @GetMapping("/works/{id}")
    public MediaWorkResponse findOne(@PathVariable Long id) {
        return catalogService.findById(id)
                .map(MediaWorkResponse::from)
                .orElseThrow(() -> NotFoundException.of("Obra", id));
    }

    /**
     * Importa desde un proveedor. Es idempotente: si la obra ya esta, devuelve
     * la existente. Por eso responde 200 y no 201, y no falla al repetir.
     */
    @PostMapping("/works")
    public ResponseEntity<MediaWorkResponse> importWork(
            @Valid @RequestBody ImportWorkRequest request,
            UriComponentsBuilder uriBuilder) {

        MediaWork work = catalogService.findOrImport(
                request.source(), request.externalId(), request.type());

        URI location = uriBuilder.path("/api/catalog/works/{id}")
                .buildAndExpand(work.getId())
                .toUri();

        return ResponseEntity.ok()
                .location(location)
                .body(MediaWorkResponse.from(work));
    }

    /**
     * Fuerza el refresco de unidades. El trabajo programado hace esto mismo de
     * noche; el endpoint existe para cuando sabes que ha salido un episodio y
     * no quieres esperar.
     */
    @PostMapping("/works/{id}/units/sync")
    @ResponseStatus(HttpStatus.OK)
    public SyncResult syncUnits(@PathVariable Long id) {
        return new SyncResult(catalogService.syncUnits(id));
    }

    public record SyncResult(int addedUnits) { }
}
