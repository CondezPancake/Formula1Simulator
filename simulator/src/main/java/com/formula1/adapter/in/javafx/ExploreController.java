package com.formula1.adapter.in.javafx;

import javafx.fxml.FXML;

/** Coordina el refresco de las pestañas de Explorar (Pilotos y Teams). */
public class ExploreController {

    @FXML private ExploreDriversController pilotosViewController;
    @FXML private ExploreTeamsController equiposViewController;

    /** Vuelve a consultar los catálogos de ambas pestañas; lo llama Navigator al entrar en Explorar. */
    void refrescarTodo() {
        pilotosViewController.refrescar();
        equiposViewController.refrescar();
    }
}
