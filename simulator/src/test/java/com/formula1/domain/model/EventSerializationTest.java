package com.formula1.domain.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    @Test
    void sessionKeepsLapEvolutionAfterSerializationRoundTrip() throws Exception {
        WeatherSnapshot weather = new WeatherSnapshot(1, 1, DynamicWeatherState.SECO,
                24, 50, 10, 0, 34, 96, 95, 94);
        TelemetrySnapshot sample = new TelemetrySnapshot(
                "Max Verstappen", "RB20", 1, 1,
                315, 340, 12_100, 82, 4.5,
                98, 108, 1, 72.35, -0.18, weather,
                LapStatus.VALID, EventOccurrence.noEvent(1, "Max Verstappen", 1));
        QualifyingSession original = new QualifyingSession(
                "Circuito de Monza", WeatherCondition.SECO, new SimulationConfig());
        original.setEvolucionVuelta(List.of(sample));

        QualifyingSession copy = mapper.readValue(
                mapper.writeValueAsString(original), QualifyingSession.class);

        assertEquals(List.of(sample), copy.getEvolucionVuelta());
    }

    @Test
    void oldSessionWithoutLapEvolutionRemainsCompatible() throws Exception {
        QualifyingSession session = mapper.readValue(
                "{\"circuito\":\"Circuito de Monza\"}", QualifyingSession.class);

        assertEquals(List.of(), session.getEvolucionVuelta());
    }

    @Test
    void simulationDurationSurvivesRoundTripAndOldConfigsUseDefault() throws Exception {
        SimulationConfig config = new SimulationConfig();
        config.setDuracionSegundos(120);

        SimulationConfig copy = mapper.readValue(
                mapper.writeValueAsString(config), SimulationConfig.class);
        SimulationConfig oldConfig = mapper.readValue("{}", SimulationConfig.class);

        assertEquals(120, copy.getDuracionSegundos());
        assertEquals(SimulationConfig.DURACION_PREDETERMINADA_SEGUNDOS,
                oldConfig.getDuracionSegundos());
        assertThrows(IllegalArgumentException.class,
                () -> config.setDuracionSegundos(0));
        assertThrows(IllegalArgumentException.class,
                () -> config.setDuracionSegundos(3_601));
    }

    @Test
    void sessionKeepsSectorTimesAfterSerializationRoundTrip() throws Exception {
        LapResult result = new LapResult(1, "Max Verstappen", "Red Bull", "RB20", 70.234);
        result.setSectorTimes(new SectorTimes(22.341, 24.512, 23.381));
        QualifyingSession original = new QualifyingSession(
                "Circuito de Monza", WeatherCondition.SECO, new SimulationConfig());
        original.setResultados(List.of(result));

        QualifyingSession copy = mapper.readValue(
                mapper.writeValueAsString(original), QualifyingSession.class);

        assertEquals(result.getSectorTimes(), copy.getResultados().get(0).getSectorTimes());
    }

    @Test
    void oldResultWithoutSectorTimesRemainsCompatible() throws Exception {
        LapResult result = mapper.readValue(
                "{\"pilotoId\":1,\"piloto\":\"Max Verstappen\",\"tiempoSegundos\":70.234}",
                LapResult.class);

        assertFalse(result.hasSectorTimes());
    }

    @Test
    void sessionKeepsTrackEvolutionAfterSerializationRoundTrip() throws Exception {
        TrackEvolutionSnapshot sample = new TrackEvolutionSnapshot(
                1, "Max Verstappen", 84, 85, 0, 0.55, 0);
        QualifyingSession original = new QualifyingSession(
                "Circuito de Monza", WeatherCondition.SECO, new SimulationConfig());
        original.setEvolucionPista(List.of(sample));

        QualifyingSession copy = mapper.readValue(
                mapper.writeValueAsString(original), QualifyingSession.class);

        assertEquals(List.of(sample), copy.getEvolucionPista());
    }

    @Test
    void oldSessionWithoutTrackEvolutionRemainsCompatible() throws Exception {
        QualifyingSession session = mapper.readValue(
                "{\"circuito\":\"Circuito de Monza\"}", QualifyingSession.class);

        assertEquals(List.of(), session.getEvolucionPista());
    }
}
