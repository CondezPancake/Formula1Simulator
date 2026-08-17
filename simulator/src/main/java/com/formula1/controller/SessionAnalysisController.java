package com.formula1.controller;

import com.formula1.model.SessionAnalysis;
import com.formula1.util.FormatUtils;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

/** Presenta el diagnostico automatico generado por el motor de simulacion. */
public class SessionAnalysisController {

    @FXML private Label lblPole;
    @FXML private Label lblTiempo;
    @FXML private Label lblParticipantes;
    @FXML private Label lblResumen;
    @FXML private ListView<String> listaPositivos;
    @FXML private ListView<String> listaNegativos;
    @FXML private ListView<String> listaClaves;

    @FXML
    public void initialize() {
        reiniciar();
    }

    public void reiniciar() {
        cargar(SessionAnalysis.vacio());
    }

    public void cargar(SessionAnalysis analisis) {
        SessionAnalysis seguro = analisis == null ? SessionAnalysis.vacio() : analisis;
        lblPole.setText(seguro.pilotoPole());
        lblTiempo.setText(seguro.tieneResultados()
                ? FormatUtils.formatLapTime(seguro.tiempoPoleSegundos()) : "--");
        lblParticipantes.setText(String.valueOf(seguro.participantesValidos()));
        lblResumen.setText(seguro.resumen());
        listaPositivos.setItems(FXCollections.observableArrayList(seguro.factoresPositivos()));
        listaNegativos.setItems(FXCollections.observableArrayList(seguro.factoresNegativos()));
        listaClaves.setItems(FXCollections.observableArrayList(seguro.factoresClave()));
    }
}
