package com.formula1;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class App extends Application {

    private static final String DASHBOARD_VIEW = "/views/dashboard.fxml";
    private static final String STYLESHEET = "/css/style.css";

    @Override
    public void start(Stage stage) throws IOException {
        Parent root = loadView(DASHBOARD_VIEW);
        Scene scene = new Scene(root, 1024, 768);

        URL stylesheet = getClass().getResource(STYLESHEET);
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        }

        stage.setTitle("Formula 1 Qualifying Simulator");
        stage.setScene(scene);
        stage.show();
    }

    private Parent loadView(String fxmlPath) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        return loader.load();
    }
}
