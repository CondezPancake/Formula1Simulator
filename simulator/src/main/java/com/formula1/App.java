package com.formula1;

import com.formula1.controller.IntroController;
import com.formula1.controller.MainMenuController;
import com.formula1.data.DataStore;
import com.formula1.data.MongoConnection;
import com.formula1.util.Async;

import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;

public class App extends Application {

    private static final String VISTA_MENU = "/views/menu.fxml";
    private static final String VISTA_SHELL = "/views/shell.fxml";
    private static final String HOJA_ESTILOS = "/css/style.css";
    private static final Duration CRUCE = Duration.millis(400);

    /** Se guarda para poder soltar su vídeo al entrar al shell. */
    private MainMenuController menuActual;

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
                return DataStore.getInstance().cargar();
            }
        };
        Async.ejecutar(carga);

        Node intro = IntroController.crear(() -> mostrarMenu(raizEscena));
        raizEscena.getChildren().setAll(intro);
    }

    /** Carga el menú principal y hace un cruce de opacidad con lo que hubiera antes. */
    private void mostrarMenu(StackPane raizEscena) {
        try {
            FXMLLoader cargador = new FXMLLoader(getClass().getResource(VISTA_MENU));
            Parent menu = cargador.load();
            MainMenuController controlador = cargador.getController();
            menuActual = controlador;
            controlador.setAlEntrarAlShell(irA -> mostrarShell(raizEscena, irA));
            cruzar(raizEscena, menu);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo cargar el menú principal", e);
        }
    }

    /** Carga el shell (ya con su sección inicial resuelta) y hace el mismo cruce. */
    private void mostrarShell(StackPane raizEscena, Runnable alTerminarDeCargar) {
        try {
            // Sin esto el vídeo del menú seguiría decodificando durante la sesión.
            if (menuActual != null) {
                menuActual.liberar();
                menuActual = null;
            }
            Parent shell = FXMLLoader.load(getClass().getResource(VISTA_SHELL));
            cruzar(raizEscena, shell);
            alTerminarDeCargar.run();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo cargar la aplicación", e);
        }
    }

    /** Añade {@code entrante} con fade-in mientras desvanece lo que hubiera en la raíz. */
    private void cruzar(StackPane raizEscena, Node entrante) {
        entrante.setOpacity(0);
        raizEscena.getChildren().add(entrante);
        FadeTransition entrada = new FadeTransition(CRUCE, entrante);
        entrada.setToValue(1);
        entrada.setOnFinished(e -> raizEscena.getChildren().remove(0));
        entrada.play();
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

    /** Libera el pool de hilos y la conexión con MongoDB al cerrar. */
    @Override
    public void stop() {
        Async.cerrar();
        MongoConnection.cerrar();
    }
}
