package com.formula1.controller;

import com.formula1.util.AudioManager;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Panel mínimo de ajustes de audio: volumen de música, de efectos y
 * silenciar. No es un sistema de configuración completo, solo lo que la
 * introducción de sonido en el menú necesita para ser controlable.
 */
public final class AjustesDialog {

    private AjustesDialog() {
    }

    public static void mostrar() {
        VBox raiz = new VBox(16);
        raiz.getStyleClass().add("card");
        raiz.setPadding(new Insets(24));

        Label titulo = new Label("AJUSTES DE SONIDO");
        titulo.getStyleClass().add("view-title");

        Label lblMusica = new Label("Música");
        lblMusica.getStyleClass().add("card-label");
        Slider sliderMusica = new Slider(0, 1, AudioManager.getVolumenMusica());
        sliderMusica.valueProperty().addListener((obs, viejo, nuevo) ->
                AudioManager.setVolumenMusica(nuevo.doubleValue()));

        Label lblSfx = new Label("Efectos");
        lblSfx.getStyleClass().add("card-label");
        Slider sliderSfx = new Slider(0, 1, AudioManager.getVolumenSfx());
        sliderSfx.valueProperty().addListener((obs, viejo, nuevo) ->
                AudioManager.setVolumenSfx(nuevo.doubleValue()));

        CheckBox silenciar = new CheckBox("Silenciar todo");
        silenciar.setSelected(AudioManager.isMute());
        silenciar.selectedProperty().addListener((obs, viejo, nuevo) -> AudioManager.setMute(nuevo));

        raiz.getChildren().addAll(titulo, lblMusica, sliderMusica, lblSfx, sliderSfx, silenciar);

        Stage dialogo = new Stage(StageStyle.UTILITY);
        dialogo.setTitle("Ajustes");
        dialogo.initModality(Modality.APPLICATION_MODAL);

        Scene escena = new Scene(raiz, 320, 280);
        var estilos = AjustesDialog.class.getResource("/css/style.css");
        if (estilos != null) {
            escena.getStylesheets().add(estilos.toExternalForm());
        }
        dialogo.setScene(escena);
        dialogo.showAndWait();
    }
}
