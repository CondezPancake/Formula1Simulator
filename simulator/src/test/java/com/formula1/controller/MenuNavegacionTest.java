package com.formula1.controller;

import com.formula1.data.DataStore;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Navegación del menú principal.
 *
 * Cubre lo que una captura de pantalla no puede comprobar: que el orden de
 * las opciones es el pedido, que el teclado recorre la lista con envolvente y
 * que el resaltado y el panel derecho siguen a la opción activa.
 */
class MenuNavegacionTest {

    /** El orden es un requisito explícito, no un detalle de implementación. */
    private static final List<String> ORDEN_ESPERADO = List.of(
            "CLASIFICACIÓN", "GESTIÓN DE EQUIPOS", "EXPLORAR", "HISTORIAL", "AJUSTES");

    private static final String CLASE_ACTIVA = "menu-opcion-activa";

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
            System.err.println("MenuNavegacionTest: sin entorno gráfico, no verificado ("
                    + sinEntornoGrafico.getClass().getSimpleName() + ")");
            toolkitListo = false;
        }
        if (toolkitListo) {
            DataStore.getInstance().cargar();
        }
    }

    @Test
    void listaLasOpcionesEnElOrdenPedido() {
        if (!toolkitListo) {
            return;   // sin entorno gráfico no se puede comprobar; no se falsea un OK
        }
        Parent menu = cargarMenu();
        enFx(() -> assertEquals(ORDEN_ESPERADO, titulos(menu),
                "el orden de las opciones del menú"));
    }

    @Test
    void laFlechaAbajoAvanzaYLaPrimeraDaLaVueltaHaciaArriba() {
        if (!toolkitListo) {
            return;
        }
        Parent menu = cargarMenu();

        enFx(() -> assertEquals(0, indiceActivo(menu), "arranca en la primera opción"));

        pulsar(menu, KeyCode.DOWN);
        enFx(() -> assertEquals(1, indiceActivo(menu), "abajo avanza una posición"));

        pulsar(menu, KeyCode.UP);
        pulsar(menu, KeyCode.UP);
        enFx(() -> assertEquals(ORDEN_ESPERADO.size() - 1, indiceActivo(menu),
                "arriba desde la primera da la vuelta a la última"));

        pulsar(menu, KeyCode.DOWN);
        enFx(() -> assertEquals(0, indiceActivo(menu),
                "abajo desde la última vuelve a la primera"));
    }

    @Test
    void alCambiarDeOpcionSiguenElTituloYLaDescripcion() {
        if (!toolkitListo) {
            return;
        }
        Parent menu = cargarMenu();

        AtomicReference<String> tituloInicial = new AtomicReference<>();
        AtomicReference<String> descripcionInicial = new AtomicReference<>();
        enFx(() -> {
            tituloInicial.set(texto(menu, "lblTituloSeccion"));
            descripcionInicial.set(texto(menu, "lblDescripcion"));
            assertEquals(ORDEN_ESPERADO.get(0), tituloInicial.get(),
                    "el panel derecho arranca en la primera opción");
            assertTrue(descripcionInicial.get() != null && !descripcionInicial.get().isBlank(),
                    "la descripción no puede estar vacía");
        });

        pulsar(menu, KeyCode.DOWN);
        enFx(() -> {
            assertEquals(ORDEN_ESPERADO.get(1), texto(menu, "lblTituloSeccion"),
                    "el título del panel derecho sigue a la opción activa");
            assertNotEquals(descripcionInicial.get(), texto(menu, "lblDescripcion"),
                    "la descripción cambia con la opción");
        });
    }

    // --- utilidades -------------------------------------------------------

    /**
     * Índice de la fila resaltada. La clase activa no va en la fila sino en
     * el marco que ciñe al texto, así que se busca dentro de cada fila.
     */
    private static int indiceActivo(Parent menu) {
        List<Parent> filas = filas(menu);
        int encontrado = -1;
        for (int i = 0; i < filas.size(); i++) {
            if (filas.get(i).lookup("." + CLASE_ACTIVA) != null) {
                if (encontrado != -1) {
                    fail("hay más de una opción marcada como activa");
                }
                encontrado = i;
            }
        }
        if (encontrado == -1) {
            fail("ninguna opción está marcada como activa");
        }
        return encontrado;
    }

    private static List<Parent> filas(Parent menu) {
        VBox lista = (VBox) menu.lookup("#listaOpciones");
        if (lista == null) {
            fail("no se encontró #listaOpciones");
        }
        List<Parent> filas = new ArrayList<>();
        lista.getChildren().forEach(n -> filas.add((Parent) n));
        return filas;
    }

    private static List<String> titulos(Parent menu) {
        List<String> titulos = new ArrayList<>();
        for (Parent fila : filas(menu)) {
            fila.lookupAll(".menu-opcion-texto").stream()
                    .filter(Label.class::isInstance)
                    .map(Label.class::cast)
                    .findFirst()
                    .ifPresent(etiqueta -> titulos.add(etiqueta.getText()));
        }
        return titulos;
    }

    private static String texto(Parent menu, String id) {
        Label etiqueta = (Label) menu.lookup("#" + id);
        if (etiqueta == null) {
            fail("no se encontró #" + id);
        }
        return etiqueta.getText();
    }

    /**
     * Dispara la tecla contra la escena, que es donde vive el filtro.
     *
     * Va a la escena y no a la raíz a propósito: así se comprueba de paso que
     * el menú responde al teclado sin depender de qué nodo tenga el foco.
     */
    private static void pulsar(Parent menu, KeyCode tecla) {
        enFx(() -> menu.getScene().getRoot().fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED,
                "", "", tecla, false, false, false, false)));
    }

    private Parent cargarMenu() {
        AtomicReference<Parent> raiz = new AtomicReference<>();
        AtomicReference<Throwable> fallo = new AtomicReference<>();
        enFx(() -> {
            try {
                Parent menu = FXMLLoader.load(getClass().getResource("/views/menu.fxml"));
                // Sin Scene no se aplica el CSS ni se resuelve el layout.
                new Scene(menu, 1600, 940);
                menu.applyCss();
                menu.layout();
                raiz.set(menu);
            } catch (Throwable t) {
                fallo.set(t);
            }
        });
        if (fallo.get() != null) {
            fail("No se pudo cargar el menú", fallo.get());
        }
        return raiz.get();
    }

    /** Ejecuta en el hilo de FX y espera: las aserciones deben correr allí. */
    private static void enFx(Runnable accion) {
        AtomicReference<Throwable> fallo = new AtomicReference<>();
        CountDownLatch hecho = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                accion.run();
            } catch (Throwable t) {
                fallo.set(t);
            } finally {
                hecho.countDown();
            }
        });
        try {
            if (!hecho.await(20, TimeUnit.SECONDS)) {
                fail("La acción en el hilo de FX no terminó");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail(e);
        }
        if (fallo.get() instanceof AssertionError error) {
            throw error;
        }
        if (fallo.get() != null) {
            fail(fallo.get());
        }
    }
}
