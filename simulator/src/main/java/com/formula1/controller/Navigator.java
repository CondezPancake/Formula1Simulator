package com.formula1.controller;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.layout.StackPane;

/**
 * Cambia la vista central del shell.
 *
 * Es un punto de acceso estático para que cualquier controlador pueda
 * navegar (por ejemplo, de la lista de circuitos a su detalle) sin tener
 * que ir pasándose referencias entre pantallas.
 */
public final class Navigator {

    private static StackPane contenedor;
    private static Object ultimoControlador;

    private Navigator() {
    }

    static void registrar(StackPane centro) {
        contenedor = centro;
    }

    /** Carga una vista y la muestra en el centro del shell. */
    public static void ir(String vista) {
        if (contenedor == null) {
            return;
        }
        try {
            FXMLLoader cargador = new FXMLLoader(Navigator.class.getResource("/views/" + vista + ".fxml"));
            Node contenido = cargador.load();
            ultimoControlador = cargador.getController();
            contenedor.getChildren().setAll(contenido);
        } catch (Exception e) {
            error("No se pudo abrir la vista «" + vista + "»", e.getMessage());
        }
    }

    /** Controlador de la última vista cargada, para pasarle datos. */
    public static Object ultimoControlador() {
        return ultimoControlador;
    }

    public static void error(String titulo, String detalle) {
        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setTitle("Error");
        alerta.setHeaderText(titulo);
        alerta.setContentText(detalle);
        alerta.showAndWait();
    }

    public static void aviso(String titulo, String detalle) {
        Alert alerta = new Alert(Alert.AlertType.WARNING);
        alerta.setTitle("Aviso");
        alerta.setHeaderText(titulo);
        alerta.setContentText(detalle);
        alerta.showAndWait();
    }

    public static boolean confirmar(String pregunta) {
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
        alerta.setTitle("Confirmar");
        alerta.setHeaderText(pregunta);
        return alerta.showAndWait().filter(b -> b.getButtonData().isDefaultButton()).isPresent();
    }
}
