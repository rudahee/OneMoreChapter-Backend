package org.rudahee.backend.entry.web.dto;

import jakarta.validation.constraints.Min;

/** @param positionSeconds null para olvidar la posicion guardada */
public record SavePositionRequest(@Min(0) Integer positionSeconds) {
}
