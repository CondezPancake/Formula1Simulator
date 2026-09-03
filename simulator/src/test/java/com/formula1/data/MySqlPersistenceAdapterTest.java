package com.formula1.data;

import com.formula1.model.AerodynamicLoad;
import com.formula1.model.DrivingMode;
import com.formula1.model.FuelStrategy;
import com.formula1.model.LapResult;
import com.formula1.model.QualifyingSession;
import com.formula1.model.SimulationConfig;
import com.formula1.model.TirePressure;
import com.formula1.model.WeatherCondition;
import com.formula1.service.LapTimeCalculator;
import com.formula1.service.QualifyingService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class MySqlPersistenceAdapterTest {

    @Test
    void cargaElModeloRelacionalCompletoCuandoMySqlEstaDisponible() {
        assumeTrue(DatabaseConnection.isAvailable());

        MySqlPersistenceAdapter adapter = new MySqlPersistenceAdapter();
        PersistencePort.CatalogData data = adapter.loadCatalogs();

        assertEquals(20, data.drivers().size());
        assertEquals(10, data.teams().size());
        assertEquals(10, data.vehicles().size());
        assertEquals(7, data.circuits().size());
        assertFalse(data.drivers().get(0).getHabilidades().isEmpty());
        assertFalse(data.vehicles().get(0).getRendimiento().isEmpty());
        assertNotNull(data.circuits().get(0).getRecordVuelta());
        assertNotNull(adapter.loadSessions());
    }

    @Test
    void guardaYRecuperaUnaSesionEnUnaTransaccion() {
        assumeTrue(DatabaseConnection.isAvailable());
        MySqlPersistenceAdapter adapter = new MySqlPersistenceAdapter();
        PersistencePort.CatalogData data = adapter.loadCatalogs();
        var driver = data.drivers().get(0);
        var vehicle = data.vehicles().stream().filter(v -> v.conduce(driver.getId())).findFirst().orElseThrow();
        var circuit = data.circuits().get(0);

        SimulationConfig config = new SimulationConfig(circuit.getNombre(), driver.getId(), vehicle.getModelo(),
                DrivingMode.NORMAL, AerodynamicLoad.MEDIA, TirePressure.ESTANDAR, FuelStrategy.BALANCEADA);
        QualifyingSession session = new QualifyingSession(circuit.getNombre(), WeatherCondition.SECO, config);
        session.setFecha(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        LapResult lap = new LapResult(driver.getId(), driver.getNombre(), driver.getEquipo(), vehicle.getModelo(), 80);
        lap.setPosicion(1);
        session.setResultados(java.util.List.of(lap));

        try {
            adapter.saveSession(session);
            QualifyingSession restored = adapter.loadSessions().stream()
                    .filter(value -> value.getId().equals(session.getId())).findFirst().orElseThrow();
            assertEquals(driver.getNombre(), restored.getResultados().get(0).getPiloto());
            assertEquals(vehicle.getModelo(), restored.getConfig().getVehiculo());
        } finally {
            adapter.deleteSession(session.getId());
        }
    }

    @Test
    void conservaUnaSimulacionCompletaConSusRelaciones() {
        assumeTrue(DatabaseConnection.isAvailable());
        MySqlPersistenceAdapter adapter = new MySqlPersistenceAdapter();
        PersistencePort.CatalogData data = adapter.loadCatalogs();
        var driver = data.drivers().get(0);
        var vehicle = data.vehicles().stream().filter(v -> v.conduce(driver.getId())).findFirst().orElseThrow();
        var circuit = data.circuits().get(0);
        SimulationConfig config = new SimulationConfig(circuit.getNombre(), driver.getId(), vehicle.getModelo(),
                DrivingMode.NORMAL, AerodynamicLoad.MEDIA, TirePressure.ESTANDAR, FuelStrategy.BALANCEADA);
        QualifyingSession session = new QualifyingService(DataStore.enMemoria(), new LapTimeCalculator())
                .simular(config, WeatherCondition.SECO, null);

        try {
            adapter.saveSession(session);
            QualifyingSession restored = adapter.loadSessions().stream()
                    .filter(value -> value.getId().equals(session.getId())).findFirst().orElseThrow();
            assertEquals(session.getResultados().size(), restored.getResultados().size());
            assertEquals(session.getEvolucionClimatica().size(), restored.getEvolucionClimatica().size());
            assertEquals(session.getEvolucionVuelta().size(), restored.getEvolucionVuelta().size());
            assertEquals(session.getEvolucionPista().size(), restored.getEvolucionPista().size());
            assertEquals(session.getEventos().size(), restored.getEventos().size());
            assertEquals(session.getParadasBoxes().size(), restored.getParadasBoxes().size());
            assertEquals(session.getCambiosNeumaticos().size(), restored.getCambiosNeumaticos().size());
        } finally {
            adapter.deleteSession(session.getId());
        }
    }

    @Test
    void actualizaLosCuatroCatalogosSinPerderRelaciones() {
        assumeTrue(DatabaseConnection.isAvailable());
        MySqlPersistenceAdapter adapter = new MySqlPersistenceAdapter();
        PersistencePort.CatalogData original = adapter.loadCatalogs();

        adapter.saveTeam(original.teams().get(0));
        adapter.saveDriver(original.drivers().get(0));
        adapter.saveVehicle(original.vehicles().get(0));
        adapter.saveCircuit(original.circuits().get(0));

        PersistencePort.CatalogData restored = adapter.loadCatalogs();
        assertEquals(original.drivers().get(0).getHabilidades(), restored.drivers().get(0).getHabilidades());
        assertEquals(original.teams().get(0).getPilotos(), restored.teams().get(0).getPilotos());
        assertEquals(original.vehicles().get(0).getRendimiento().keySet(),
                restored.vehicles().get(0).getRendimiento().keySet());
        assertEquals(original.circuits().get(0).getGanadores().size(),
                restored.circuits().get(0).getGanadores().size());
    }
}
