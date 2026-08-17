package com.formula1.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventSerializationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void sessionKeepsCrashDetailsAfterSerializationRoundTrip() throws Exception {
        EventOccurrence crash = new EventOccurrence(EventType.CRASH, 16,
                "Charles Leclerc", 1, TrackSector.SECTOR_2,
                new EventImpact(0, 0, -9, 6, 4, 2,
                        0, true, true, TrackFlag.YELLOW));
        LapResult result = new LapResult(16, "Charles Leclerc", "Ferrari", "SF-24", 0);
        result.setEstadoVuelta(LapStatus.OUT);
        result.setSectorIncidente(TrackSector.SECTOR_2);
        result.setEventos(List.of(crash));
        QualifyingSession original = new QualifyingSession(
                "Circuito de Monza", WeatherCondition.LLUVIOSO, new SimulationConfig());
        original.setResultados(List.of(result));
        original.setEventos(List.of(crash));

        QualifyingSession copy = mapper.readValue(
                mapper.writeValueAsString(original), QualifyingSession.class);

        assertEquals(crash, copy.getEventos().get(0));
        assertEquals(LapStatus.OUT, copy.getResultados().get(0).getEstadoVuelta());
        assertEquals(TrackSector.SECTOR_2, copy.getResultados().get(0).getSectorIncidente());
        assertEquals(TrackFlag.YELLOW,
                copy.getResultados().get(0).getEventos().get(0).impacto().bandera());
    }
}
