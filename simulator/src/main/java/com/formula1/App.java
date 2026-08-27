package com.formula1;

import com.formula1.controller.IntroController;
import com.formula1.controller.MainMenuController;
import com.formula1.controller.ShellController;
import com.formula1.data.DataStore;
import com.formula1.data.MongoConnection;
import com.formula1.util.Animaciones;
import com.formula1.util.Async;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
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

        Node intro = IntroController.crear(() -> mostrarMenu(raizEscena, false));
        raizEscena.getChildren().setAll(intro);
    }

    /**
     * Carga el menú principal. La primera vez (desde la intro) usa el fundido
     * simple de siempre; al volver desde el shell usa el mismo slide de
     * {@link #cruzarSeccion} que la sección, pero en sentido inverso.
     */
    private void mostrarMenu(StackPane raizEscena, boolean regresando) {
        try {
            FXMLLoader cargador = new FXMLLoader(getClass().getResource(VISTA_MENU));
            Parent menu = cargador.load();
            MainMenuController controlador = cargador.getController();
            menuActual = controlador;
            controlador.setAlEntrarAlShell(irA -> mostrarShell(raizEscena, irA));
            if (regresando) {
                cruzarSeccion(raizEscena, menu, false);
            } else {
                cruzar(raizEscena, menu);
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo cargar el menú principal", e);
        }
    }

    /** Carga el shell (ya con su sección inicial resuelta) y hace la transición con slide. */
    private void mostrarShell(StackPane raizEscena, Runnable alTerminarDeCargar) {
        try {
            // Sin esto el slideshow del menú seguiría corriendo durante la sesión.
            if (menuActual != null) {
                menuActual.liberar();
                menuActual = null;
            }
            FXMLLoader cargador = new FXMLLoader(getClass().getResource(VISTA_SHELL));
            Parent shell = cargador.load();
            ShellController controlador = cargador.getController();
            controlador.setAlVolverAlMenu(() -> mostrarMenu(raizEscena, true));
            cruzarSeccion(raizEscena, shell, true);
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
     * Cruce con slide entre el menú y el shell: el entrante llega desde un
     * lado con fundido, el saliente se va hacia el contrario, para que se
     * sienta como continuación del menú y no como un corte a negro.
     * {@code avanzando} decide el sentido; llamar con {@code false} produce
     * la animación inversa (volver al menú).
     */
    private void cruzarSeccion(StackPane raizEscena, Node entrante, boolean avanzando) {
        Node saliente = raizEscena.getChildren().isEmpty() ? null : raizEscena.getChildren().get(0);
        double desplazamiento = avanzando ? 36 : -36;

        entrante.setOpacity(0);
        entrante.setTranslateX(desplazamiento);
        raizEscena.getChildren().add(entrante);

        FadeTransition entradaFade = new FadeTransition(Animaciones.TRANSICION_SECCION, entrante);
        entradaFade.setToValue(1);
        TranslateTransition entradaSlide = new TranslateTransition(Animaciones.TRANSICION_SECCION, entrante);
        entradaSlide.setToX(0);
        entradaSlide.setInterpolator(Animaciones.EASE_OUT);
        ParallelTransition entrada = new ParallelTransition(entradaFade, entradaSlide);

        if (saliente != null) {
            FadeTransition salidaFade = new FadeTransition(Animaciones.TRANSICION_SECCION, saliente);
            salidaFade.setToValue(0);
            TranslateTransition salidaSlide = new TranslateTransition(Animaciones.TRANSICION_SECCION, saliente);
            salidaSlide.setToX(-desplazamiento * 0.4);
            salidaSlide.setInterpolator(Animaciones.EASE_OUT);
            new ParallelTransition(salidaFade, salidaSlide).play();
        }

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
