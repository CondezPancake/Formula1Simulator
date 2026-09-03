package com.formula1.controller;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.layout.StackPane;
import javafx.util.Callback;
import javafx.util.Duration;

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
    /**
     * Explorar es, con diferencia, la vista mas cara de construir: monta las
     * rejillas de tarjetas con sus imagenes. Se cachea como las otras dos, o
     * cada visita la reconstruye entera.
     */
    private static final String VISTA_EXPLORAR = "explorar";

    /**
     * Fábrica de controladores de la Fase 6: la instala {@code App} al
     * arrancar para que las vistas que Navigator carga bajo demanda
     * compartan los mismos servicios que las que carga App directamente, en
     * vez de que cada una construya los suyos. Si nadie la instala (por
     * ejemplo, en un test que no pasa por App), {@link FXMLLoader} cae a su
     * fábrica por defecto —el constructor sin argumentos— como hacía antes.
     */
    private static Callback<Class<?>, Object> fabricaControladores;
    private static StackPane contenedor;
    private static Object ultimoControlador;
    private static Node sesion;
    private static Object controladorSesion;
    private static Node gestion;
    private static Object controladorGestion;
    private static Node explorar;
    private static Object controladorExplorar;
    private static Node vistaRetorno;
    private static Object controladorRetorno;
    private static String vistaRetornoNombre;

    private Navigator() {
    }

    /** Instala la fábrica de controladores compartida (Fase 6, la llama {@code App} al arrancar). */
    public static void usarFabrica(Callback<Class<?>, Object> fabrica) {
        fabricaControladores = fabrica;
    }

    static void registrar(StackPane centro) {
        // Un shell nuevo delimita un ciclo de vida nuevo de la aplicacion.
        // Evita reutilizar nodos pertenecientes a una escena anterior.
        if (contenedor != centro) {
            sesion = null;
            controladorSesion = null;
            gestion = null;
            controladorGestion = null;
            explorar = null;
            controladorExplorar = null;
            vistaRetorno = null;
            controladorRetorno = null;
            vistaRetornoNombre = null;
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
            mostrarVista(sesion);
            prepararSesionPendiente();
            ShellController.sincronizarVista(vista);
            return;
        }
        if (VISTA_GESTION.equals(vista) && gestion != null) {
            ultimoControlador = controladorGestion;
            mostrarVista(gestion);
            ShellController.sincronizarVista(vista);
            return;
        }
        if (VISTA_EXPLORAR.equals(vista) && explorar != null) {
            ultimoControlador = controladorExplorar;
            mostrarVista(explorar);
            ShellController.sincronizarVista(vista);
            return;
        }
        try {
            FXMLLoader cargador = new FXMLLoader(Navigator.class.getResource("/views/" + vista + ".fxml"));
            if (fabricaControladores != null) {
                cargador.setControllerFactory(fabricaControladores);
            }
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
            } else if (VISTA_EXPLORAR.equals(vista)) {
                explorar = contenido;
                controladorExplorar = ultimoControlador;
            }
            mostrarVista(contenido);
            if (VISTA_SESION.equals(vista)) {
                prepararSesionPendiente();
            }
            ShellController.sincronizarVista(vista);
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
        vistaRetornoNombre = ShellController.vistaActual();
        ir(vista);
    }

    /** Restaura la vista anterior con sus filtros, selección y pestaña intactos. */
    public static boolean volver() {
        if (contenedor == null || vistaRetorno == null) {
            return false;
        }
        Node anterior = vistaRetorno;
        Object controladorAnterior = controladorRetorno;
        String seccionAnterior = vistaRetornoNombre;
        vistaRetorno = null;
        controladorRetorno = null;
        vistaRetornoNombre = null;
        mostrarVista(anterior);
        ultimoControlador = controladorAnterior;
        ShellController.sincronizarVista(seccionAnterior);
        return true;
    }

    /**
     * Coloca la vista en el centro del shell con una entrada corta.
     *
     * El fade va sobre el nodo entrante y no encadenado con una salida: las
     * vistas cacheadas se reutilizan, y animar también la saliente dejaría su
     * opacidad a medias la próxima vez que se muestre.
     */
    private static void mostrarVista(Node contenido) {
        contenedor.getChildren().setAll(contenido);
        FadeTransition entrada = new FadeTransition(Duration.millis(200), contenido);
        entrada.setFromValue(0);
        entrada.setToValue(1);
        entrada.setInterpolator(Interpolator.EASE_BOTH);
        entrada.setOnFinished(e -> contenido.setOpacity(1));
        entrada.play();
    }

    private static void prepararSesionPendiente() {
        if (controladorSesion instanceof SimulationController simulacion) {
            simulacion.aplicarConfiguracionGuardadaPendiente();
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
