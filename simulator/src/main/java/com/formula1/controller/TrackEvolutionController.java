package com.formula1.controller;

import com.formula1.model.TrackEvolutionSnapshot;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.List;
import java.util.Locale;

/** Presenta la evolución acumulativa de grip de HU-34 sin recalcular el motor. */
public final class TrackEvolutionController {

    @FXML private Label lblGripInicial;
    @FXML private Label lblGripFinal;
    @FXML private Label lblCambioGrip;
    @FXML private TableView<TrackEvolutionSnapshot> tabla;
    @FXML private TableColumn<TrackEvolutionSnapshot, Number> colVuelta;
    @FXML private TableColumn<TrackEvolutionSnapshot, String> colPiloto;
    @FXML private TableColumn<TrackEvolutionSnapshot, String> colGripInicial;
    @FXML private TableColumn<TrackEvolutionSnapshot, String> colGripFinal;
    @FXML private TableColumn<TrackEvolutionSnapshot, String> colGoma;
    @FXML private TableColumn<TrackEvolutionSnapshot, String> colLluvia;
    @FXML private TableColumn<TrackEvolutionSnapshot, String> colTendencia;
    @FXML private LineChart<Number, Number> grafico;

    @FXML
    private void initialize() {
        colVuelta.setCellValueFactory(
                cell -> new SimpleIntegerProperty(cell.getValue().vuelta()));
        colPiloto.setCellValueFactory(
                cell -> new SimpleStringProperty(cell.getValue().piloto()));
        colGripInicial.setCellValueFactory(cell -> new SimpleStringProperty(
                porcentaje(cell.getValue().gripInicialPorcentaje())));
        colGripFinal.setCellValueFactory(cell -> new SimpleStringProperty(
                porcentaje(cell.getValue().gripFinalPorcentaje())));
        colGoma.setCellValueFactory(cell -> new SimpleStringProperty(String.format(
                Locale.ROOT, "%.2f → %.2f %%",
                cell.getValue().gomaInicialPorcentaje(),
                cell.getValue().gomaFinalPorcentaje())));
        colLluvia.setCellValueFactory(cell -> new SimpleStringProperty(
                porcentaje(cell.getValue().lluviaPromedioPorcentaje())));
        colTendencia.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().tendencia()));
        grafico.setAnimated(false);
        grafico.setCreateSymbols(true);
        reiniciar();
    }

    void reiniciar() {
        tabla.getItems().clear();
        grafico.getData().clear();
        lblGripInicial.setText("— %");
        lblGripFinal.setText("— %");
        lblCambioGrip.setText("—");
    }

    /** Carga las lecturas consolidadas que ya fueron utilizadas por el motor. */
    void cargar(List<TrackEvolutionSnapshot> muestras) {
        List<TrackEvolutionSnapshot> datos = muestras == null ? List.of() : List.copyOf(muestras);
        tabla.setItems(FXCollections.observableArrayList(datos));
        grafico.getData().clear();
        if (datos.isEmpty()) {
            reiniciar();
            return;
        }

        XYChart.Series<Number, Number> serieGrip = new XYChart.Series<>();
        serieGrip.setName("Grip final");
        XYChart.Series<Number, Number> serieGoma = new XYChart.Series<>();
        serieGoma.setName("Goma acumulada");
        for (TrackEvolutionSnapshot muestra : datos) {
            serieGrip.getData().add(new XYChart.Data<>(
                    muestra.vuelta(), muestra.gripFinalPorcentaje()));
            serieGoma.getData().add(new XYChart.Data<>(
                    muestra.vuelta(), muestra.gomaFinalPorcentaje()));
        }
        grafico.getData().add(serieGrip);
        grafico.getData().add(serieGoma);

        TrackEvolutionSnapshot primera = datos.get(0);
        TrackEvolutionSnapshot ultima = datos.get(datos.size() - 1);
        lblGripInicial.setText(porcentaje(primera.gripInicialPorcentaje()));
        lblGripFinal.setText(porcentaje(ultima.gripFinalPorcentaje()));
        lblCambioGrip.setText(String.format(Locale.ROOT, "%+.2f puntos",
                ultima.gripFinalPorcentaje() - primera.gripInicialPorcentaje()));
    }

    private String porcentaje(double value) {
        return String.format(Locale.ROOT, "%.2f %%", value);
    }
}
