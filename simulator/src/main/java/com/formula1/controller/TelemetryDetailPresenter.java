package com.formula1.controller;

import com.formula1.model.TelemetrySnapshot;

import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleFunction;

/** Mantiene el detalle gráfico de telemetría y actualiza sus series incrementalmente. */
final class TelemetryDetailPresenter {

    private final ComboBox<Integer> lapSelector;
    private final Button detailsButton;
    private final Label driverLabel;
    private final LineChart<Number, Number> speedChart;
    private final LineChart<Number, Number> rpmChart;
    private final LineChart<Number, Number> fuelChart;
    private final LineChart<Number, Number> wearChart;
    private final LineChart<Number, Number> temperatureChart;
    private final LineChart<Number, Number> deltaChart;
    private final List<List<TelemetrySnapshot>> laps = new ArrayList<>();

    TelemetryDetailPresenter(
            ComboBox<Integer> lapSelector, Button detailsButton, Label driverLabel,
            LineChart<Number, Number> speedChart,
            LineChart<Number, Number> rpmChart,
            LineChart<Number, Number> fuelChart,
            LineChart<Number, Number> wearChart,
            LineChart<Number, Number> temperatureChart,
            LineChart<Number, Number> deltaChart) {
        this.lapSelector = lapSelector;
        this.detailsButton = detailsButton;
        this.driverLabel = driverLabel;
        this.speedChart = speedChart;
        this.rpmChart = rpmChart;
        this.fuelChart = fuelChart;
        this.wearChart = wearChart;
        this.temperatureChart = temperatureChart;
        this.deltaChart = deltaChart;
        lapSelector.valueProperty().addListener((observable, previous, lap) -> {
            if (lap != null && lap >= 1 && lap <= laps.size()) {
                render(laps.get(lap - 1));
            }
        });
    }

    boolean hasData() {
        return !laps.isEmpty();
    }

    /** Reemplaza la fuente completa al recuperar una sesión o reiniciar. */
    void load(List<TelemetrySnapshot> samples) {
        Integer previousSelection = lapSelector.getValue();
        laps.clear();
        group(samples == null ? List.of() : samples);
        lapSelector.getItems().setAll(
                java.util.stream.IntStream.rangeClosed(1, laps.size()).boxed().toList());
        boolean empty = laps.isEmpty();
        lapSelector.setDisable(empty);
        detailsButton.setDisable(empty);
        if (empty) {
            lapSelector.setValue(null);
            driverLabel.setText("Esperando datos");
            clearCharts();
            return;
        }
        int selection = previousSelection == null
                ? laps.size() : Math.min(previousSelection, laps.size());
        if (lapSelector.getValue() != null && lapSelector.getValue() == selection) {
            render(laps.get(selection - 1));
        } else {
            lapSelector.setValue(selection);
        }
    }

    /** Añade una muestra sin reconstruir todos los puntos ya representados. */
    void append(TelemetrySnapshot sample) {
        if (sample == null) {
            return;
        }
        boolean newLap = startsNewLap(sample);
        if (newLap) {
            List<TelemetrySnapshot> lap = new ArrayList<>();
            lap.add(sample);
            laps.add(lap);
            lapSelector.getItems().add(laps.size());
            lapSelector.setDisable(false);
            detailsButton.setDisable(false);
            lapSelector.setValue(laps.size());
            return;
        }

        List<TelemetrySnapshot> currentLap = laps.get(laps.size() - 1);
        currentLap.add(sample);
        if (lapSelector.getValue() != null && lapSelector.getValue() == laps.size()) {
            appendToCharts(sample);
            showDriver(sample);
        }
    }

    private boolean startsNewLap(TelemetrySnapshot sample) {
        if (laps.isEmpty()) {
            return true;
        }
        List<TelemetrySnapshot> lastLap = laps.get(laps.size() - 1);
        TelemetrySnapshot lastSample = lastLap.get(lastLap.size() - 1);
        return sample.segmento() <= lastSample.segmento();
    }

    private void group(List<TelemetrySnapshot> samples) {
        List<TelemetrySnapshot> currentLap = null;
        int previousSegment = Integer.MAX_VALUE;
        for (TelemetrySnapshot sample : samples) {
            if (currentLap == null || sample.segmento() <= previousSegment) {
                currentLap = new ArrayList<>();
                laps.add(currentLap);
            }
            currentLap.add(sample);
            previousSegment = sample.segmento();
        }
    }

    private void render(List<TelemetrySnapshot> samples) {
        clearCharts();
        speedChart.getData().add(series("Velocidad", samples, TelemetrySnapshot::velocidadKmh));
        rpmChart.getData().add(series("RPM", samples, sample -> sample.rpm()));
        fuelChart.getData().add(series(
                "Combustible", samples, TelemetrySnapshot::combustibleRestantePorcentaje));
        wearChart.getData().add(series(
                "Desgaste", samples, TelemetrySnapshot::desgasteNeumaticosPorcentaje));
        temperatureChart.getData().add(series(
                "Neumáticos", samples, TelemetrySnapshot::temperaturaNeumaticosC));
        temperatureChart.getData().add(series(
                "Motor", samples, TelemetrySnapshot::temperaturaMotorC));
        deltaChart.getData().add(series("Delta", samples, TelemetrySnapshot::deltaSegundos));
        if (!samples.isEmpty()) {
            showDriver(samples.get(samples.size() - 1));
        }
    }

    private void appendToCharts(TelemetrySnapshot sample) {
        if (speedChart.getData().isEmpty()) {
            render(laps.get(laps.size() - 1));
            return;
        }
        add(speedChart, 0, sample.segmento(), sample.velocidadKmh());
        add(rpmChart, 0, sample.segmento(), sample.rpm());
        add(fuelChart, 0, sample.segmento(), sample.combustibleRestantePorcentaje());
        add(wearChart, 0, sample.segmento(), sample.desgasteNeumaticosPorcentaje());
        add(temperatureChart, 0, sample.segmento(), sample.temperaturaNeumaticosC());
        add(temperatureChart, 1, sample.segmento(), sample.temperaturaMotorC());
        add(deltaChart, 0, sample.segmento(), sample.deltaSegundos());
    }

    private void add(LineChart<Number, Number> chart, int seriesIndex,
                     int segment, double value) {
        chart.getData().get(seriesIndex).getData().add(new XYChart.Data<>(segment, value));
    }

    private XYChart.Series<Number, Number> series(
            String name, List<TelemetrySnapshot> samples,
            ToDoubleFunction<TelemetrySnapshot> value) {
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName(name);
        for (TelemetrySnapshot sample : samples) {
            series.getData().add(new XYChart.Data<>(
                    sample.segmento(), value.applyAsDouble(sample)));
        }
        return series;
    }

    private void showDriver(TelemetrySnapshot sample) {
        driverLabel.setText(sample.piloto() + " · " + sample.vehiculo());
    }

    private void clearCharts() {
        speedChart.getData().clear();
        rpmChart.getData().clear();
        fuelChart.getData().clear();
        wearChart.getData().clear();
        temperatureChart.getData().clear();
        deltaChart.getData().clear();
    }
}
