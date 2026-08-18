package com.formula1.service;

import com.formula1.data.DataStore;
import com.formula1.model.Circuit;
import com.formula1.model.Driver;
import com.formula1.model.DriverRole;
import com.formula1.model.Vehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** CRUD, búsquedas y reglas de negocio de los cuatro catálogos. */
class CatalogServiceTest {

    private DataStore datos;
    private DriverService pilotos;
    private TeamService equipos;
    private VehicleService vehiculos;
    private CircuitService circuitos;

    @BeforeEach
    void preparar() {
        datos = DataStore.enMemoria();
        pilotos = new DriverService(datos);
        equipos = new TeamService(datos);
        vehiculos = new VehicleService(datos);
        circuitos = new CircuitService(datos);
    }

    @Test
    void buscaCircuitosPorNombreYPorUbicacion() {
        assertEquals(1, circuitos.buscar("monza").size());
        assertEquals(1, circuitos.buscar("Brasil").size(), "Interlagos por país");
        assertEquals(7, circuitos.buscar("").size(), "sin texto devuelve todo");
    }

    @Test
    void buscaPilotosPorNombreEquipoYRol() {
        assertEquals(1, pilotos.buscar("verstappen").size());
        assertEquals(2, pilotos.buscar("Ferrari").size());
        assertEquals(10, pilotos.buscar("Líder").size(), "un líder por equipo");
    }

    @Test
    void buscaVehiculosPorCaracteristicas() {
        assertEquals(1, vehiculos.buscar("RB20", null).size());
        assertTrue(vehiculos.buscar(null, 355).size() >= 2, "solo los más rápidos");
        assertEquals(10, vehiculos.buscar(null, null).size());
    }

    @Test
    void rechazaAsignarUnPilotoDeOtroEquipo() {
        Vehicle rb20 = vehiculos.porModelo("RB20").orElseThrow();

        // Hamilton (id 3) corre para Mercedes: no puede pilotar un Red Bull.
        ValidationException error = assertThrows(ValidationException.class,
                () -> vehiculos.asignarPilotos(rb20, List.of(3)));

        assertTrue(error.getMessage().contains("Mercedes"));
    }

    @Test
    void permiteAsignarPilotosDelMismoEquipo() {
        Vehicle rb20 = vehiculos.porModelo("RB20").orElseThrow();

        vehiculos.asignarPilotos(rb20, List.of(1, 2));

        assertEquals(List.of(1, 2), vehiculos.porModelo("RB20").orElseThrow().getPilotos());
    }

    @Test
    void desmarcarPilotoEliminaSuAsignacionReal() {
        Vehicle rb20 = vehiculos.porModelo("RB20").orElseThrow();
        vehiculos.asignarPilotos(rb20, List.of(1, 2));

        vehiculos.asignarPilotos(rb20, List.of(1));

        assertEquals(List.of(1), vehiculos.porModelo("RB20").orElseThrow().getPilotos());
        assertFalse(vehiculos.porModelo("RB20").orElseThrow().conduce(2));
    }

    @Test
    void rechazaDatosInvalidos() {
        assertThrows(ValidationException.class, () -> pilotos.guardar(new Driver(99, "  ", "Ferrari", DriverRole.LIDER, 1)));
        assertThrows(ValidationException.class, () -> pilotos.guardar(new Driver(99, "X", "Equipo Fantasma", DriverRole.LIDER, 1)));
        assertThrows(ValidationException.class, () -> circuitos.guardar(new Circuit("X", "Y", 0, 10)));
        assertThrows(ValidationException.class, () -> circuitos.guardar(new Circuit("X", "Y", 5, 0)));
    }

    @Test
    void daDeAltaYDeBajaUnPiloto() {
        int id = pilotos.siguienteId();
        assertEquals(21, id);

        pilotos.guardar(new Driver(id, "Piloto Nuevo", "Ferrari", DriverRole.ESCUDERO, 0));
        assertEquals(21, pilotos.listar().size());

        pilotos.eliminar(id);
        assertEquals(20, pilotos.listar().size());
    }

    @Test
    void altaYBajaDePilotoSincronizaInmediatamenteSuEquipo() {
        int id = pilotos.siguienteId();
        pilotos.guardar(new Driver(id, "Piloto Nuevo", "Ferrari", DriverRole.ESCUDERO, 0));

        var ferrari = equipos.porNombre("Ferrari").orElseThrow();
        assertTrue(ferrari.getPilotos().contains(id));
        assertTrue(equipos.pilotosDe(ferrari).stream()
                .anyMatch(p -> p.getNombre().equals("Piloto Nuevo")));

        pilotos.eliminar(id);
        assertFalse(ferrari.getPilotos().contains(id));
    }

    @Test
    void impideBorrarUnEquipoConPilotos() {
        assertThrows(ValidationException.class, () -> equipos.eliminar("Ferrari"));
    }

    @Test
    void resuelveLosGanadoresHistoricosANombres() {
        Circuit monza = circuitos.porNombre("Circuito de Monza").orElseThrow();

        List<String> ganadores = circuitos.ganadoresDe(monza);

        assertEquals(3, ganadores.size());
        assertTrue(ganadores.get(0).startsWith("2021"), "ordenados por temporada");
        assertFalse(ganadores.get(0).contains("Piloto "), "el id debe resolverse a nombre");
    }
}
