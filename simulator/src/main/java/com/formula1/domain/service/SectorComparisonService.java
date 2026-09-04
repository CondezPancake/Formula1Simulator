package com.formula1.domain.service;

import com.formula1.domain.model.LapResult;
import com.formula1.domain.model.TrackSector;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Reglas de comparación de HU-33, independientes de la interfaz JavaFX. */
public final class SectorComparisonService {

    /**
     * Obtiene el piloto más rápido de un sector. Las vueltas incompletas o
     * inválidas no pueden ganar un parcial.
     */
    public Optional<LapResult> mejorEn(List<LapResult> results, TrackSector sector) {
        if (sector == null || sector == TrackSector.NONE) {
            throw new IllegalArgumentException("Debe indicarse un sector comparable");
        }
        if (results == null) {
            return Optional.empty();
        }
        return results.stream()
                .filter(LapResult::isVueltaValida)
                .filter(LapResult::hasSectorTimes)
                .min(Comparator.comparingDouble(
                        result -> result.getSectorTimes().tiempoDe(sector)));
    }
}
