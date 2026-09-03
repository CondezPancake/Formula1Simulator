package com.formula1.service;

import com.formula1.data.DataStore;
import com.formula1.domain.event.EventManager;
import com.formula1.domain.model.AerodynamicLoad;
import com.formula1.domain.model.Driver;
import com.formula1.domain.model.DrivingMode;
import com.formula1.domain.model.FuelStrategy;
import com.formula1.domain.model.SimulationConfig;
import com.formula1.domain.model.TelemetrySnapshot;
import com.formula1.domain.model.TirePressure;
import com.formula1.domain.model.TrackEvolutionSnapshot;
import com.formula1.domain.model.Vehicle;
import com.formula1.domain.model.WeatherCondition;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvolutionDataFlowTest {

    @Test
    void publicaCambiosRealesDePistaYClimaDuranteLaSesion() {
        DataStore datos = DataStore.enMemoria();
        Driver piloto = datos.pilotos().get(1);
        Vehicle vehiculo = datos.vehiculos().get("RB20");
        SimulationConfig config = new SimulationConfig(
                "Circuito de Monza", piloto.getId(), vehiculo.getModelo(), DrivingMode.NORMAL,
                AerodynamicLoad.MEDIA, TirePressure.ESTANDAR, FuelStrategy.BALANCEADA);
        QualifyingService servicio = new QualifyingService(
                datos, new LapTimeCalculator(new Random(7)),
                new DynamicWeatherService(new Random(19)), new EventManager(31));
        List<TrackEvolutionSnapshot> pista = new ArrayList<>();
        List<TelemetrySnapshot> telemetria = new ArrayList<>();

        servicio.simular(config, WeatherCondition.SECO, null, null,
                telemetria::add, pista::add);

        assertEquals(20, pista.size(), "debe publicarse el paso de cada piloto");
        assertTrue(valoresDistintos(pista.stream()
                .map(TrackEvolutionSnapshot::gripFinalPorcentaje).toList()) > 1);
        assertTrue(pista.get(pista.size() - 1).gripFinalPorcentaje()
                > pista.get(0).gripFinalPorcentaje());

        assertEquals(20, telemetria.size());
        assertTrue(valoresDistintos(telemetria.stream()
                .map(m -> m.clima().temperaturaPistaC()).toList()) > 1);
        assertTrue(valoresDistintos(telemetria.stream()
                .map(m -> m.clima().humedadPorcentaje()).toList()) > 1);
        assertTrue(valoresDistintos(telemetria.stream()
                .map(m -> m.clima().gripPorcentaje()).toList()) > 1);
    }

    private long valoresDistintos(List<Double> valores) {
        return valores.stream().map(valor -> Math.round(valor * 1_000)).distinct().count();
    }
}
