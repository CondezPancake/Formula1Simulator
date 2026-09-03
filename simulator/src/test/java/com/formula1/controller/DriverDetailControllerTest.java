package com.formula1.controller;

import com.formula1.data.DataStore;
import com.formula1.domain.model.Driver;
import com.formula1.service.DriverService;
import com.formula1.service.LapTimeCalculator;
import com.formula1.service.QualifyingService;
import com.formula1.service.VehicleService;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * La ficha rediseñada monta capas del hero, la galería y sus tarjetas de cifra
 * por código, así que un fallo ahí no lo detecta {@code ViewsLoadTest} —solo
 * carga el FXML vacío—. Esta prueba llama a {@code mostrar} de verdad, con un
 * piloto de cada extremo del catálogo: uno con historial de sesiones y otro
 * sin ninguna.
 */
class DriverDetailControllerTest {

    @BeforeAll
    static void iniciarJavaFx() throws InterruptedException {
        System.setProperty("prism.order", "sw");
        CountDownLatch listo = new CountDownLatch(1);
        try {
            Platform.startup(listo::countDown);
        } catch (IllegalStateException yaIniciado) {
            listo.countDown();
        }
        assertTrue(listo.await(10, TimeUnit.SECONDS));
    }

    @Test
    void muestraLaFichaDeUnPilotoConHistorial() throws InterruptedException {
        mostrarSinReventar(4); // George Russell
    }

    @Test
    void muestraLaFichaDeUnPilotoSinSesionesGuardadas() throws InterruptedException {
        mostrarSinReventar(20); // Logan Sargeant
    }

    private void mostrarSinReventar(int pilotoId) throws InterruptedException {
        AtomicReference<Throwable> fallo = new AtomicReference<>();
        CountDownLatch hecho = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                DataStore datos = DataStore.enMemoria();
                DriverDetailController controller = new DriverDetailController(
                        new DriverService(datos), new VehicleService(datos),
                        new QualifyingService(datos, new LapTimeCalculator()));
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/views/driver-detail.fxml"));
                loader.setControllerFactory(tipo -> controller);
                Parent vista = loader.load();
                new Scene(vista);
                vista.applyCss();

                controller.mostrar(pilotoId);

                Driver piloto = new DriverService(datos).porId(pilotoId).orElseThrow();
                assertTrue(textos(vista).contains(piloto.getNombre().toUpperCase()));
                assertFalse(vista.lookupAll(".team-stat").isEmpty());
            } catch (Throwable error) {
                fallo.set(error);
            } finally {
                hecho.countDown();
            }
        });
        if (!hecho.await(15, TimeUnit.SECONDS)) {
            fail("La ficha no terminó de mostrarse");
        }
        if (fallo.get() != null) {
            fail("La ficha de piloto no se construye", fallo.get());
        }
    }

    private java.util.List<String> textos(Parent vista) {
        return vista.lookupAll(".label").stream()
                .filter(n -> n instanceof javafx.scene.control.Label)
                .map(n -> ((javafx.scene.control.Label) n).getText())
                .filter(t -> t != null && !t.isBlank())
                .toList();
    }
}
