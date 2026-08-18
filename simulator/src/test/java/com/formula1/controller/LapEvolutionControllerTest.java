package com.formula1.controller;

import com.formula1.model.DynamicWeatherState;
import com.formula1.model.EventOccurrence;
import com.formula1.model.LapStatus;
import com.formula1.model.TelemetrySnapshot;
import com.formula1.model.TrackSector;
import com.formula1.model.WeatherSnapshot;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

class LapEvolutionControllerTest {

    @BeforeAll
    static void iniciarJavaFx() throws InterruptedException {
        System.setProperty("prism.order", "sw");
        CountDownLatch listo = new CountDownLatch(1);
        try {
            Platform.startup(listo::countDown);
        } catch (IllegalStateException yaIniciado) {
            listo.countDown();
        }
        if (!listo.await(10, TimeUnit.SECONDS)) {
            fail("JavaFX no inicio");
        }
    }

    @Test
    void resumeLasMuestrasInternasEnLosTresSectoresReales() {
        List<TelemetrySnapshot> lecturas = new ArrayList<>();
        for (int segmento = 1; segmento <= 20; segmento++) {
            int sector = TrackSector.desdeSegmento(segmento, 20).ordinal();
            lecturas.add(muestra(segmento, sector));
        }

        List<TelemetrySnapshot> sectores = LapEvolutionController.resumirPorSector(lecturas);

        assertEquals(List.of(1, 2, 3), sectores.stream()
                .map(TelemetrySnapshot::sectorActual).toList());
        assertEquals(List.of(7, 14, 20), sectores.stream()
                .map(TelemetrySnapshot::segmento).toList());
    }

    @Test
    void admiteUnaEvolucionVacia() {
        assertEquals(List.of(), LapEvolutionController.resumirPorSector(null));
        assertEquals(List.of(), LapEvolutionController.resumirPorSector(List.of()));
    }

    @Test
    void cargaLaVistaConLasColumnasDeSector() throws InterruptedException {
        AtomicReference<Object> vista = new AtomicReference<>();
        AtomicReference<Throwable> fallo = new AtomicReference<>();
        CountDownLatch hecho = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                vista.set(FXMLLoader.load(getClass().getResource("/views/lap-evolution.fxml")));
            } catch (Throwable error) {
                fallo.set(error);
            } finally {
                hecho.countDown();
            }
        });

        if (!hecho.await(10, TimeUnit.SECONDS)) {
            fail("La vista no termino de cargar");
        }
        if (fallo.get() != null) {
            fail("La vista de evolucion de vuelta no carga", fallo.get());
        }
        assertNotNull(vista.get());
    }

    private TelemetrySnapshot muestra(int segmento, int sector) {
        WeatherSnapshot clima = new WeatherSnapshot(
                segmento, 20, DynamicWeatherState.SECO, 24, 50,
                0, 0, 32, 95, 95, 95);
        return new TelemetrySnapshot(
                "Piloto", "Auto", segmento, 20, 250, 350, 10_000,
                80, 10, 90, 95, sector, segmento * 4,
                0.1, clima, LapStatus.VALID,
                EventOccurrence.noEvent(1, "Piloto", 1));
    }
}
