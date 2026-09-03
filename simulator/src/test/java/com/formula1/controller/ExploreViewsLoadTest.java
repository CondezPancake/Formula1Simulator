package com.formula1.controller;

import com.formula1.data.DataStore;
import com.formula1.service.CircuitService;
import com.formula1.service.TeamService;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class ExploreViewsLoadTest {

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
    void cargaTeamsYLosDetallesConSusDatos() throws InterruptedException {
        CountDownLatch hecho = new CountDownLatch(1);
        AtomicReference<Throwable> fallo = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                DataStore datos = DataStore.enMemoria();
                Parent teams = cargar("explore-teams", new ExploreTeamsController(
                        new TeamService(datos), new VehicleService(datos)));
                // Una tarjeta por escudería del seed.
                assertEquals(new TeamService(datos).listar().size(),
                        teams.lookupAll(".team-card").size(),
                        "la rejilla debe mostrar una tarjeta por equipo");

                Parent fichaEquipo = cargar("team-detail", new TeamDetailController(
                        new TeamService(datos), new VehicleService(datos)));
                assertNotNull(fichaEquipo.lookup("#marcoCoche"));
                assertNotNull(fichaEquipo.lookup("#tarjetasPilotos"));

                Parent detalle = cargar("circuit-detail", new CircuitDetailController(
                        new CircuitService(datos), new VehicleService(datos)));
                assertNotNull(detalle.lookup("#tablaImpacto"));
                assertNotNull(detalle.lookup("#listaGanadores"));
            } catch (Throwable error) {
                fallo.set(error);
            } finally {
                hecho.countDown();
            }
        });
        if (!hecho.await(20, TimeUnit.SECONDS)) fail("Las vistas no terminaron de cargar");
        if (fallo.get() != null) fail("Falló la carga enfocada de Explorar", fallo.get());
    }

    private Parent cargar(String vista, Object controlador) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/" + vista + ".fxml"));
        loader.setControllerFactory(tipo -> controlador);
        Parent raiz = loader.load();
        new Scene(raiz);
        raiz.applyCss();
        return raiz;
    }
}
