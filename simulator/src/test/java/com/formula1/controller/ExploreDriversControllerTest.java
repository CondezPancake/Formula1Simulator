package com.formula1.controller;

import com.formula1.data.DataStore;
import com.formula1.model.Driver;
import com.formula1.service.DriverService;
import com.formula1.service.TeamService;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class ExploreDriversControllerTest {

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
    void guardarReconstruyeLaCardConExperienciaYAtributosActualizados()
            throws InterruptedException {
        AtomicReference<Throwable> fallo = new AtomicReference<>();
        CountDownLatch hecho = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                DataStore datos = DataStore.enMemoria();
                DriverService pilotos = new DriverService(datos);
                ExploreDriversController controller = new ExploreDriversController(
                        pilotos, new TeamService(datos));
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/views/explore-drivers.fxml"));
                loader.setControllerFactory(tipo -> controller);
                Parent vista = loader.load();
                Driver piloto = pilotos.porId(1).orElseThrow();
                piloto.setExperiencia(11);

                controller.guardar(piloto);

                new Scene(vista);
                vista.applyCss();
                List<String> textos = textos(vista);
                assertTrue(textos.contains("11 años"));
                assertEquals(3L * pilotos.listar().size(),
                        textos.stream().filter(texto -> texto.endsWith("/100")).count());
            } catch (Throwable error) {
                fallo.set(error);
            } finally {
                hecho.countDown();
            }
        });
        if (!hecho.await(15, TimeUnit.SECONDS)) {
            fail("La card no terminó de actualizarse");
        }
        if (fallo.get() != null) {
            fail("La card de piloto no se actualiza", fallo.get());
        }
    }

    private List<String> textos(Parent raiz) {
        return raiz.lookupAll(".label").stream()
                .filter(Label.class::isInstance)
                .map(Label.class::cast)
                .map(Label::getText)
                .toList();
    }
}
