package com.formula1.util;

import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.Optional;

/**
 * Encaja una imagen en una caja de tamaño fijo recortándola, no estirándola.
 *
 * El material del proyecto tiene proporciones muy dispares —las fotos de
 * pilotos van de 224x224 a 3444x4429 y las de monoplazas llegan a 4400 px de
 * ancho—, así que ajustarlas por deformación se nota de inmediato. Aquí se
 * recorta la imagen de origen a la proporción de la caja y luego se escala,
 * que es el equivalente al {@code object-fit: cover} de la web.
 */
public final class ImageCrop {

    /** Recorte centrado en vertical. */
    public static final double CENTRADO = 0.5;

    /** Pegado al borde superior: en un retrato la cabeza está arriba. */
    public static final double SESGO_RETRATO = 0.1;

    private ImageCrop() {
    }

    /**
     * Carga una imagen del classpath y la encaja en la caja indicada.
     *
     * @param ruta         ruta de classpath, empezando por {@code /}
     * @param ancho        ancho de la caja en píxeles
     * @param alto         alto de la caja en píxeles
     * @param sesgoVertical 0 pega el recorte arriba, 0,5 lo centra
     * @return la vista lista para añadir, o vacío si la imagen no existe
     */
    public static Optional<ImageView> desdeClasspath(String ruta, double ancho, double alto,
                                                     double sesgoVertical) {
        if (ruta == null || ruta.isBlank()) {
            return Optional.empty();
        }
        var recurso = ImageCrop.class.getResourceAsStream(ruta);
        if (recurso == null) {
            return Optional.empty();
        }
        Image imagen = new Image(recurso);
        if (imagen.isError() || imagen.getWidth() <= 0) {
            return Optional.empty();
        }
        return Optional.of(encajar(imagen, ancho, alto, sesgoVertical));
    }

    /**
     * Encaja una imagen ya cargada. El recorte se aplica sobre el origen, no
     * sobre el nodo escalado: así la vista mide exactamente la caja y no
     * puede desbordarla, que es lo que ocurría al recortar el nodo.
     */
    public static ImageView encajar(Image imagen, double ancho, double alto, double sesgoVertical) {
        ImageView vista = new ImageView(imagen);
        vista.setViewport(recorteDe(imagen.getWidth(), imagen.getHeight(), ancho, alto, sesgoVertical));
        vista.setFitWidth(ancho);
        vista.setFitHeight(alto);
        // El recorte ya tiene la proporción exacta de la caja, así que
        // ajustarlo a ella no deforma nada.
        vista.setPreserveRatio(false);
        vista.setSmooth(true);
        return vista;
    }

    /**
     * Zona de la imagen de origen que se conserva. Se expone aparte para
     * poder comprobarla sin necesidad de un entorno gráfico.
     */
    public static Rectangle2D recorteDe(double anchoOrigen, double altoOrigen,
                                        double ancho, double alto, double sesgoVertical) {
        double proporcion = ancho / alto;
        double recorteAncho;
        double recorteAlto;
        if (anchoOrigen / altoOrigen > proporcion) {
            recorteAlto = altoOrigen;                    // sobra a los lados
            recorteAncho = altoOrigen * proporcion;
        } else {
            recorteAncho = anchoOrigen;                  // sobra arriba y abajo
            recorteAlto = anchoOrigen / proporcion;
        }
        return new Rectangle2D(
                (anchoOrigen - recorteAncho) / 2,
                (altoOrigen - recorteAlto) * sesgoVertical,
                recorteAncho, recorteAlto);
    }
}
