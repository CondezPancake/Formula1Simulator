package com.formula1.adapter.in.javafx;

import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

/** Permite llegar a una sub-pestaña concreta de Config. &amp; Historial desde fuera. */
public class ConfigHistorialController {

    @FXML private TabPane secciones;
    @FXML private Tab tabConfiguracion;
    @FXML private Tab tabHistorial;

    public void mostrarHistorial() {
        secciones.getSelectionModel().select(tabHistorial);
    }

    public void mostrarConfiguracion() {
        secciones.getSelectionModel().select(tabConfiguracion);
    }
}
