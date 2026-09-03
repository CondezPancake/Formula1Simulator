package com.formula1.service;

import com.formula1.data.DataStore;
import com.formula1.domain.model.AerodynamicLoad;
import com.formula1.domain.model.Circuit;
import com.formula1.domain.model.Driver;
import com.formula1.domain.model.DrivingMode;
import com.formula1.domain.model.DynamicWeatherState;
import com.formula1.domain.model.FuelStrategy;
import com.formula1.domain.model.SimulationConfig;
import com.formula1.domain.model.TirePressure;
import com.formula1.domain.model.Vehicle;
import com.formula1.domain.model.WeatherSnapshot;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrackEvolutionServiceTest {

    private final TrackEvolutionService service = new TrackEvolutionService();

    @Test
    void dryLapsDepositRubberUntilTheTrackLimit() {
        double rubber = 0;
        TrackEvolutionService.Evolution first = service.evolucionar(
                weather(0, 95), rubber, 1, "Driver 1");
        TrackEvolutionService.Evolution current = first;

        for (int lap = 2; lap <= 30; lap++) {
            current = service.evolucionar(
                    weather(0, 95), current.gomaFinalPorcentaje(), lap, "Driver " + lap);
        }

        assertTrue(first.resumen().gomaFinalPorcentaje() > 0);
        assertEquals(TrackEvolutionService.MAX_GOMA_PORCENTAJE,
                current.gomaFinalPorcentaje(), 1e-9);
        assertTrue(current.resumen().gripFinalPorcentaje()
                > first.resumen().gripFinalPorcentaje());
    }

    @Test
    void rainRemovesRubberAndGripProgressively() {
        TrackEvolutionService.Evolution wet = service.evolucionar(
                weather(80, 68), TrackEvolutionService.MAX_GOMA_PORCENTAJE,
                1, "Wet driver");

        assertTrue(wet.gomaFinalPorcentaje() < TrackEvolutionService.MAX_GOMA_PORCENTAJE);
        assertTrue(wet.resumen().gripFinalPorcentaje()
                < wet.resumen().gripInicialPorcentaje());
        assertEquals("La lluvia limpia la pista", wet.resumen().tendencia());
    }

    @Test
    void accumulatedRubberProducesAFasterLap() {
        DataStore data = DataStore.enMemoria();
        Driver driver = data.pilotos().get(1);
        Vehicle vehicle = data.vehiculos().get("RB20");
        Circuit circuit = data.circuitos().get("Circuito de Monza");
        SimulationConfig config = new SimulationConfig(
                circuit.getNombre(), driver.getId(), vehicle.getModelo(), DrivingMode.NORMAL,
                AerodynamicLoad.MEDIA, TirePressure.ESTANDAR, FuelStrategy.BALANCEADA);
        List<WeatherSnapshot> base = weather(0, 95);
        List<WeatherSnapshot> greenTrack = service.evolucionar(
                base, 0, 1, driver.getNombre()).clima();
        List<WeatherSnapshot> rubberedTrack = service.evolucionar(
                base, TrackEvolutionService.MAX_GOMA_PORCENTAJE,
                20, driver.getNombre()).clima();

        double greenTime = new LapTimeCalculator(new Random(7)).calcularTiempo(
                driver, vehicle, circuit, greenTrack, config);
        double rubberedTime = new LapTimeCalculator(new Random(7)).calcularTiempo(
                driver, vehicle, circuit, rubberedTrack, config);

        assertTrue(rubberedTime < greenTime);
    }

    @Test
    void rejectsAnInvalidInitialState() {
        assertThrows(ValidationException.class, () ->
                service.evolucionar(weather(0, 95), -1, 1, "Driver"));
    }

    private List<WeatherSnapshot> weather(double rain, double grip) {
        List<WeatherSnapshot> samples = new ArrayList<>();
        DynamicWeatherState state = rain == 0
                ? DynamicWeatherState.SECO
                : DynamicWeatherState.LLUVIA_INTENSA;
        for (int segment = 1; segment <= 20; segment++) {
            samples.add(new WeatherSnapshot(
                    segment, 20, state, 24, rain == 0 ? 45 : 90,
                    rain, rain, rain == 0 ? 36 : 18,
                    grip, grip - 2, grip - 3));
        }
        return List.copyOf(samples);
    }
}
