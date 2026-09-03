package com.formula1.adapter.in.javafx;

import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

/** Coordina las pestañas internas de Gestión y sus refrescos cruzados. */
public class ManagementController {

    @FXML private TabPane secciones;
    @FXML private Tab tabEquipos;
    @FXML private Tab tabVehiculos;
    @FXML private Tab tabCircuitos;
    @FXML private TeamController equiposViewController;

    @FXML
    public void initialize() {
        secciones.getSelectionModel().selectedItemProperty().addListener((o, anterior, actual) -> {
            if (actual == tabEquipos) {
                equiposViewController.refrescarVista();
            }
        });
    }

    /** Regresa a la lista que originó una comparación conservando filtros y selección. */
    public void mostrarVehiculos() {
        secciones.getSelectionModel().select(tabVehiculos);
    }

    public void mostrarCircuitos() {
        secciones.getSelectionModel().select(tabCircuitos);
    }
}
