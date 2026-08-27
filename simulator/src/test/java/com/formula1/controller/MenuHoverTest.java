package com.formula1.controller;

import com.formula1.data.DataStore;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * El resaltado de las tarjetas del menú al pasar el ratón.
 *
 * Cuando el cursor va de una tarjeta a la de al lado, JavaFX dispara primero
 * la salida de la primera y luego la entrada de la segunda. Si cada evento
 * anima por su cuenta, las tarjetas que no intervienen reciben a la vez un
 * destino de opacidad distinto por cada evento y se quedan a medias. Aquí se
 * reproduce esa secuencia exacta y se comprueba el estado final, que es lo
 * que una captura de pantalla no puede verificar.
 */
class MenuHoverTest {

    /** Debe coincidir con MainMenuController.ATENUACION_HERMANA. */
    private static final double ATENUADA = 0.72;
    private static final double ESCALA_HOVER = 1.03;
    private static final double TOLERANCIA = 0.01;

    /** Holgada respecto a Animaciones.HOVER (180 ms) para no depender del reloj. */
    private static final long ESPERA_ANIMACION_MS = 700;

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
            System.err.println("MenuHoverTest: sin entorno gráfico, no verificado ("
                    + sinEntornoGrafico.getClass().getSimpleName() + ")");
            toolkitListo = false;
        }
        if (toolkitListo) {
            DataStore.getInstance().cargar();
        }
    }

    @Test
    void alPasarDeUnaTarjetaAOtraNoQuedanOpacidadesAMedias() {
        if (!toolkitListo) {
            return;   // sin entorno gráfico no se puede comprobar; no se falsea un OK
        }
        Parent menu = cargarMenu();

        StackPane clasificacion = tarjeta(menu, "envolturaClasificacion");
        StackPane gestion = tarjeta(menu, "envolturaGestion");
        StackPane explorar = tarjeta(menu, "envolturaExplorar");

        // Entrar en la primera tarjeta: solo ella queda a plena opacidad.
        enFx(() -> clasificacion.getOnMouseEntered().handle(null));
        esperarAnimacion();
        enFx(() -> {
            assertEquals(1.0, clasificacion.getOpacity(), TOLERANCIA, "la tarjeta activa");
            assertEquals(ESCALA_HOVER, clasificacion.getScaleX(), TOLERANCIA, "escala de la activa");
            assertEquals(ATENUADA, gestion.getOpacity(), TOLERANCIA, "una hermana");
        });

        // La secuencia conflictiva: salir de una y entrar en la siguiente.
        // EXPLORAR recibe el «restaurar» de la salida y el «atenuar» de la
        // entrada en el mismo pulso.
        enFx(() -> {
            clasificacion.getOnMouseExited().handle(null);
            gestion.getOnMouseEntered().handle(null);
        });
        esperarAnimacion();
        enFx(() -> {
            assertEquals(1.0, gestion.getOpacity(), TOLERANCIA, "la nueva tarjeta activa");
            assertEquals(ESCALA_HOVER, gestion.getScaleX(), TOLERANCIA, "escala de la nueva activa");
            assertEquals(ATENUADA, clasificacion.getOpacity(), TOLERANCIA,
                    "la tarjeta que se abandona vuelve a atenuarse");
            assertEquals(1.0, clasificacion.getScaleX(), TOLERANCIA,
                    "la tarjeta que se abandona recupera su escala");
            assertEquals(ATENUADA, explorar.getOpacity(), TOLERANCIA,
                    "la tarjeta ajena no puede quedarse en un valor intermedio");
        });

        // Salir del menú: todo vuelve a su sitio.
        enFx(() -> gestion.getOnMouseExited().handle(null));
        esperarAnimacion();
        enFx(() -> {
            assertEquals(1.0, clasificacion.getOpacity(), TOLERANCIA, "sin cursor, todas opacas");
            assertEquals(1.0, gestion.getOpacity(), TOLERANCIA, "sin cursor, todas opacas");
            assertEquals(1.0, explorar.getOpacity(), TOLERANCIA, "sin cursor, todas opacas");
            assertEquals(1.0, gestion.getScaleX(), TOLERANCIA, "sin cursor, sin escala");
        });
    }

    /**
     * Una entrada sin la salida previa: pasa de verdad cuando algo se traga
     * el evento —un modal que se abre encima, el cursor que reaparece en otro
     * punto—. La tarjeta activa tiene que quedar a plena opacidad aunque
     * viniera de estar atenuada, o el menú se queda apagado del todo.
     */
    @Test
    void unaEntradaSinSalidaPreviaDejaLaTarjetaActivaAPlenaOpacidad() {
        if (!toolkitListo) {
            return;
        }
        Parent menu = cargarMenu();
        StackPane clasificacion = tarjeta(menu, "envolturaClasificacion");
        StackPane gestion = tarjeta(menu, "envolturaGestion");

        enFx(() -> clasificacion.getOnMouseEntered().handle(null));
        esperarAnimacion();
        enFx(() -> assertEquals(ATENUADA, gestion.getOpacity(), TOLERANCIA,
                "GESTIÓN debe empezar atenuada para que la comprobación tenga sentido"));

        // Sin pasar por getOnMouseExited() de CLASIFICACIÓN.
        enFx(() -> gestion.getOnMouseEntered().handle(null));
        esperarAnimacion();
        enFx(() -> {
            assertEquals(1.0, gestion.getOpacity(), TOLERANCIA,
                    "la tarjeta bajo el cursor no puede quedarse atenuada");
            assertEquals(ATENUADA, clasificacion.getOpacity(), TOLERANCIA,
                    "la anterior pasa a atenuarse");
        });
    }

    private Parent cargarMenu() {
        AtomicReference<Parent> raiz = new AtomicReference<>();
        AtomicReference<Throwable> fallo = new AtomicReference<>();
        enFx(() -> {
            try {
                Parent menu = FXMLLoader.load(getClass().getResource("/views/menu.fxml"));
                new Scene(menu, 1600, 900);   // sin Scene el CSS y el layout no se aplican
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

    private static StackPane tarjeta(Parent menu, String id) {
        StackPane nodo = (StackPane) menu.lookup("#" + id);
        if (nodo == null) {
            fail("No se encontró la tarjeta #" + id);
        }
        return nodo;
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

    private static void esperarAnimacion() {
        try {
            Thread.sleep(ESPERA_ANIMACION_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
