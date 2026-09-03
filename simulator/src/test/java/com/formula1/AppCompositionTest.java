package com.formula1;

import com.formula1.controller.CircuitController;
import com.formula1.controller.CircuitDetailController;
import com.formula1.controller.ConfigController;
import com.formula1.controller.DriverController;
import com.formula1.controller.DriverDetailController;
import com.formula1.controller.ExploreDriversController;
import com.formula1.controller.ExploreTeamsController;
import com.formula1.controller.HistoryController;
import com.formula1.controller.ManagementController;
import com.formula1.controller.SimulationController;
import com.formula1.controller.TeamController;
import com.formula1.controller.TeamDetailController;
import com.formula1.controller.VehicleCompareController;
import com.formula1.controller.VehicleController;
import org.junit.jupiter.api.Test;

import javafx.util.Callback;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Prueba de caracterización de la Fase 6: ni {@code ViewsLoadTest} (usa un
 * {@code FXMLLoader} propio) ni {@code MenuNavegacionTest} (no instala la
 * fábrica de {@code Navigator}) ejercitan la fábrica de controladores de
 * {@link AppComposition} — solo se ejercita al arrancar la aplicación real.
 * Este test construye cada controlador con servicios a través de esa
 * fábrica y comprueba que no lanza y que el tipo es el correcto, sin
 * necesitar el toolkit de JavaFX (los constructores no tocan nodos de
 * escena; eso lo inyecta FXMLLoader después mediante los campos
 * {@code @FXML}).
 */
class AppCompositionTest {

    private final Callback<Class<?>, Object> fabrica = new AppComposition().controllerFactory();

    @Test
    void construyeLosControladoresConDependenciasExplicitas() {
        assertInstanceOf(CircuitController.class, fabrica.call(CircuitController.class));
        assertInstanceOf(CircuitDetailController.class, fabrica.call(CircuitDetailController.class));
        assertInstanceOf(ConfigController.class, fabrica.call(ConfigController.class));
        assertInstanceOf(DriverController.class, fabrica.call(DriverController.class));
        assertInstanceOf(DriverDetailController.class, fabrica.call(DriverDetailController.class));
        assertInstanceOf(ExploreDriversController.class, fabrica.call(ExploreDriversController.class));
        assertInstanceOf(ExploreTeamsController.class, fabrica.call(ExploreTeamsController.class));
        assertInstanceOf(HistoryController.class, fabrica.call(HistoryController.class));
        assertInstanceOf(SimulationController.class, fabrica.call(SimulationController.class));
        assertInstanceOf(TeamController.class, fabrica.call(TeamController.class));
        assertInstanceOf(TeamDetailController.class, fabrica.call(TeamDetailController.class));
        assertInstanceOf(VehicleCompareController.class, fabrica.call(VehicleCompareController.class));
        assertInstanceOf(VehicleController.class, fabrica.call(VehicleController.class));
    }

    @Test
    void caeAlConstructorSinArgumentosParaControladoresSinDependencias() {
        // ManagementController no tiene constructor propio: prueba que el
        // camino por defecto (reflexión) sigue funcionando para el resto de
        // los 18 fx:controller que no necesitan servicios compartidos.
        assertInstanceOf(ManagementController.class, fabrica.call(ManagementController.class));
    }
}
