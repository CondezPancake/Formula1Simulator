package com.formula1.event;

import com.formula1.model.Driver;
import com.formula1.model.DrivingMode;
import com.formula1.model.SimulationConfig;
import com.formula1.model.TrackSector;
import com.formula1.model.Vehicle;
import com.formula1.model.WeatherSnapshot;
import com.formula1.util.MathUtils;

import java.util.List;

/** Estima las métricas previas necesarias para evaluar compatibilidad. */
public final class EventContextFactory {

    public EventContext create(int sequence, Driver driver, Vehicle vehicle,
                               SimulationConfig config, List<WeatherSnapshot> weather,
                               double tyreWear) {
        double trackTemperature = weather.stream()
                .mapToDouble(WeatherSnapshot::temperaturaPistaC)
                .average().orElse(30);
        double rainIntensity = weather.stream()
                .mapToDouble(WeatherSnapshot::intensidadLluviaPorcentaje)
                .average().orElse(0);
        double speedRatio = vehicle.rendimientoDe(config.getModo()).getVelocidadPromedioKmh()
                / (double) Math.max(1, vehicle.getVelocidadMaximaKmh());
        double modeHeat = config.getModo() == DrivingMode.AGRESIVA ? 8
                : config.getModo() == DrivingMode.AHORRO ? -5 : 0;
        double tyreTemperature = MathUtils.clamp(
                58 + 0.35 * trackTemperature + 18 * speedRatio
                        + modeHeat - 0.18 * rainIntensity, 35, 125);
        double engineTemperature = MathUtils.clamp(
                86 + 30 * speedRatio + modeHeat, 75, 125);

        // La densidad cambia por tanda; 0 es aire limpio y 3-4 habilita tráfico pesado.
        int traffic = Math.floorMod(sequence + driver.getId(), 5);
        return new EventContext(sequence, 1, driver, vehicle, config, weather,
                tyreTemperature, engineTemperature, tyreWear,
                traffic, 0, TrackSector.NONE);
    }
}
