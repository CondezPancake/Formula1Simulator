package com.formula1.controller;

import com.formula1.data.DataStore;
import com.formula1.util.Async;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Marco de la aplicación: navegación lateral, contenido central y barra de
 * estado.
 *
 * La carga inicial de datos se hace en un {@link Task} para no bloquear el
 * hilo de JavaFX: la ventana aparece de inmediato con un indicador de
 * progreso y la navegación se habilita cuando los datos están listos.
 */
public class ShellController {

    @FXML
    private StackPane contenido;

    @FXML
    private VBox navegacion;

    @FXML
    private Label lblEstado;

    @FXML
    private ProgressIndicator cargando;

    @FXML
    public void initialize() {
        Navigator.registrar(contenido);
        navegacion.setDisable(true);
        cargando.setVisible(true);
        lblEstado.setText("Cargando datos…");

        Task<String> carga = new Task<>() {
            @Override
            protected String call() {
                return DataStore.getInstance().cargar();
            }
        };

        carga.setOnSucceeded(e -> {
            lblEstado.setText(carga.getValue() + "  ·  " + resumen());
            navegacion.setDisable(false);
            cargando.setVisible(false);
            Navigator.ir("home");
        });

        carga.setOnFailed(e -> {
            cargando.setVisible(false);
            lblEstado.setText("No se pudieron cargar los datos");
            Navigator.error("No se pudieron cargar los datos",
                    String.valueOf(carga.getException() != null ? carga.getException().getMessage() : ""));
        });

        Async.ejecutar(carga);
    }

    private String resumen() {
        DataStore datos = DataStore.getInstance();
        return datos.pilotos().size() + " pilotos · "
                + datos.equipos().size() + " equipos · "
                + datos.vehiculos().size() + " vehículos · "
                + datos.circuitos().size() + " circuitos";
    }

    @FXML
    private void onInicio() {
        Navigator.ir("home");
    }

    @FXML
    private void onPilotos() {
        Navigator.ir("drivers");
    }

    @FXML
    private void onEquipos() {
        Navigator.ir("teams");
    }

    @FXML
    private void onVehiculos() {
        Navigator.ir("vehicles");
    }

    @FXML
    private void onCircuitos() {
        Navigator.ir("circuits");
    }

    @FXML
    private void onSimulacion() {
        Navigator.ir("simulation");
    }

    @FXML
    private void onHistorial() {
        Navigator.ir("history");
    }
}
