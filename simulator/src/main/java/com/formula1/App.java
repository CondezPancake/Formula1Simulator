package com.formula1;

import com.formula1.data.MongoConnection;
import com.formula1.util.Async;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class App extends Application {

    private static final String VISTA_PRINCIPAL = "/views/shell.fxml";
    private static final String HOJA_ESTILOS = "/css/style.css";

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

        Parent raiz = FXMLLoader.load(getClass().getResource(VISTA_PRINCIPAL));
        Scene escena = new Scene(raiz);

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
