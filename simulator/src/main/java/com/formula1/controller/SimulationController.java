package com.formula1.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;

/**
 * Controlador de la vista de simulación de clasificación.
 *
 * El motor de cálculo y la ejecución en segundo plano se implementan en
 * feature/qualifying-engine y feature/ui-simulation; aquí solo queda el
 * enlace con la vista para mantener la aplicación arrancable.
 */
public class SimulationController {

    @FXML
    private Button btnStartQualifying;

    @FXML
    public void initialize() {
        // Sin estado que inicializar todavía.
    }

    @FXML
    private void onStartQualifying() {
        // Pendiente: lanzar la sesión de clasificación en un Task.
    }
}
