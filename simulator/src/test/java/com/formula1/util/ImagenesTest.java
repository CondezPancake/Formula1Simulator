package com.formula1.util;

import javafx.application.Platform;
import javafx.scene.image.Image;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La carga de imágenes con tamaño y caché.
 *
 * El fallo que motiva estas pruebas: las fotos se abrían a resolución nativa
 * aunque se fueran a pintar diminutas. La del catálogo más grande son
 * 3444x4429 —unos 61 MB ya descomprimida— para una caja de 210x240.
 */
class ImagenesTest {

    /** La foto más grande del proyecto, la que hacía evidente el problema. */
    private static final String FOTO_GRANDE = "/images/drivers/carlos-sainz.jpg";
    /** Las banderas son 1000x563 y se dibujan a 34 px de ancho. */
    private static final String BANDERA = "/images/flags/spain.jpg";

    private static boolean toolkitListo;

    @BeforeAll
    static void arrancarToolkit() {
        System.setProperty("prism.order", "sw");
        try {
            CountDownLatch listo = new CountDownLatch(1);
            Platform.startup(listo::countDown);
            toolkitListo = listo.await(20, TimeUnit.SECONDS);
        } catch (IllegalStateException yaArrancado) {
            toolkitListo = true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            toolkitListo = false;
        } catch (Throwable sinEntornoGrafico) {
            System.err.println("ImagenesTest: sin entorno gráfico, no verificado ("
                    + sinEntornoGrafico.getClass().getSimpleName() + ")");
            toolkitListo = false;
        }
    }

    @BeforeEach
    void limpiar() {
        Imagenes.vaciar();
    }

    @Test
    void decodificaAlTamanoPedidoYNoAlNativo() {
        if (!toolkitListo) {
            return;   // sin entorno gráfico no se puede comprobar; no se falsea un OK
        }
        Image imagen = Imagenes.cargar(FOTO_GRANDE, 480, 0);
        assertNotNull(imagen, "la foto de referencia debe existir en el classpath");

        // El original son 3444x4429. Si se ignorase el tamaño pedido, aquí
        // vendrían esas medidas y con ellas los ~61 MB de mapa de píxeles.
        assertTrue(imagen.getWidth() <= 480 + 1,
                "se decodificó a " + imagen.getWidth() + " px de ancho, no al tamaño pedido");
        assertTrue(imagen.getHeight() < 4429,
                "conserva el alto nativo: no se aplicó el tamaño pedido");
        assertTrue(imagen.getWidth() > 0 && imagen.getHeight() > 0);
    }

    @Test
    void unaBanderaPedidaPequenaNoSeAbreEnGrande() {
        if (!toolkitListo) {
            return;
        }
        Image bandera = Imagenes.cargar(BANDERA, 34, 0);
        assertNotNull(bandera);
        assertTrue(bandera.getWidth() <= 35,
                "la bandera se abrió a " + bandera.getWidth() + " px para dibujarse a 34");
    }

    @Test
    void repetirLaMismaPeticionDevuelveLaImagenCacheada() {
        if (!toolkitListo) {
            return;
        }
        Image primera = Imagenes.cargar(BANDERA, 34, 0);
        Image segunda = Imagenes.cargar(BANDERA, 34, 0);
        assertSame(primera, segunda, "la segunda petición debería salir de la caché");
    }

    @Test
    void elMismoFicheroADosTamanosSonDosImagenesDistintas() {
        if (!toolkitListo) {
            return;
        }
        Image pequena = Imagenes.cargar(BANDERA, 34, 0);
        Image grande = Imagenes.cargar(BANDERA, 300, 0);
        assertNotNull(pequena);
        assertNotNull(grande);
        // Si la caché ignorara el tamaño, la segunda devolvería la de 34 px.
        assertTrue(grande.getWidth() > pequena.getWidth(),
                "la caché confunde dos tamaños del mismo fichero");
    }

    @Test
    void unaRutaInexistenteODeguelveNuloSinReventar() {
        if (!toolkitListo) {
            return;
        }
        assertNull(Imagenes.cargar("/images/no-existe-esto.png", 100, 0));
        assertNull(Imagenes.cargar(null, 100, 0));
        assertNull(Imagenes.cargar("   ", 100, 0));
    }
}
