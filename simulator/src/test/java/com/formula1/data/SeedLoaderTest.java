package com.formula1.data;

import com.formula1.model.Circuit;
import com.formula1.model.DriverRole;
import com.formula1.model.DrivingMode;
import com.formula1.model.Vehicle;
import com.formula1.model.WeatherCondition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Comprueba que seed.json se lee entero y reproduce fielmente los datos de
 * la especificación. Si el mapeo se rompiera, la aplicación arrancaría con
 * datos vacíos en lugar de fallar, así que conviene fijarlo en un test.
 */
class SeedLoaderTest {

    private final SeedLoader.Seed seed = SeedLoader.cargar();

    @Test
    void cargaLaParrillaCompleta() {
        assertEquals(20, seed.getPilotos().size());
        assertEquals(10, seed.getEquipos().size());
        assertEquals(10, seed.getVehiculos().size());
        assertEquals(7, seed.getCircuitos().size());
    }

    @Test
    void todoPilotoTieneUnVehiculoConElQueCorrer() {
        List<Integer> conVehiculo = seed.getVehiculos().stream()
                .flatMap(v -> v.getPilotos().stream())
                .toList();

        assertEquals(20, conVehiculo.size(), "los 20 pilotos deben poder clasificar");
        seed.getPilotos().forEach(p ->
                assertTrue(conVehiculo.contains(p.getId()), "sin vehículo: " + p.getNombre()));
    }

    @Test
    void reproduceLosValoresLiteralesDelRb20() {
        Vehicle rb20 = seed.getVehiculos().stream()
                .filter(v -> v.getModelo().equals("RB20")).findFirst().orElseThrow();

        assertEquals("Red Bull Racing", rb20.getEquipo());
        assertEquals(360, rb20.getVelocidadMaximaKmh());
        assertEquals(2.5, rb20.getAceleracion0100(), 0.001);

        Vehicle.Performance normal = rb20.rendimientoDe(DrivingMode.NORMAL);
        assertEquals(320, normal.getVelocidadPromedioKmh());
        assertEquals(1.9, normal.consumoCon(WeatherCondition.SECO), 0.001);
        assertEquals(2.1, normal.consumoCon(WeatherCondition.LLUVIOSO), 0.001);
        assertEquals(2.4, normal.consumoCon(WeatherCondition.EXTREMO), 0.001);
        assertEquals(1.5, normal.desgasteCon(WeatherCondition.SECO), 0.001);

        Vehicle.Performance agresiva = rb20.rendimientoDe(DrivingMode.AGRESIVA);
        assertEquals(340, agresiva.getVelocidadPromedioKmh());
        assertEquals(3.5, agresiva.desgasteCon(WeatherCondition.EXTREMO), 0.001);
    }

    @Test
    void leeLosPilotosConSuRolYHabilidades() {
        var verstappen = seed.getPilotos().stream()
                .filter(p -> p.getId() == 1).findFirst().orElseThrow();

        assertEquals("Max Verstappen", verstappen.getNombre());
        assertEquals("Red Bull Racing", verstappen.getEquipo());
        assertEquals(DriverRole.LIDER, verstappen.getRol());
        assertTrue(verstappen.getHabilidad(com.formula1.model.Driver.HABILIDAD_VELOCIDAD) > 90);
    }

    @Test
    void leeElRecordYLosGanadoresDeCadaCircuito() {
        Circuit monaco = seed.getCircuitos().stream()
                .filter(c -> c.getNombre().contains("Mónaco")).findFirst().orElseThrow();

        assertEquals(3.34, monaco.getLongitudKm(), 0.001);
        assertEquals(78, monaco.getVueltas());
        assertEquals(70.166, monaco.getRecordVuelta().getTiempoSegundos(), 0.001);
        assertEquals("Lewis Hamilton", monaco.getRecordVuelta().getPiloto());
        assertEquals(3, monaco.getGanadores().size());

        // El factor técnico precalculado coincide con el derivado del récord.
        assertEquals(monaco.calcularFactorTecnico(), monaco.getFactorTecnico(), 0.01);
        assertEquals(1.98, monaco.getFactorTecnico(), 0.02);
    }

    @Test
    void laProbabilidadDeClimaSumaUnoEnTodoCircuito() {
        for (Circuit circuito : seed.getCircuitos()) {
            double total = 0;
            for (WeatherCondition clima : WeatherCondition.values()) {
                total += circuito.probabilidadDe(clima);
            }
            assertEquals(1.0, total, 0.001, "clima mal repartido en " + circuito.getNombre());
        }
    }
}
