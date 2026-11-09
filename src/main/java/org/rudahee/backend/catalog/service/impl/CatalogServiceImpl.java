package org.rudahee.backend.catalog.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rudahee.backend.catalog.domain.MediaUnit;
import org.rudahee.backend.catalog.domain.MediaWork;
import org.rudahee.backend.catalog.repository.MediaWorkRepository;
import org.rudahee.backend.catalog.service.CatalogService;
import org.rudahee.backend.common.exception.NotFoundException;
import org.rudahee.backend.common.util.Texts;
import org.rudahee.backend.metadata.domain.UnitDetail;
import org.rudahee.backend.metadata.domain.WorkDetail;
import org.rudahee.backend.metadata.domain.WorkType;
import org.rudahee.backend.metadata.service.MetadataService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogServiceImpl implements CatalogService {

    private final MediaWorkRepository workRepository;
    private final MetadataService metadataService;

    @Override
    @Transactional
    public MediaWork findOrImport(String source, String externalId, WorkType type) {
        return workRepository.findByExternalId(source, externalId)
                .orElseGet(() -> importFromProvider(source, externalId, type));
    }

    private MediaWork importFromProvider(String source, String externalId, WorkType type) {
        WorkDetail detail = metadataService.resolve(source, externalId, type)
                .orElseThrow(() -> NotFoundException.of(
                        "Obra en el proveedor " + source, externalId));
        return importWork(detail);
    }

    @Override
    @Transactional
    public MediaWork importWork(WorkDetail detail) {
        try {
            return findExisting(detail)
                    .map(existing -> update(existing, detail))
                    .orElseGet(() -> create(detail));
        } catch (DataIntegrityViolationException ex) {
            // Los dos habeis anyadido la misma serie a la vez y la restriccion
            // unica de media_work_external_id ha frenado la segunda insercion.
            // No es un error: la obra existe, solo hay que releerla.
            log.debug("Carrera al importar {}:{}, releyendo",
                    detail.source(), detail.externalId(), ex);
            return findExisting(detail).orElseThrow(() -> ex);
        }
    }

    /**
     * Busca por cualquiera de los identificadores que trae el detalle, no solo
     * por el del proveedor de origen. Es lo que evita duplicar una obra que ya
     * entro por otra fuente.
     */
    private Optional<MediaWork> findExisting(WorkDetail detail) {
        List<String> pairs = detail.externalIds().entrySet().stream()
                .map(entry -> entry.getKey() + '=' + entry.getValue())
                .toList();
        if (pairs.isEmpty()) {
            return workRepository.findByExternalId(detail.source(), detail.externalId());
        }
        List<MediaWork> matches = workRepository.findByAnyExternalId(pairs);

        if (matches.size() > 1) {
            // Dos filas comparten identificadores: la primera vez que pasa
            // suele significar que un proveedor devolvio un id cruzado erroneo.
            log.warn("Identificadores de {} apuntan a {} obras distintas: {}",
                    detail.title(), matches.size(), matches.stream().map(MediaWork::getId).toList());
        }
        return matches.stream().findFirst();
    }

    private MediaWork create(WorkDetail detail) {
        MediaWork work = new MediaWork(detail.type(), detail.title(),
                detail.episodic(), detail.status());
        work.setOriginalTitle(detail.originalTitle());
        work.setReleaseYear(detail.year());
        work.setCoverUrl(detail.coverUrl());
        work.setTotalUnits(detail.totalUnits());
        detail.externalIds().forEach(work::addExternalId);
        work.addExternalId(detail.source(), detail.externalId());

        applyUnits(work, detail.units());

        MediaWork saved = workRepository.save(work);
        log.info("Obra importada: {} [{}] con {} unidades",
                saved.getTitle(), saved.getType(), saved.getUnits().size());
        return saved;
    }

    /**
     * Actualiza solo lo que puede cambiar con el tiempo. El titulo y el anyo se
     * refrescan porque los proveedores corrigen datos; el tipo no, porque
     * cambiarlo invalidaria las listas que ya lo referencian.
     */
    private MediaWork update(MediaWork work, WorkDetail detail) {
        if (detail.title() != null && !detail.title().equals(work.getTitle())) {
            work.setTitle(detail.title());
        }
        if (detail.coverUrl() != null) {
            work.setCoverUrl(detail.coverUrl());
        }
        if (detail.year() != null) {
            work.setReleaseYear(detail.year());
        }
        if (detail.status() != null) {
            work.setStatus(detail.status());
        }
        if (detail.totalUnits() != null) {
            work.setTotalUnits(detail.totalUnits());
        }
        detail.externalIds().forEach(work::addExternalId);
        work.addExternalId(detail.source(), detail.externalId());

        applyUnits(work, detail.units());
        return work;
    }

    /**
     * Fusion aditiva de unidades: se anyaden las que faltan y nunca se borran
     * las existentes, porque el progreso del espacio las referencia.
     *
     * Si un proveedor deja de listar un episodio (recuento corregido, capitulo
     * retirado), la unidad se queda huerfana en el catalogo. Es preferible eso
     * a que alguien pierda la marca de "visto".
     */
    private int applyUnits(MediaWork work, List<UnitDetail> units) {
        if (units.isEmpty()) {
            return 0;
        }
        int added = 0;
        for (UnitDetail unit : units) {
            int season = unit.seasonNumber() == null
                    ? MediaUnit.NO_SEASON
                    : unit.seasonNumber();
            if (work.addUnitIfAbsent(season, unit.unitNumber(), unit.title(),
                    unit.airDate(), unit.runtimeMinutes())) {
                added++;
            }
        }
        work.setUnitsSyncedAt(LocalDate.now());
        return added;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int syncUnits(Long workId) {
        MediaWork work = workRepository.findByIdWithUnits(workId)
                .orElseThrow(() -> NotFoundException.of("Obra", workId));

        if (!work.isEpisodic()) {
            return 0;
        }

        // Se reintenta con cada proveedor conocido hasta que uno responda: si
        // TVMaze esta penalizado, quiza AniList tenga la misma serie.
        for (Map.Entry<String, String> id : externalIdsOf(work)) {
            Optional<WorkDetail> detail =
                    metadataService.resolve(id.getKey(), id.getValue(), work.getType());
            if (detail.isPresent()) {
                int added = applyUnits(work, detail.get().units());
                if (detail.get().status() != null) {
                    work.setStatus(detail.get().status());
                }
                if (added > 0) {
                    log.info("{} unidades nuevas en {}", added, work.getTitle());
                }
                return added;
            }
        }
        log.debug("Ningun proveedor respondio para {}", work.getTitle());
        return 0;
    }

    private List<Map.Entry<String, String>> externalIdsOf(MediaWork work) {
        return work.getExternalIds().stream()
                .map(id -> Map.entry(id.getSource(), id.getExternalId()))
                .toList();
    }

    @Override
    public Optional<MediaWork> findById(Long id) {
        return workRepository.findByIdWithUnits(id);
    }

    @Override
    public Page<MediaWork> search(String query, WorkType type, Pageable pageable) {
        // La normalizacion se hace aqui y no en el SQL: aplicar una funcion
        // sobre la columna en el WHERE impediria usar el indice.
        String normalized = Texts.normalize(query);
        if (normalized.isBlank()) {
            return Page.empty(pageable);
        }
        return workRepository.searchByNormalizedTitle(normalized, type, pageable);
    }
}
