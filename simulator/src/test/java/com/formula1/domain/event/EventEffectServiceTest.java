package com.formula1.domain.event;

import com.formula1.domain.model.DynamicWeatherState;
import com.formula1.domain.model.EventImpact;
import com.formula1.domain.model.EventOccurrence;
import com.formula1.domain.model.EventType;
import com.formula1.domain.model.LapResult;
import com.formula1.domain.model.LapStatus;
import com.formula1.domain.model.TrackFlag;
import com.formula1.domain.model.TrackSector;
import com.formula1.domain.model.WeatherSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventEffectServiceTest {

    private final EventEffectService effects = new EventEffectService();

    @Test
    void performanceEventChangesRealLapTimeAndWear() {
        LapResult result = new LapResult(1, "Driver", "Team", "Car", 80);
        EventOccurrence lockUp = occurrence(EventType.LOCK_UP, TrackSector.SECTOR_2,
                new EventImpact(0.35, 0.95, -3, 0.6,
                        7, 2, 0, false, false, TrackFlag.GREEN));

        effects.applyResult(result, 80, 2.1, 4.2, List.of(lockUp));

        assertEquals(80.35, result.getTiempoSegundos(), 1e-9);
        assertEquals(4.8, result.getDesgasteEstimado(), 1e-9);
        assertEquals(LapStatus.VALID, result.getEstadoVuelta());
        assertEquals(lockUp, result.getEventos().get(0));
    }

    @Test
    void crashInvalidatesLapAndKeepsPartialConsumption() {
        LapResult result = new LapResult(1, "Driver", "Team", "Car", 80);
        EventOccurrence crash = occurrence(EventType.CRASH, TrackSector.SECTOR_2,
                new EventImpact(0, 0, -10, 8,
                        5, 3, 0, true, true, TrackFlag.RED));

        effects.applyResult(result, 80, 2.4, 6, List.of(crash));

        assertEquals(0, result.getTiempoSegundos());
        assertEquals(1.2, result.getConsumoEstimado(), 1e-9);
        assertEquals(11, result.getDesgasteEstimado(), 1e-9);
        assertEquals(LapStatus.OUT, result.getEstadoVuelta());
        assertEquals(TrackSector.SECTOR_2, result.getSectorIncidente());
    }

    @Test
    void rainEventChangesOnlyTheCurrentAndFollowingSectors() {
        List<WeatherSnapshot> weather = java.util.stream.IntStream.rangeClosed(1, 20)
                .mapToObj(segment -> new WeatherSnapshot(segment, 20, DynamicWeatherState.SECO,
                        24, 55, 45, 0, 34, 95, 94, 93))
                .toList();
        EventOccurrence rain = occurrence(EventType.RAIN_STARTS, TrackSector.SECTOR_2,
                new EventImpact(0, 0.97, -7, 0,
                        -5, 0, 20, false, false, TrackFlag.GREEN));

        List<WeatherSnapshot> updated = effects.applyGlobalWeather(weather, List.of(rain));

        assertEquals(weather.get(6), updated.get(6), "El sector previo no cambia");
        assertTrue(updated.get(7).intensidadLluviaPorcentaje()
                > weather.get(7).intensidadLluviaPorcentaje());
        assertTrue(updated.get(19).gripPorcentaje() < weather.get(19).gripPorcentaje());
    }

    private EventOccurrence occurrence(EventType type, TrackSector sector, EventImpact impact) {
        return new EventOccurrence(type, 1, "Driver", 1, sector, impact);
    }
}
