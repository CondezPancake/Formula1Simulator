package com.formula1.adapter.in.javafx;

import com.formula1.adapter.out.memory.DataStore;
import com.formula1.domain.model.Driver;
import com.formula1.application.usecase.DriverService;
import com.formula1.application.usecase.TeamService;
import com.formula1.util.F1Assets;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    void laRejillaMuestraUnaTarjetaPorPilotoConSuIdentidad()
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

                assertEquals(pilotos.listar().size(),
                        vista.lookupAll(".driver-card").size());

                // La tarjeta separa nombre de apellido, como la referencia.
                List<String> textos = textos(vista);
                assertTrue(textos.contains("Max"));
                assertTrue(textos.contains("Verstappen"));
                assertTrue(textos.contains("RED BULL RACING"));
                assertTrue(textos.contains(String.valueOf(piloto.getNumero())));
                assertTrue(textos.contains(piloto.getCodigo()));

                // Las cifras del piloto se mudaron a la ficha: aqui no salen.
                assertTrue(textos.stream().noneMatch(t -> t.endsWith("/100")));
                assertTrue(textos.stream().noneMatch(t -> t.endsWith("años")));
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
            fail("La card de piloto no se construye", fallo.get());
        }
    }

    /**
     * Los assets se resuelven por convencion desde el seed, asi que una errata
     * en un codigo o una nacionalidad deja la tarjeta sin foto sin avisar.
     */
    @Test
    void cadaPilotoDelSeedResuelveSuRenderYSuBandera() {
        DriverService pilotos = new DriverService(DataStore.enMemoria());

        for (Driver piloto : pilotos.listar()) {
            String render = F1Assets.render(piloto.getCodigo());
            assertNotNull(render, "sin render: " + piloto.getNombre());
            assertNotNull(getClass().getResource(render),
                    "falta el fichero " + render + " (ejecuta tools/descargar_assets_f1.py)");

            String bandera = F1Assets.bandera(piloto.getNacionalidad());
            assertNotNull(bandera, "nacionalidad sin mapear: " + piloto.getNacionalidad());
            assertNotNull(getClass().getResource(bandera), "falta el fichero " + bandera);

            assertNotNull(F1Assets.logo(piloto.getEquipo()),
                    "equipo sin mapear: " + piloto.getEquipo());
        }
        assertNotNull(getClass().getResource(F1Assets.texturaDrs()));
    }

    private List<String> textos(Parent raiz) {
        return raiz.lookupAll(".label").stream()
                .filter(Label.class::isInstance)
                .map(Label.class::cast)
                .map(Label::getText)
                .toList();
    }
}
