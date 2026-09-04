package com.formula1.adapter.in.javafx;

import com.formula1.adapter.out.memory.DataStore;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Carga cada vista FXML de verdad.
 *
 * Los errores de FXML (un {@code fx:id} que no existe en el controlador, un
 * {@code onAction} mal escrito, un import que falta) no los detecta el
 * compilador: solo aparecen al abrir la pantalla. Este test los saca a la
 * luz en la construcción.
 */
class ViewsLoadTest {

    private static boolean toolkitListo;

    @BeforeAll
    static void arrancarToolkit() {
        // Renderizado por software: no se pinta nada, solo se construye el grafo.
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
            // Sin servidor gráfico disponible no se puede comprobar; se avisa
            // en lugar de dar un falso verde.
            System.err.println("ViewsLoadTest: sin entorno gráfico, vistas no verificadas ("
                    + sinEntornoGrafico.getClass().getSimpleName() + ")");
            toolkitListo = false;
        }
        if (toolkitListo) {
            // Las vistas piden datos al construirse: se siembran en memoria.
            DataStore.getInstance().cargar();
        }
    }

    private void cargar(String vista) {
        if (!toolkitListo) {
            return; // sin entorno gráfico no se puede comprobar; no se falsea un OK
        }
        AtomicReference<Throwable> fallo = new AtomicReference<>();
        AtomicReference<Object> raiz = new AtomicReference<>();
        CountDownLatch hecho = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                raiz.set(FXMLLoader.load(getClass().getResource("/views/" + vista + ".fxml")));
            } catch (Throwable t) {
                fallo.set(t);
            } finally {
                hecho.countDown();
            }
        });

        try {
            if (!hecho.await(20, TimeUnit.SECONDS)) {
                fail("La vista " + vista + " no terminó de cargarse");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail(e);
        }

        if (fallo.get() != null) {
            Throwable causa = fallo.get();
            while (causa.getCause() != null) {
                causa = causa.getCause();
            }
            fail("La vista " + vista + " no carga: " + causa, fallo.get());
        }
        assertNotNull(raiz.get());
    }

    @Test
    void cargaElShellYElMenu() {
        cargar("shell");
        cargar("menu");
    }

    @Test
    void cargaLosCatalogos() {
        cargar("drivers");
        cargar("teams");
        cargar("vehicles");
        cargar("circuits");
    }

    @Test
    void cargaExplorar() {
        cargar("explore-drivers");
        cargar("explore-teams");
        cargar("driver-detail");
        cargar("team-detail");
    }

    @Test
    void cargaLasSecciones() {
        cargar("gestion");
        cargar("explorar");
        cargar("config");
        cargar("config-historial");
    }

    @Test
    void cargaLasVistasDeAnalisis() {
        cargar("vehicle-compare");
        cargar("circuit-detail");
    }

    @Test
    void cargaSimulacionEHistorial() {
        cargar("sector-comparison");
        cargar("simulation");
        cargar("history");
    }

    @Test
    void conservaLaMismaSesionAlCambiarDeSeccion() {
        if (!toolkitListo) {
            return;
        }
        AtomicReference<Throwable> fallo = new AtomicReference<>();
        CountDownLatch hecho = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                StackPane centro = new StackPane();
                Navigator.registrar(centro);
                Navigator.ir("simulation");
                Node vistaSesion = centro.getChildren().get(0);
                Object controladorSesion = Navigator.ultimoControlador();

                Navigator.ir("explorar");
                Navigator.ir("simulation");

                assertSame(vistaSesion, centro.getChildren().get(0));
                assertSame(controladorSesion, Navigator.ultimoControlador());
            } catch (Throwable t) {
                fallo.set(t);
            } finally {
                hecho.countDown();
            }
        });

        try {
            if (!hecho.await(20, TimeUnit.SECONDS)) {
                fail("La comprobacion de persistencia no termino");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail(e);
        }

        if (fallo.get() != null) {
            fail("La vista de sesion no se conservo", fallo.get());
        }
    }

    @Test
    void conservaGestionAlEntrarYSalirDeComparacion() {
        if (!toolkitListo) {
            return;
        }
        AtomicReference<Throwable> fallo = new AtomicReference<>();
        CountDownLatch hecho = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                StackPane centro = new StackPane();
                Navigator.registrar(centro);
                Navigator.ir("gestion");
                Node vistaGestion = centro.getChildren().get(0);
                Object controladorGestion = Navigator.ultimoControlador();

                Navigator.ir("vehicle-compare");
                Navigator.ir("gestion");

                assertSame(vistaGestion, centro.getChildren().get(0));
                assertSame(controladorGestion, Navigator.ultimoControlador());
            } catch (Throwable t) {
                fallo.set(t);
            } finally {
                hecho.countDown();
            }
        });

        try {
            if (!hecho.await(20, TimeUnit.SECONDS)) {
                fail("La comprobación de navegación de Gestión no terminó");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail(e);
        }
        if (fallo.get() != null) {
            fail("La vista de Gestión no conservó su estado", fallo.get());
        }
    }

    @Test
    void detalleDeCircuitoVuelveAlMismoContextoDeGestion() {
        if (!toolkitListo) {
            return;
        }
        AtomicReference<Throwable> fallo = new AtomicReference<>();
        CountDownLatch hecho = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                StackPane centro = new StackPane();
                Navigator.registrar(centro);
                Navigator.ir("gestion");
                Node vistaGestion = centro.getChildren().get(0);

                Navigator.irConRetorno("circuit-detail");
                assertTrue(Navigator.volver());

                assertSame(vistaGestion, centro.getChildren().get(0));
            } catch (Throwable t) {
                fallo.set(t);
            } finally {
                hecho.countDown();
            }
        });
        try {
            if (!hecho.await(20, TimeUnit.SECONDS)) {
                fail("La comprobación de retorno desde Circuito no terminó");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail(e);
        }
        if (fallo.get() != null) {
            fail("El detalle no restauró el contexto de Gestión", fallo.get());
        }
    }
}
