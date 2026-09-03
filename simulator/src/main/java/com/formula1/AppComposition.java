package com.formula1;

import com.formula1.controller.CircuitController;
import com.formula1.controller.CircuitDetailController;
import com.formula1.controller.ConfigController;
import com.formula1.controller.DriverController;
import com.formula1.controller.DriverDetailController;
import com.formula1.controller.ExploreDriversController;
import com.formula1.controller.ExploreTeamsController;
import com.formula1.controller.HistoryController;
import com.formula1.controller.SimulationController;
import com.formula1.controller.TeamController;
import com.formula1.controller.TeamDetailController;
import com.formula1.controller.VehicleCompareController;
import com.formula1.controller.VehicleController;
import com.formula1.data.DataStore;
import com.formula1.service.CircuitService;
import com.formula1.service.DriverService;
import com.formula1.service.LapTimeCalculator;
import com.formula1.service.QualifyingService;
import com.formula1.service.TeamService;
import com.formula1.service.VehicleService;

import javafx.util.Callback;

/**
 * Punto único de construcción de la aplicación (Fase 6 de la migración a
 * hexagonal). Crea una sola vez cada servicio y expone una fábrica de
 * controladores que {@link App} y {@code Navigator} comparten, para que
 * ninguna pantalla vuelva a montar su propio {@code DriverService},
 * {@code QualifyingService}, etc. — antes cada controlador lo hacía en su
 * constructor sin argumentos.
 *
 * {@link DataStore#getInstance()} sigue siendo el único origen de datos: el
 * singleton no se retira en esta fase. Lo que cambia es que ahora solo esta
 * clase lo resuelve; el resto de la aplicación recibe instancias ya
 * construidas. Los controladores conservan su constructor sin argumentos
 * (compatibilidad con {@code fx:controller} cuando se cargan fuera de esta
 * fábrica, y con los tests que los construyen directamente).
 */
public final class AppComposition {

    private final DataStore datos = DataStore.getInstance();

    private final DriverService pilotos = new DriverService(datos);
    private final TeamService equipos = new TeamService(datos);
    private final VehicleService vehiculos = new VehicleService(datos);
    private final CircuitService circuitos = new CircuitService(datos);
    private final QualifyingService sesiones = new QualifyingService(datos, new LapTimeCalculator());

    /** Fuente de datos compartida; solo {@link App} debería usarla, para el arranque. */
    DataStore datos() {
        return datos;
    }

    /** Fábrica que {@link App} y {@code Navigator} instalan en cada {@code FXMLLoader}. */
    public Callback<Class<?>, Object> controllerFactory() {
        return this::crear;
    }

    private Object crear(Class<?> tipo) {
        try {
            if (tipo == CircuitController.class) {
                return new CircuitController(circuitos);
            }
            if (tipo == CircuitDetailController.class) {
                return new CircuitDetailController(circuitos, vehiculos);
            }
            if (tipo == ConfigController.class) {
                return new ConfigController(vehiculos, circuitos, datos);
            }
            if (tipo == DriverController.class) {
                return new DriverController(pilotos, equipos);
            }
            if (tipo == DriverDetailController.class) {
                return new DriverDetailController(pilotos, vehiculos, sesiones);
            }
            if (tipo == ExploreDriversController.class) {
                return new ExploreDriversController(pilotos, equipos);
            }
            if (tipo == ExploreTeamsController.class) {
                return new ExploreTeamsController(equipos, vehiculos);
            }
            if (tipo == HistoryController.class) {
                return new HistoryController(sesiones, datos);
            }
            if (tipo == SimulationController.class) {
                return new SimulationController(sesiones, circuitos, vehiculos, pilotos, datos);
            }
            if (tipo == TeamController.class) {
                return new TeamController(equipos);
            }
            if (tipo == TeamDetailController.class) {
                return new TeamDetailController(equipos, vehiculos, sesiones);
            }
            if (tipo == VehicleCompareController.class) {
                return new VehicleCompareController(vehiculos);
            }
            if (tipo == VehicleController.class) {
                return new VehicleController(vehiculos, equipos, pilotos);
            }
            // Controladores sin dependencias propias (ShellController,
            // MainMenuController, ManagementController...): constructor
            // sin argumentos, igual que hacía el FXMLLoader por defecto.
            return tipo.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("No se pudo construir el controlador " + tipo.getName(), e);
        }
    }
}
