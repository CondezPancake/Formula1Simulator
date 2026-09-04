package com.formula1.application.usecase;

import com.formula1.domain.model.AerodynamicLoad;
import com.formula1.domain.model.DrivingMode;
import com.formula1.domain.model.DynamicWeatherState;
import com.formula1.domain.model.FuelStrategy;
import com.formula1.domain.model.SimulationConfig;
import com.formula1.domain.model.TirePressure;
import com.formula1.domain.model.Vehicle;
import com.formula1.domain.model.WeatherSnapshot;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VehicleSpeedTest {

    @Test
    void laVelocidadInstantaneaSupera250CuandoElVehiculoPuedeHacerlo() {
        Vehicle vehiculo = vehiculo();
        SimulationConfig config = config(DrivingMode.NORMAL);

        double maximaMostrada = velocidades(vehiculo, config, 1).stream()
                .mapToDouble(Double::doubleValue).max().orElseThrow();

        assertTrue(maximaMostrada > 330,
                "la recta debe reflejar la punta disponible del vehículo");
        assertTrue(maximaMostrada <= vehiculo.getVelocidadMaximaKmh());
    }

    @Test
    void elModoAgresivoPuedeAlcanzarLaMaximaSinSobrepasarla() {
        Vehicle vehiculo = vehiculo();
        SimulationConfig config = config(DrivingMode.AGRESIVA);

        double maximaMostrada = velocidades(vehiculo, config, 1.2).stream()
                .mapToDouble(Double::doubleValue).max().orElseThrow();

        assertEquals(vehiculo.getVelocidadMaximaKmh(), maximaMostrada, 1e-9);
    }

    private List<Double> velocidades(Vehicle vehiculo, SimulationConfig config,
                                     double multiplicadorEvento) {
        WeatherSnapshot clima = new WeatherSnapshot(
                1, 20, DynamicWeatherState.SECO, 24, 45,
                0, 0, 32, 100, 100, 100);
        return java.util.stream.IntStream.rangeClosed(1, 20)
                .mapToObj(segmento -> QualifyingService.calcularVelocidad(
                        vehiculo, config, 250, 1, clima,
                        segmento / 20.0, multiplicadorEvento))
                .toList();
    }

    private Vehicle vehiculo() {
        Vehicle vehiculo = new Vehicle("RB20", "Red Bull Racing", "Honda", 360, 2.5);
        EnumMap<DrivingMode, Vehicle.Performance> rendimiento = new EnumMap<>(DrivingMode.class);
        rendimiento.put(DrivingMode.NORMAL, new Vehicle.Performance(320));
        rendimiento.put(DrivingMode.AGRESIVA, new Vehicle.Performance(340));
        rendimiento.put(DrivingMode.AHORRO, new Vehicle.Performance(300));
        vehiculo.setRendimiento(rendimiento);
        return vehiculo;
    }

    private SimulationConfig config(DrivingMode modo) {
        return new SimulationConfig("Monza", 1, "RB20", modo,
                AerodynamicLoad.MEDIA, TirePressure.ESTANDAR, FuelStrategy.BALANCEADA);
    }
}
