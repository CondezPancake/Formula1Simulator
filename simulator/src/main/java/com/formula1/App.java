package com.formula1;

import com.formula1.controller.IntroController;
import com.formula1.controller.MainMenuController;
import com.formula1.controller.Navigator;
import com.formula1.controller.ShellController;
import com.formula1.util.Async;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class App extends Application {

    private static final String VISTA_MENU = "/views/menu.fxml";
    private static final String VISTA_SHELL = "/views/shell.fxml";
    private static final String HOJA_ESTILOS = "/css/style.css";

    /** Se guarda para que suelte su reloj al volver al menú. */
    private ShellController shellActual;

    /** Único punto de construcción de servicios y controladores (Fase 6). */
    private final AppComposition composicion = new AppComposition();

    /**
     * Titillium Web fue la tipografia oficial de la F1 entre 2013 y 2017 y es
     * la base de la actual, que es propietaria. Se empaqueta con la app porque
     * la hoja de estilos la pide por nombre y no se puede dar por instalada.
     */
    private static final String[] FUENTES = {
        "/fonts/TitilliumWeb-Regular.ttf",
        "/fonts/TitilliumWeb-SemiBold.ttf",
        "/fonts/TitilliumWeb-Bold.ttf",
        "/fonts/TitilliumWeb-Black.ttf",
    };

    @Override
    public void start(Stage escenario) throws IOException {
        cargarFuentes();
        // Antes de la primera navegación, para que toda vista cargada por
        // Navigator comparta los mismos servicios que las que carga App.
        Navigator.usarFabrica(composicion.controllerFactory());

        StackPane raizEscena = new StackPane();
        raizEscena.setStyle("-fx-background-color: -fx-bg-0;");
        Scene escena = new Scene(raizEscena);

        URL estilos = getClass().getResource(HOJA_ESTILOS);
        if (estilos != null) {
            escena.getStylesheets().add(estilos.toExternalForm());
        }

        escenario.setTitle("Formula 1 Simulator");
        escenario.setScene(escena);
        // El diseño está trazado sobre 1920 de ancho: por debajo de ~1280 la
        // cabecera y las grillas de tarjetas se apelmazan.
        escenario.setMinWidth(1280);
        escenario.setMinHeight(800);
        escenario.setMaximized(true);
        escenario.show();

        // La carga de datos arranca en paralelo a la intro, una única vez:
        // DataStore.cargar() no es idempotente, así que nada más debe
        // volver a invocarla (el shell la salta si ya terminó).
        Task<String> carga = new Task<>() {
            @Override
            protected String call() {
                return composicion.datos().cargar();
            }
        };
        Async.ejecutar(carga);

        Node intro = IntroController.crear(() -> mostrarMenu(raizEscena));
        raizEscena.getChildren().setAll(intro);
    }

    /** Carga el menú principal y lo pone en pantalla. */
    private void mostrarMenu(StackPane raizEscena) {
        try {
            if (shellActual != null) {
                shellActual.liberar();
                shellActual = null;
            }
            FXMLLoader cargador = new FXMLLoader(getClass().getResource(VISTA_MENU));
            cargador.setControllerFactory(composicion.controllerFactory());
            Parent menu = cargador.load();
            MainMenuController controlador = cargador.getController();
            controlador.setAlEntrarAlShell(irA -> mostrarShell(raizEscena, irA));
            mostrar(raizEscena, menu);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo cargar el menú principal", e);
        }
    }

    /** Carga el shell ya con su sección inicial resuelta. */
    private void mostrarShell(StackPane raizEscena, Runnable alTerminarDeCargar) {
        try {
            FXMLLoader cargador = new FXMLLoader(getClass().getResource(VISTA_SHELL));
            cargador.setControllerFactory(composicion.controllerFactory());
            Parent shell = cargador.load();
            ShellController controlador = cargador.getController();
            shellActual = controlador;
            controlador.setAlVolverAlMenu(() -> mostrarMenu(raizEscena));
            // Antes de mostrarlo, para que aparezca ya en la sección elegida
            // en vez de asomar en Carrera y saltar acto seguido.
            controlador.arrancar(alTerminarDeCargar);
            mostrar(raizEscena, shell);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo cargar la aplicación", e);
        }
    }

    /**
     * Cambio de pantalla, sin transición.
     *
     * Es un relevo seco a propósito: la intro se desvanece ella sola antes de
     * llamar a su callback, así que el corte no llega a verse.
     */
    private void mostrar(StackPane raizEscena, Node pantalla) {
        raizEscena.getChildren().setAll(pantalla);
    }

    /**
     * Registra las tipografias empaquetadas antes de aplicar el CSS: si se
     * cargan despues, las reglas que las nombran ya han caido al fallback.
     */
    private void cargarFuentes() {
        for (String fuente : FUENTES) {
            try (var flujo = getClass().getResourceAsStream(fuente)) {
                if (flujo != null) {
                    Font.loadFont(flujo, 12);
                }
            } catch (IOException e) {
                // Sin la fuente la app sigue siendo usable: el CSS declara
                // alternativas del sistema para cada familia.
            }
        }
    }

    /** Libera el pool de hilos al cerrar. Las conexiones JDBC son de corta vida. */
    @Override
    public void stop() {
        Async.cerrar();
    }
}
