package com.formula1.application.usecase;

import com.formula1.application.port.out.CatalogPort;
import com.formula1.domain.model.Circuit;
import com.formula1.domain.model.Driver;
import com.formula1.domain.model.DriverRole;
import com.formula1.domain.model.Team;
import com.formula1.domain.model.Vehicle;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba de caracterización de la Fase 2 (migración a hexagonal): los
 * servicios de catálogo ya no requieren el singleton {@code DataStore} ni su
 * carga JDBC/seed — les basta cualquier implementación de {@link CatalogPort}.
 * Este falso en memoria no toca {@code DataStore}, JDBC ni {@code seed.json}.
 */
class CatalogPortDecouplingTest {

    /** Implementación mínima de {@link CatalogPort} sin relación con DataStore. */
    private static final class InMemoryCatalogPort implements CatalogPort {
        final Map<Integer, Driver> pilotos = new LinkedHashMap<>();
        final Map<String, Team> equipos = new LinkedHashMap<>();
        final Map<String, Vehicle> vehiculos = new LinkedHashMap<>();
        final Map<String, Circuit> circuitos = new LinkedHashMap<>();

        @Override
        public Map<Integer, Driver> pilotos() {
            return pilotos;
        }

        @Override
        public void guardarPiloto(Driver piloto) {
            pilotos.put(piloto.getId(), piloto);
        }

        @Override
        public void eliminarPiloto(int id) {
            pilotos.remove(id);
        }

        @Override
        public Map<String, Team> equipos() {
            return equipos;
        }

        @Override
        public void guardarEquipo(Team equipo) {
            equipos.put(equipo.getNombre(), equipo);
        }

        @Override
        public void eliminarEquipo(String nombre) {
            equipos.remove(nombre);
        }

        @Override
        public Map<String, Vehicle> vehiculos() {
            return vehiculos;
        }

        @Override
        public void guardarVehiculo(Vehicle vehiculo) {
            vehiculos.put(vehiculo.getModelo(), vehiculo);
        }

        @Override
        public void eliminarVehiculo(String modelo) {
            vehiculos.remove(modelo);
        }

        @Override
        public Map<String, Circuit> circuitos() {
            return circuitos;
        }

        @Override
        public void guardarCircuito(Circuit circuito) {
            circuitos.put(circuito.getNombre(), circuito);
        }

        @Override
        public void eliminarCircuito(String nombre) {
            circuitos.remove(nombre);
        }
    }

    @Test
    void driverServiceOperaContraUnFalsoQueNoEsDataStore() {
        InMemoryCatalogPort falso = new InMemoryCatalogPort();
        falso.guardarEquipo(new Team("Williams", "Reino Unido", "Mercedes"));
        DriverService pilotos = new DriverService(falso);

        pilotos.guardar(new Driver(44, "Piloto de Prueba", "Williams", DriverRole.LIDER, 5));

        assertEquals(1, pilotos.listar().size());
        assertTrue(pilotos.porId(44).isPresent());

        pilotos.eliminar(44);
        assertTrue(pilotos.listar().isEmpty());
    }
}
