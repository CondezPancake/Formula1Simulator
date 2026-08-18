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

    private static final String VISTA_SESION = "simulation";
    private static final String VISTA_GESTION = "gestion";

    private static StackPane contenedor;
    private static Object ultimoControlador;
    private static Node sesion;
    private static Object controladorSesion;
    private static Node gestion;
    private static Object controladorGestion;
    private static Node vistaRetorno;
    private static Object controladorRetorno;

    private Navigator() {
    }

    static void registrar(StackPane centro) {
        // Un shell nuevo delimita un ciclo de vida nuevo de la aplicacion.
        // Evita reutilizar nodos pertenecientes a una escena anterior.
        if (contenedor != centro) {
            sesion = null;
            controladorSesion = null;
            gestion = null;
            controladorGestion = null;
            vistaRetorno = null;
            controladorRetorno = null;
        }
        contenedor = centro;
    }

    /** Carga una vista y la muestra en el centro del shell. */
    public static void ir(String vista) {
        if (contenedor == null) {
            return;
        }
        if (VISTA_SESION.equals(vista) && sesion != null) {
            ultimoControlador = controladorSesion;
            contenedor.getChildren().setAll(sesion);
            return;
        }
        if (VISTA_GESTION.equals(vista) && gestion != null) {
            ultimoControlador = controladorGestion;
            contenedor.getChildren().setAll(gestion);
            return;
        }
        try {
            FXMLLoader cargador = new FXMLLoader(Navigator.class.getResource("/views/" + vista + ".fxml"));
            Node contenido = cargador.load();
            ultimoControlador = cargador.getController();
            if (VISTA_SESION.equals(vista)) {
                // La sesion puede seguir ejecutandose aunque su vista no este
                // visible. Conservar ambos mantiene tarea, bindings y datos
                // exactamente en el punto en que los dejo el usuario.
                sesion = contenido;
                controladorSesion = ultimoControlador;
            } else if (VISTA_GESTION.equals(vista)) {
                gestion = contenido;
                controladorGestion = ultimoControlador;
            }
            contenedor.getChildren().setAll(contenido);
        } catch (Exception e) {
            error("No se pudo abrir la vista «" + vista + "»", e.getMessage());
        }
    }

    /** Abre una vista secundaria recordando exactamente el nodo que la originó. */
    public static void irConRetorno(String vista) {
        if (contenedor == null || contenedor.getChildren().isEmpty()) {
            ir(vista);
            return;
        }
        vistaRetorno = contenedor.getChildren().get(0);
        controladorRetorno = ultimoControlador;
        ir(vista);
    }

    /** Restaura la vista anterior con sus filtros, selección y pestaña intactos. */
    public static boolean volver() {
        if (contenedor == null || vistaRetorno == null) {
            return false;
        }
        Node anterior = vistaRetorno;
        Object controladorAnterior = controladorRetorno;
        vistaRetorno = null;
        controladorRetorno = null;
        contenedor.getChildren().setAll(anterior);
        ultimoControlador = controladorAnterior;
        return true;
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
