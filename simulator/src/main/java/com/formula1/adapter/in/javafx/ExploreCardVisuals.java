package com.formula1.adapter.in.javafx;

import com.formula1.domain.model.Circuit;
import com.formula1.domain.model.Vehicle;
import com.formula1.util.ImageCrop;
import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Text;
import java.net.URL;
import java.util.Locale;

/** Construye la cabecera visual de las tarjetas sin depender de la red. */
final class ExploreCardVisuals {
    private static final double WIDTH = 242;
    private static final double HEIGHT = 122;

    private ExploreCardVisuals() { }

    static StackPane vehiculo(Vehicle vehiculo, String colorEquipo) {
        StackPane visual = base();
        // La foto de un monoplaza se ve mejor a sangre: se recorta para
        // llenar la tarjeta en vez de dejar franjas a los lados. El dibujo
        // solo aparece si no hay foto, porque si no se transparenta debajo.
        if (!agregarFoto(visual, vehiculo.getImagen())) {
            visual.getChildren().add(monoplaza(colorEquipo, vehiculo.getModelo()));
        }
        return visual;
    }

    static StackPane circuito(Circuit circuito) {
        StackPane visual = base();
        // Un trazado hay que verlo entero: recortarlo le cortaria curvas, asi
        // que aqui si se ajusta con franjas en vez de recortar. El dibujo de
        // respaldo solo se pinta si no hay imagen, porque los PNG de trazado
        // tienen transparencia y dejarian ver el vector por debajo.
        if (!agregarImagen(visual, circuito.getImagen())) {
            visual.getChildren().add(trazado(circuito.getNombre()));
        }
        return visual;
    }

    private static StackPane base() {
        StackPane visual = new StackPane();
        visual.getStyleClass().add("explore-card-photo");
        visual.setPrefSize(WIDTH, HEIGHT);
        visual.setMinSize(WIDTH, HEIGHT);
        visual.setMaxSize(WIDTH, HEIGHT);
        return visual;
    }

    /** Convierte los mapas SVG de Wikimedia en su miniatura PNG para JavaFX. */
    static String fuenteCompatible(String fuente) {
        if (fuente == null || fuente.isBlank()) return null;
        String limpia = fuente.trim();
        if (limpia.startsWith("/") && !limpia.startsWith("//")) {
            URL recurso = ExploreCardVisuals.class.getResource(limpia);
            return recurso == null ? null : recurso.toExternalForm();
        }
        if (limpia.startsWith("https://upload.wikimedia.org/wikipedia/") && limpia.endsWith(".svg")) {
            int slash = limpia.lastIndexOf('/');
            String archivo = limpia.substring(slash + 1);
            return limpia.substring(0, slash)
                    .replace("/wikipedia/commons/", "/wikipedia/commons/thumb/")
                    + "/" + archivo + "/640px-" + archivo + ".png";
        }
        return limpia;
    }

    /**
     * Rellena la tarjeta recortando la foto, sin deformarla ni dejar franjas.
     *
     * @return {@code true} si la foto se pudo cargar y ya ocupa la tarjeta
     */
    private static boolean agregarFoto(StackPane contenedor, String fuente) {
        String compatible = fuenteCompatible(fuente);
        if (compatible == null) {
            return false;
        }
        // Carga sincrona a proposito: hay que saber ya si existe para decidir
        // entre la foto y el dibujo de respaldo.
        Image imagen = new Image(compatible, WIDTH * 2, HEIGHT * 2, true, true, false);
        if (imagen.isError() || imagen.getWidth() <= 0) {
            return false;
        }
        ImageView vista = ImageCrop.encajar(imagen, WIDTH, HEIGHT, ImageCrop.CENTRADO);
        vista.getStyleClass().add("explore-card-image");
        contenedor.getChildren().add(vista);
        return true;
    }

    /**
     * Ajusta la imagen dentro de la tarjeta sin recortarla.
     *
     * @return {@code true} si la imagen se pudo cargar
     */
    private static boolean agregarImagen(StackPane contenedor, String fuente) {
        String compatible = fuenteCompatible(fuente);
        if (compatible == null) {
            return false;
        }
        Image imagen = new Image(compatible, WIDTH - 22, HEIGHT - 18, true, true, false);
        if (imagen.isError() || imagen.getWidth() <= 0) {
            return false;
        }
        ImageView vista = new ImageView(imagen);
        vista.setFitWidth(WIDTH - 22);
        vista.setFitHeight(HEIGHT - 18);
        vista.setPreserveRatio(true);
        vista.setSmooth(true);
        vista.getStyleClass().add("explore-card-image");
        contenedor.getChildren().add(vista);
        return true;
    }

    private static Pane monoplaza(String colorEquipo, String modelo) {
        Pane lienzo = new Pane();
        lienzo.setPrefSize(WIDTH, HEIGHT);
        Color color = Color.web(colorEquipo);
        Rectangle halo = new Rectangle(18, 22, WIDTH - 36, HEIGHT - 44);
        halo.setArcWidth(32); halo.setArcHeight(32);
        halo.setFill(Color.color(color.getRed(), color.getGreen(), color.getBlue(), 0.10));
        SVGPath carroceria = new SVGPath();
        carroceria.setContent("M 46 59 L 72 51 L 91 34 L 151 34 L 169 51 L 196 59 "
                + "L 196 69 L 164 72 L 150 88 L 92 88 L 78 72 L 46 69 Z");
        carroceria.setFill(color); carroceria.setStroke(Color.web("#F4F4F6")); carroceria.setStrokeWidth(1.2);
        Rectangle aleron = new Rectangle(72, 27, 98, 7);
        aleron.setArcWidth(4); aleron.setFill(color.darker());
        Circle cockpit = new Circle(121, 55, 12, Color.web("#16161B"));
        cockpit.setStroke(Color.web("#6E6E78"));
        Rectangle ruedaIzq = new Rectangle(54, 45, 18, 35);
        Rectangle ruedaDer = new Rectangle(170, 45, 18, 35);
        ruedaIzq.setArcWidth(7); ruedaDer.setArcWidth(7);
        ruedaIzq.setFill(Color.web("#09090B")); ruedaDer.setFill(Color.web("#09090B"));
        Text etiqueta = new Text(modelo == null ? "MONOPLAZA" : modelo);
        etiqueta.setFill(Color.web("#F4F4F6"));
        etiqueta.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
        etiqueta.setX(14); etiqueta.setY(112);
        lienzo.getChildren().addAll(halo, ruedaIzq, ruedaDer, carroceria, aleron, cockpit, etiqueta);
        return lienzo;
    }

    private static Group trazado(String nombre) {
        SVGPath pista = new SVGPath();
        pista.setContent("M 42 73 C 30 43, 65 20, 96 32 C 120 42, 126 18, 161 27 "
                + "C 202 38, 211 78, 179 91 C 151 102, 133 77, 105 88 C 76 99, 52 96, 42 73 Z");
        pista.setFill(Color.TRANSPARENT); pista.setStroke(Color.web("#E10600")); pista.setStrokeWidth(6);
        Text etiqueta = new Text(nombre == null ? "CIRCUITO" : nombre.toUpperCase(Locale.ROOT));
        etiqueta.setFill(Color.web("#C8C8D0"));
        etiqueta.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;");
        etiqueta.setX(15); etiqueta.setY(116);
        return new Group(pista, etiqueta);
    }
}
