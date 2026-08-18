package com.formula1.util;

import javafx.geometry.Rectangle2D;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El recorte se comprueba sobre la geometría, sin entorno gráfico: es donde
 * estaba el fallo que deformaba y desbordaba las fotos.
 */
class ImageCropTest {

    private static final double CAJA_ANCHO = 242;
    private static final double CAJA_ALTO = 170;
    private static final double PRECISION = 0.001;

    @Test
    void elRecorteSiempreTieneLaProporcionDeLaCaja() {
        // Las fotos reales del proyecto van de 224x224 a 3444x4429.
        double[][] tamanos = {{224, 224}, {3444, 4429}, {1200, 800}, {656, 875}, {4402, 2476}};
        double esperada = CAJA_ANCHO / CAJA_ALTO;

        for (double[] tamano : tamanos) {
            Rectangle2D recorte = ImageCrop.recorteDe(tamano[0], tamano[1],
                    CAJA_ANCHO, CAJA_ALTO, ImageCrop.CENTRADO);
            assertEquals(esperada, recorte.getWidth() / recorte.getHeight(), PRECISION,
                    "La proporcion no cuadra para " + tamano[0] + "x" + tamano[1]);
        }
    }

    @Test
    void elRecorteNuncaSaleDeLaImagenDeOrigen() {
        double[][] tamanos = {{224, 224}, {3444, 4429}, {1200, 800}, {656, 875}};

        for (double[] tamano : tamanos) {
            Rectangle2D recorte = ImageCrop.recorteDe(tamano[0], tamano[1],
                    CAJA_ANCHO, CAJA_ALTO, ImageCrop.CENTRADO);
            assertTrue(recorte.getMinX() >= -PRECISION, "Se sale por la izquierda");
            assertTrue(recorte.getMinY() >= -PRECISION, "Se sale por arriba");
            assertTrue(recorte.getMaxX() <= tamano[0] + PRECISION, "Se sale por la derecha");
            assertTrue(recorte.getMaxY() <= tamano[1] + PRECISION, "Se sale por abajo");
        }
    }

    @Test
    void unRetratoSeRecortaPorArribaYUnPanoramicoPorLosLados() {
        // Mas alta que la caja: sobra por arriba y por abajo, no a los lados.
        Rectangle2D retrato = ImageCrop.recorteDe(656, 875, CAJA_ANCHO, CAJA_ALTO, ImageCrop.CENTRADO);
        assertEquals(656, retrato.getWidth(), PRECISION);
        assertEquals(0, retrato.getMinX(), PRECISION);
        assertTrue(retrato.getMinY() > 0);

        // Mas ancha que la caja: sobra a los lados.
        Rectangle2D panoramico = ImageCrop.recorteDe(4402, 2476, CAJA_ANCHO, CAJA_ALTO, ImageCrop.CENTRADO);
        assertEquals(2476, panoramico.getHeight(), PRECISION);
        assertEquals(0, panoramico.getMinY(), PRECISION);
        assertTrue(panoramico.getMinX() > 0);
    }

    @Test
    void elSesgoDeRetratoDejaMasEspacioPorArribaQueElCentrado() {
        Rectangle2D arriba = ImageCrop.recorteDe(656, 875, CAJA_ANCHO, CAJA_ALTO, ImageCrop.SESGO_RETRATO);
        Rectangle2D centrado = ImageCrop.recorteDe(656, 875, CAJA_ANCHO, CAJA_ALTO, ImageCrop.CENTRADO);

        // Cuanto menor es la Y, mas arriba empieza el recorte: es lo que evita
        // cortar la cabeza en los planos cortos.
        assertTrue(arriba.getMinY() < centrado.getMinY());
    }
}
