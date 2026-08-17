package com.formula1.controller;

import com.formula1.model.LapStatus;
import com.formula1.model.TelemetrySnapshot;
import com.formula1.util.FormatUtils;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.ToDoubleFunction;

/**
 * Presenta HU-32 como un componente JavaFX independiente.
 * Recibe telemetría calculada; no contiene reglas del motor de simulación.
 */
public final class LapEvolutionController {

    @FXML private ComboBox<LapEvolutionMetric> selectorMetrica;
    @FXML private LineChart<Number, Number> grafico;
    @FXML private NumberAxis ejeValor;
    @FXML private Label lblProgreso;
    @FXML private Label lblTendencia;
    @FXML private Label lblRango;
    @FXML private TableView<TelemetrySnapshot> tabla;
    @FXML private TableColumn<TelemetrySnapshot, Number> colSegmento;
    @FXML private TableColumn<TelemetrySnapshot, String> colSector;
    @FXML private TableColumn<TelemetrySnapshot, String> colVelocidad;
    @FXML private TableColumn<TelemetrySnapshot, String> colTiempo;
    @FXML private TableColumn<TelemetrySnapshot, String> colDelta;
    @FXML private TableColumn<TelemetrySnapshot, String> colCombustible;
    @FXML private TableColumn<TelemetrySnapshot, String> colDesgaste;
    @FXML private TableColumn<TelemetrySnapshot, String> colTemperaturas;
    @FXML private TableColumn<TelemetrySnapshot, String> colEstado;

    private final List<TelemetrySnapshot> muestras = new ArrayList<>();
    private XYChart.Series<Number, Number> serie;

    @FXML
    private void initialize() {
        selectorMetrica.getItems().addAll(LapEvolutionMetric.values());
        selectorMetrica.setValue(LapEvolutionMetric.DELTA);
        selectorMetrica.valueProperty().addListener(
                (obs, anterior, actual) -> pintarGrafico());
        grafico.setAnimated(false);
        grafico.setCreateSymbols(false);
        configurarTabla();
        reiniciar();
    }

    void reiniciar() {
        muestras.clear();
        tabla.getItems().clear();
        lblProgreso.setText("Esperando vuelta");
        lblTendencia.setText("—");
        lblRango.setText("—");
        pintarGrafico();
    }

    /** Incorpora o reemplaza la lectura de un segmento sin producir duplicados. */
    void actualizar(TelemetrySnapshot muestra) {
        int indice = indiceDelSegmento(muestra.segmento());
        if (indice >= 0) {
            muestras.set(indice, muestra);
            tabla.getItems().set(indice, muestra);
            pintarGrafico();
        } else {
            muestras.add(muestra);
            tabla.getItems().add(muestra);
            LapEvolutionMetric metrica = selectorMetrica.getValue();
            if (metrica != null && serie != null) {
                serie.getData().add(new XYChart.Data<>(
                        muestra.segmento(), metrica.valorDe(muestra)));
            }
        }
        actualizarResumen(muestra);
    }

    /** Sustituye el flujo en vivo por la versión consolidada de la sesión. */
    void cargar(List<TelemetrySnapshot> nuevasMuestras) {
        muestras.clear();
        muestras.addAll(nuevasMuestras == null ? List.of() : nuevasMuestras);
        tabla.setItems(FXCollections.observableArrayList(muestras));
        pintarGrafico();
        if (!muestras.isEmpty()) {
            actualizarResumen(muestras.get(muestras.size() - 1));
        }
    }

    private void configurarTabla() {
        colSegmento.setCellValueFactory(
                f -> new SimpleIntegerProperty(f.getValue().segmento()));
        colSector.setCellValueFactory(f -> new SimpleStringProperty(
                "S" + f.getValue().sectorActual()));
        colVelocidad.setCellValueFactory(f -> new SimpleStringProperty(
                String.format(Locale.ROOT, "%.0f km/h", f.getValue().velocidadKmh())));
        colTiempo.setCellValueFactory(f -> new SimpleStringProperty(
                FormatUtils.formatLapTime(f.getValue().tiempoVueltaSegundos())));
        colDelta.setCellValueFactory(f -> new SimpleStringProperty(
                FormatUtils.formatDelta(f.getValue().deltaSegundos())));
        colCombustible.setCellValueFactory(f -> new SimpleStringProperty(
                String.format(Locale.ROOT, "%.1f %%", f.getValue().combustibleRestantePorcentaje())));
        colDesgaste.setCellValueFactory(f -> new SimpleStringProperty(
                String.format(Locale.ROOT, "%.1f %%", f.getValue().desgasteNeumaticosPorcentaje())));
        colTemperaturas.setCellValueFactory(f -> new SimpleStringProperty(
                String.format(Locale.ROOT, "N %.1f °C · M %.1f °C",
                        f.getValue().temperaturaNeumaticosC(), f.getValue().temperaturaMotorC())));
        colEstado.setCellValueFactory(f -> new SimpleStringProperty(
                f.getValue().estadoVuelta().getEtiqueta()));
    }

