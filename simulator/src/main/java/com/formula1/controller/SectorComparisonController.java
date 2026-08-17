package com.formula1.controller;

import com.formula1.model.LapResult;
import com.formula1.model.TrackSector;
import com.formula1.service.SectorComparisonService;
import com.formula1.util.FormatUtils;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Presenta HU-33 sin duplicar reglas de negocio en JavaFX.
 * El controlador se limita a transformar la comparación en celdas y etiquetas.
 */
public final class SectorComparisonController {

    @FXML private Label lblBestSector1;
    @FXML private Label lblBestSector2;
    @FXML private Label lblBestSector3;
    @FXML private TableView<LapResult> tabla;
    @FXML private TableColumn<LapResult, Number> colPosicion;
    @FXML private TableColumn<LapResult, String> colPiloto;
    @FXML private TableColumn<LapResult, String> colEquipo;
    @FXML private TableColumn<LapResult, String> colSector1;
    @FXML private TableColumn<LapResult, String> colSector2;
    @FXML private TableColumn<LapResult, String> colSector3;
    @FXML private TableColumn<LapResult, String> colTotal;

    private final SectorComparisonService comparaciones = new SectorComparisonService();
    private final Map<TrackSector, LapResult> ganadores = new EnumMap<>(TrackSector.class);

    @FXML
    private void initialize() {
        colPosicion.setCellValueFactory(
                cell -> new SimpleIntegerProperty(cell.getValue().getPosicion()));
        colPiloto.setCellValueFactory(
                cell -> new SimpleStringProperty(cell.getValue().getPiloto()));
        colEquipo.setCellValueFactory(
                cell -> new SimpleStringProperty(cell.getValue().getEquipo()));
        configurarColumnaSector(colSector1, TrackSector.SECTOR_1);
        configurarColumnaSector(colSector2, TrackSector.SECTOR_2);
        configurarColumnaSector(colSector3, TrackSector.SECTOR_3);
        colTotal.setCellValueFactory(cell -> new SimpleStringProperty(
                FormatUtils.formatLapResult(cell.getValue())));
        reiniciar();
    }

    void reiniciar() {
        ganadores.clear();
        tabla.getItems().clear();
        lblBestSector1.setText("Mejor Sector 1 —");
        lblBestSector2.setText("Mejor Sector 2 —");
        lblBestSector3.setText("Mejor Sector 3 —");
    }

    /** Carga la parrilla completa y vuelve a calcular los tres ganadores. */
    void cargar(List<LapResult> resultados) {
        List<LapResult> resultadosSeguros = resultados == null ? List.of() : List.copyOf(resultados);
        ganadores.clear();
        for (TrackSector sector : sectoresComparables()) {
            comparaciones.mejorEn(resultadosSeguros, sector)
                    .ifPresent(ganador -> ganadores.put(sector, ganador));
        }
        tabla.setItems(FXCollections.observableArrayList(resultadosSeguros));
        actualizarEtiquetaGanador(lblBestSector1, TrackSector.SECTOR_1);
        actualizarEtiquetaGanador(lblBestSector2, TrackSector.SECTOR_2);
        actualizarEtiquetaGanador(lblBestSector3, TrackSector.SECTOR_3);
        tabla.refresh();
    }

    private void configurarColumnaSector(TableColumn<LapResult, String> column,
                                         TrackSector sector) {
        column.setCellValueFactory(cell -> new SimpleStringProperty(
                formatearSector(cell.getValue(), sector)));
        column.setCellFactory(ignored -> new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                getStyleClass().remove("best-sector");
                setText(empty ? null : value);
                LapResult result = empty ? null : getTableRow().getItem();
                if (result != null && result == ganadores.get(sector)) {
                    getStyleClass().add("best-sector");
                }
            }
        });
    }

    private void actualizarEtiquetaGanador(Label label, TrackSector sector) {
        LapResult ganador = ganadores.get(sector);
        if (ganador == null) {
            label.setText("Mejor " + sector.getEtiqueta() + " —");
            return;
        }
        label.setText(String.format(Locale.ROOT, "Mejor %s → %s · %.3f s",
                sector.getEtiqueta(), ganador.getPiloto(),
                ganador.getSectorTimes().tiempoDe(sector)));
    }

    private String formatearSector(LapResult result, TrackSector sector) {
        if (!result.isVueltaValida() || !result.hasSectorTimes()) {
            return "—";
        }
        return String.format(Locale.ROOT, "%.3f s", result.getSectorTimes().tiempoDe(sector));
    }

    private List<TrackSector> sectoresComparables() {
        return List.of(TrackSector.SECTOR_1, TrackSector.SECTOR_2, TrackSector.SECTOR_3);
    }
}
