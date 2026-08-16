package com.formula1;

import com.formula1.data.MongoConnection;
import com.formula1.util.Async;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class App extends Application {

    private static final String VISTA_PRINCIPAL = "/views/shell.fxml";
    private static final String HOJA_ESTILOS = "/css/style.css";

    @Override
    public void start(Stage escenario) throws IOException {
        Parent raiz = FXMLLoader.load(getClass().getResource(VISTA_PRINCIPAL));
        Scene escena = new Scene(raiz);

        URL estilos = getClass().getResource(HOJA_ESTILOS);
        if (estilos != null) {
            escena.getStylesheets().add(estilos.toExternalForm());
        }

        escenario.setTitle("Formula 1 Simulator");
        escenario.setScene(escena);
        escenario.setMinWidth(980);
        escenario.setMinHeight(640);
        escenario.show();
    }

    /** Libera el pool de hilos y la conexión con MongoDB al cerrar. */
    @Override
    public void stop() {
        Async.cerrar();
        MongoConnection.cerrar();
    }
}
