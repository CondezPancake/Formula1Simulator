package com.formula1.controller;

import com.formula1.util.TeamColors;
import com.formula1.util.VehicleImages;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.List;
import java.util.Locale;

/**
 * Visor de las distintas vistas de un monoplaza.
 *
 * Se abre al pulsar la foto de una tarjeta del garaje. Es una ventana modal
 * en lugar de una pantalla del navegador porque no es un destino al que se
 * navegue: se mira y se cierra, y al cerrarla el catálogo sigue como estaba.
 */
final class VehicleGallery {

    private static final double ANCHO_VISOR = 900;
    private static final double ALTO_VISOR = 520;

    private final List<String> vistas;
    private final String modelo;
    private final String equipo;
    private final String color;
    private int indice;

    private final ImageView lienzo = new ImageView();
    private final Label contador = new Label();

    private VehicleGallery(String modelo, String equipo, List<String> vistas) {
        this.modelo = modelo;
        this.equipo = equipo;
        this.vistas = vistas;
        this.color = TeamColors.hex(equipo);
    }

    /** Abre el visor si el modelo tiene imágenes; si no, no hace nada. */
    static void abrir(String modelo, String equipo) {
        List<String> vistas = VehicleImages.de(modelo);
        if (vistas.isEmpty()) {
            return;
        }
        new VehicleGallery(modelo, equipo, vistas).mostrar();
    }

    private void mostrar() {
        Stage ventana = new Stage();
        ventana.initModality(Modality.APPLICATION_MODAL);
        ventana.initStyle(StageStyle.UNDECORATED);
        ventana.setTitle(modelo);

        BorderPane raiz = new BorderPane();
        raiz.getStyleClass().add("gallery-root");
        raiz.setTop(cabecera(ventana));
        raiz.setCenter(visor());
        raiz.setBottom(controles());

        Scene escena = new Scene(raiz, ANCHO_VISOR, ALTO_VISOR);
        var estilos = getClass().getResource("/css/style.css");
        if (estilos != null) {
            escena.getStylesheets().add(estilos.toExternalForm());
        }
        // Salir con Escape y pasar vistas con las flechas: en un visor se
        // espera poder moverse sin buscar los botones.
        escena.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                ventana.close();
            } else if (e.getCode() == KeyCode.LEFT) {
                mover(-1);
            } else if (e.getCode() == KeyCode.RIGHT) {
                mover(1);
            }
        });

        pintar();
        ventana.showAndWait();
    }

    private HBox cabecera(Stage ventana) {
        Label titulo = new Label(modelo);
        titulo.getStyleClass().add("gallery-title");

        Label escuderia = new Label(equipo == null ? "" : equipo.toUpperCase(Locale.ROOT));
        escuderia.getStyleClass().add("gallery-team");
        escuderia.setStyle("-fx-text-fill: " + color + ";");

        VBox textos = new VBox(2, titulo, escuderia);

        Region relleno = new Region();
        HBox.setHgrow(relleno, javafx.scene.layout.Priority.ALWAYS);

        Button cerrar = new Button("CERRAR ✕");
        cerrar.getStyleClass().add("icon-button");
        cerrar.setOnAction(e -> ventana.close());

        HBox barra = new HBox(12, textos, relleno, cerrar);
        barra.getStyleClass().add("gallery-header");
        barra.setAlignment(Pos.CENTER_LEFT);
        barra.setPadding(new Insets(16, 20, 16, 20));
        barra.setStyle("-fx-border-color: transparent transparent " + color + " transparent;"
                + " -fx-border-width: 0 0 2 0;");
        return barra;
    }

    private StackPane visor() {
        lienzo.setPreserveRatio(true);
        lienzo.setSmooth(true);
        lienzo.setFitWidth(ANCHO_VISOR - 60);
        lienzo.setFitHeight(ALTO_VISOR - 170);

        StackPane marco = new StackPane(lienzo);
        marco.getStyleClass().add("gallery-stage");
        marco.setPadding(new Insets(14));
        return marco;
    }

    private HBox controles() {
        Button anterior = new Button("‹ ANTERIOR");
        anterior.getStyleClass().add("icon-button");
        anterior.setOnAction(e -> mover(-1));

        Button siguiente = new Button("SIGUIENTE ›");
        siguiente.getStyleClass().add("icon-button");
        siguiente.setOnAction(e -> mover(1));

        contador.getStyleClass().add("gallery-counter");

        HBox barra = new HBox(14, anterior, contador, siguiente);
        barra.setAlignment(Pos.CENTER);
        barra.setPadding(new Insets(14, 20, 20, 20));
        // Con una sola vista no hay nada entre lo que moverse.
        boolean variasVistas = vistas.size() > 1;
        anterior.setVisible(variasVistas);
        siguiente.setVisible(variasVistas);
        return barra;
    }

    /** Avanza de forma circular: del final vuelve al principio. */
    private void mover(int paso) {
        if (vistas.size() <= 1) {
            return;
        }
        indice = Math.floorMod(indice + paso, vistas.size());
        pintar();
    }

    private void pintar() {
        var recurso = getClass().getResourceAsStream(vistas.get(indice));
        if (recurso != null) {
            lienzo.setImage(new Image(recurso));
        }
        contador.setText((indice + 1) + " / " + vistas.size());
    }
}
