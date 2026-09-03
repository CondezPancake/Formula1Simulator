package com.formula1.service;

import com.formula1.domain.model.EventOccurrence;
import com.formula1.domain.model.SectorTimes;
import com.formula1.domain.model.TrackSector;
import com.formula1.domain.model.WeatherSnapshot;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Calcula parciales coherentes con el clima, los eventos y el total de vuelta. */
final class SectorTimeCalculator {

    private static final double MINIMUM_SECTOR_SECONDS = 0.001;

    SectorTimes calcular(double baseTime, double finalTime,
                         List<WeatherSnapshot> weather,
                         List<EventOccurrence> events) {
        if (!Double.isFinite(baseTime) || baseTime <= 0
                || !Double.isFinite(finalTime) || finalTime <= 0) {
            throw new IllegalArgumentException("Los tiempos de vuelta deben ser positivos y finitos");
        }
        if (weather == null || weather.isEmpty()) {
            throw new IllegalArgumentException("La evolución climática es obligatoria");
        }

        Map<TrackSector, Double> weatherWeight = weatherWeights(weather);
        double totalWeight = weatherWeight.values().stream().mapToDouble(Double::doubleValue).sum();
        Map<TrackSector, Double> partials = new EnumMap<>(TrackSector.class);

        for (TrackSector sector : comparableSectors()) {
            double eventDelta = eventDelta(events, sector);
            double partial = baseTime * weatherWeight.get(sector) / totalWeight + eventDelta;
            partials.put(sector, Math.max(MINIMUM_SECTOR_SECONDS, partial));
        }

        // EventEffectService consolida el total. La normalización elimina
        // diferencias de redondeo y también respeta su límite mínimo de 1 s.
        double calculatedTotal = partials.values().stream().mapToDouble(Double::doubleValue).sum();
        double scale = finalTime / calculatedTotal;
        return new SectorTimes(
                partials.get(TrackSector.SECTOR_1) * scale,
                partials.get(TrackSector.SECTOR_2) * scale,
                partials.get(TrackSector.SECTOR_3) * scale);
    }

    private Map<TrackSector, Double> weatherWeights(List<WeatherSnapshot> weather) {
        Map<TrackSector, Double> weights = new EnumMap<>(TrackSector.class);
        for (TrackSector sector : comparableSectors()) {
            weights.put(sector, 0.0);
        }
        for (WeatherSnapshot sample : weather) {
            TrackSector sector = TrackSector.desdeSegmento(
                    sample.segmento(), sample.totalSegmentos());
            weights.merge(sector, sample.factorTiempo(), Double::sum);
        }
        return weights;
    }

    private double eventDelta(List<EventOccurrence> events, TrackSector sector) {
        if (events == null) {
            return 0;
        }
        return events.stream()
                .filter(EventOccurrence::ocurrio)
                .filter(event -> event.sector() == sector)
                .mapToDouble(event -> event.impacto().deltaTiempoSegundos())
                .sum();
    }

    private List<TrackSector> comparableSectors() {
        return List.of(TrackSector.SECTOR_1, TrackSector.SECTOR_2, TrackSector.SECTOR_3);
    }
}
