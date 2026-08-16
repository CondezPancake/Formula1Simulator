package com.formula1.controller;

import com.formula1.data.DataStore;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

/** Pantalla de inicio con el resumen de la parrilla cargada. */
public class HomeController {

    @FXML
    private Label lblPilotos;

    @FXML
    private Label lblEquipos;

    @FXML
    private Label lblVehiculos;

    @FXML
    private Label lblCircuitos;

    @FXML
    public void initialize() {
        DataStore datos = DataStore.getInstance();
        lblPilotos.setText(String.valueOf(datos.pilotos().size()));
        lblEquipos.setText(String.valueOf(datos.equipos().size()));
        lblVehiculos.setText(String.valueOf(datos.vehiculos().size()));
        lblCircuitos.setText(String.valueOf(datos.circuitos().size()));
    }

    @FXML
    private void onNuevaClasificacion() {
        Navigator.ir("simulation");
    }
}
