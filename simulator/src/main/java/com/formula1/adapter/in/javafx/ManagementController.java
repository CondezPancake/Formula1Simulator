package com.formula1.adapter.in.javafx;

import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

/** Coordina las pestañas internas de Gestión y sus refrescos cruzados. */
public class ManagementController {

    @FXML private TabPane secciones;
    @FXML private Tab tabEquipos;
    @FXML private Tab tabPilotos;
    @FXML private Tab tabVehiculos;
    @FXML private Tab tabCircuitos;
    @FXML private TeamController equiposViewController;
    @FXML private DriverController pilotosViewController;
    @FXML private VehicleController vehiculosViewController;
    @FXML private CircuitController circuitosViewController;

    @FXML
    public void initialize() {
        secciones.getSelectionModel().selectedItemProperty().addListener((o, anterior, actual) -> {
            refrescarPestana(actual);
        });
    }

    /**
     * Vuelve a cargar la pestaña indicada desde el catálogo en memoria.
     *
     * Cada pestaña de Gestión guarda su tabla una sola vez al construirse; si
     * otra pestaña —o Explorar, o Carrera— crea o edita algo mientras tanto,
     * esta tabla se queda desactualizada hasta que alguien vuelve a pedirle
     * los datos. Se llama al cambiar de pestaña aquí dentro, y también desde
     * fuera (Navigator) al volver a entrar en Gestión.
     */
    void refrescarPestana(Tab pestana) {
        if (pestana == tabEquipos) {
            equiposViewController.refrescarVista();
        } else if (pestana == tabPilotos) {
            pilotosViewController.refrescarVista();
        } else if (pestana == tabVehiculos) {
            vehiculosViewController.refrescarVista();
        } else if (pestana == tabCircuitos) {
            circuitosViewController.refrescarVista();
        }
    }

    /** Refresca la pestaña actualmente visible; lo llama Navigator al volver a Gestión. */
    void refrescarVistaActual() {
        refrescarPestana(secciones.getSelectionModel().getSelectedItem());
    }

    /** Regresa a la lista que originó una comparación conservando filtros y selección. */
    public void mostrarVehiculos() {
        secciones.getSelectionModel().select(tabVehiculos);
    }

    public void mostrarCircuitos() {
        secciones.getSelectionModel().select(tabCircuitos);
    }
}