    private void pintarGrafico() {
        LapEvolutionMetric metrica = selectorMetrica.getValue();
        grafico.getData().clear();
        serie = new XYChart.Series<>();
        if (metrica == null) {
            return;
        }
        serie.setName(metrica.etiqueta);
        ejeValor.setLabel(metrica.unidad);
        grafico.setTitle(metrica.etiqueta + " por segmento");
        muestras.forEach(muestra -> serie.getData().add(
                new XYChart.Data<>(muestra.segmento(), metrica.valorDe(muestra))));
        grafico.getData().add(serie);
        actualizarRango(metrica);
    }

    private void actualizarResumen(TelemetrySnapshot muestra) {
        lblProgreso.setText(String.format("Segmento %d/%d · S%d",
                muestra.segmento(), muestra.totalSegmentos(), muestra.sectorActual()));
        if (muestra.estadoVuelta() != LapStatus.VALID) {
            lblTendencia.setText(muestra.estadoVuelta().getEtiqueta());
        } else {
            lblTendencia.setText(muestra.deltaSegundos() <= 0
                    ? "Ganando tiempo frente al récord"
                    : "Perdiendo tiempo frente al récord");
        }
        LapEvolutionMetric metrica = selectorMetrica.getValue();
        if (metrica != null) {
            actualizarRango(metrica);
        }
    }

    private void actualizarRango(LapEvolutionMetric metrica) {
        if (muestras.isEmpty()) {
            lblRango.setText("—");
            return;
        }
        double minimo = muestras.stream().mapToDouble(metrica::valorDe).min().orElse(0);
        double maximo = muestras.stream().mapToDouble(metrica::valorDe).max().orElse(0);
        lblRango.setText(String.format(Locale.ROOT,
                "Rango: %.2f a %.2f %s", minimo, maximo, metrica.unidad));
    }

    private int indiceDelSegmento(int segmento) {
        for (int indice = 0; indice < muestras.size(); indice++) {
            if (muestras.get(indice).segmento() == segmento) {
                return indice;
            }
        }
        return -1;
    }

    private enum LapEvolutionMetric {
        VELOCIDAD("Velocidad", "km/h", TelemetrySnapshot::velocidadKmh),
        TIEMPO("Tiempo acumulado", "s", TelemetrySnapshot::tiempoVueltaSegundos),
        DESGASTE("Desgaste de neumáticos", "%", TelemetrySnapshot::desgasteNeumaticosPorcentaje),
        COMBUSTIBLE("Combustible restante", "%", TelemetrySnapshot::combustibleRestantePorcentaje),
        TEMPERATURA_NEUMATICOS("Temperatura de neumáticos", "°C",
                TelemetrySnapshot::temperaturaNeumaticosC),
        TEMPERATURA_MOTOR("Temperatura del motor", "°C",
                TelemetrySnapshot::temperaturaMotorC),
        DELTA("Delta frente al récord", "s", TelemetrySnapshot::deltaSegundos);

        private final String etiqueta;
        private final String unidad;
        private final ToDoubleFunction<TelemetrySnapshot> extractor;

        LapEvolutionMetric(String etiqueta, String unidad,
                           ToDoubleFunction<TelemetrySnapshot> extractor) {
            this.etiqueta = etiqueta;
            this.unidad = unidad;
            this.extractor = extractor;
        }

        double valorDe(TelemetrySnapshot muestra) {
            return extractor.applyAsDouble(muestra);
        }

        @Override
        public String toString() {
            return etiqueta;
        }
    }
}
