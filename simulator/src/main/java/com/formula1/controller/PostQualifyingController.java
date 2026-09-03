package com.formula1.controller;

import com.formula1.domain.model.EventOccurrence;
import com.formula1.domain.model.LapResult;
import com.formula1.domain.model.PitStopRecord;
import com.formula1.domain.model.QualifyingSession;
import com.formula1.domain.model.SimulationConfig;
import com.formula1.domain.model.TireChangeRecord;
import com.formula1.domain.model.TireCompound;
import com.formula1.domain.model.TrackSector;
import com.formula1.util.FormatUtils;
import com.formula1.util.TeamColors;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Presenta una sesión terminada sin modificar ni recalcular sus resultados. */
public final class PostQualifyingController {

    @FXML private Label lblResultadoCircuito;
    @FXML private Label lblResultadoSesion;
    @FXML private Label lblResultadoPole;
    @FXML private Label lblResultadoEstadisticas;
    @FXML private Label lblResultadoConfiguracion;

    @FXML private TableView<LapResult> tablaResultadoFinal;
    @FXML private TableColumn<LapResult, Number> colResultadoPosicion;
    @FXML private TableColumn<LapResult, String> colResultadoPiloto;
    @FXML private TableColumn<LapResult, String> colResultadoTiempo;
    @FXML private TableColumn<LapResult, String> colResultadoGap;
    @FXML private TableColumn<LapResult, String> colResultadoS1;
    @FXML private TableColumn<LapResult, String> colResultadoS2;
    @FXML private TableColumn<LapResult, String> colResultadoS3;
    @FXML private TableColumn<LapResult, String> colResultadoCompuesto;
    @FXML private TableColumn<LapResult, Number> colResultadoParadas;
    @FXML private TableColumn<LapResult, Number> colResultadoEventos;

    @FXML private Label lblDetallePiloto;
    @FXML private Label lblDetalleEquipo;
    @FXML private Label lblDetalleResultado;
    @FXML private Label lblDetalleSectores;
    @FXML private Label lblDetalleNeumaticos;
    @FXML private Label lblDetalleBoxes;
    @FXML private Label lblDetalleEventos;
    @FXML private Label lblDetalleVehiculo;
    @FXML private Label lblDetalleConfiguracion;
    @FXML private VBox listaRadioResultado;

    private QualifyingSession session;

    @FXML
    public void initialize() {
        colResultadoPosicion.setCellValueFactory(row ->
                new SimpleIntegerProperty(row.getValue().getPosicion()));
        colResultadoPiloto.setCellValueFactory(row ->
                new SimpleStringProperty(row.getValue().getPiloto()));
        colResultadoTiempo.setCellValueFactory(row ->
                new SimpleStringProperty(FormatUtils.formatLapResult(row.getValue())));
        colResultadoGap.setCellValueFactory(row ->
                new SimpleStringProperty(row.getValue().isVueltaValida()
                        ? FormatUtils.formatGap(row.getValue().getGap()) : "—"));
        colResultadoS1.setCellValueFactory(row -> sector(row.getValue(), TrackSector.SECTOR_1));
        colResultadoS2.setCellValueFactory(row -> sector(row.getValue(), TrackSector.SECTOR_2));
        colResultadoS3.setCellValueFactory(row -> sector(row.getValue(), TrackSector.SECTOR_3));
        colResultadoCompuesto.setCellValueFactory(row ->
                new SimpleStringProperty(finalCompound(row.getValue().getPilotoId()).getCodigo()));
        colResultadoParadas.setCellValueFactory(row ->
                new SimpleIntegerProperty(pitStopsFor(row.getValue().getPilotoId()).size()));
        colResultadoEventos.setCellValueFactory(row ->
                new SimpleIntegerProperty(eventsFor(row.getValue().getPilotoId()).size()));
        configureClassificationStyle();
        tablaResultadoFinal.getSelectionModel().selectedItemProperty()
                .addListener((observable, previous, selected) -> showDriver(selected));
    }

    /** Reutiliza el lenguaje visual de clasificación sin alterar sus datos. */
    private void configureClassificationStyle() {
        tablaResultadoFinal.setRowFactory(table -> {
            TableRow<LapResult> row = new TableRow<>() {
                @Override
                protected void updateItem(LapResult result, boolean empty) {
                    super.updateItem(result, empty);
                    getStyleClass().removeAll(
                            "pole-row", "invalid-row", "configured-driver-row");
                    if (empty || result == null) {
                        setStyle("");
                        return;
                    }
                    if (!result.isVueltaValida()) {
                        getStyleClass().add("invalid-row");
                    } else if (result.getPosicion() == 1) {
                        getStyleClass().add("pole-row");
                    }
                    SimulationConfig config = session == null ? null : session.getConfig();
                    if (config != null && config.getPilotoId() != null
                            && config.getPilotoId() == result.getPilotoId()) {
                        getStyleClass().add("configured-driver-row");
                    }
                    setStyle("-fx-border-color: transparent transparent #17171B "
                            + TeamColors.hex(result.getEquipo())
                            + "; -fx-border-width: 0 0 1 4;");
                }
            };
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    ExploreDriversController.abrirFicha(row.getItem().getPilotoId());
                }
            });
            return row;
        });

        colResultadoPosicion.setCellFactory(column -> positionCell());
        colResultadoCompuesto.setCellFactory(column -> compoundCell());
        colResultadoParadas.setCellFactory(column -> countCell("post-pit-count"));
        colResultadoEventos.setCellFactory(column -> countCell("post-event-count"));
        colResultadoTiempo.getStyleClass().add("mono-col");
        colResultadoGap.getStyleClass().add("mono-col");
        colResultadoS1.getStyleClass().add("mono-col");
        colResultadoS2.getStyleClass().add("mono-col");
        colResultadoS3.getStyleClass().add("mono-col");
    }

    private TableCell<LapResult, Number> positionCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Number value, boolean empty) {
                super.updateItem(value, empty);
                getStyleClass().removeAll("pos-badge", "p1", "p2", "p3");
                if (empty || value == null) {
                    setText(null);
                    return;
                }
                int position = value.intValue();
                setText(String.valueOf(position));
                getStyleClass().add("pos-badge");
                if (position >= 1 && position <= 3) {
                    getStyleClass().add("p" + position);
                }
            }
        };
    }

    private TableCell<LapResult, String> compoundCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                getStyleClass().removeAll("result-compound", "compound-soft",
                        "compound-medium", "compound-hard");
                if (empty || value == null) {
                    setText(null);
                    return;
                }
                setText(value);
                getStyleClass().addAll("result-compound", switch (value) {
                    case "S" -> "compound-soft";
                    case "H" -> "compound-hard";
                    default -> "compound-medium";
                });
            }
        };
    }

    private TableCell<LapResult, Number> countCell(String populatedStyle) {
        return new TableCell<>() {
            @Override
            protected void updateItem(Number value, boolean empty) {
                super.updateItem(value, empty);
                getStyleClass().remove(populatedStyle);
                if (empty || value == null) {
                    setText(null);
                    return;
                }
                setText(String.valueOf(value.intValue()));
                if (value.intValue() > 0) {
                    getStyleClass().add(populatedStyle);
                }
            }
        };
    }

    public void load(QualifyingSession session, Integer preferredDriverId) {
        this.session = session;
        if (session == null) {
            clear();
            return;
        }
        List<LapResult> results = session.getResultados() == null
                ? List.of() : session.getResultados();
        tablaResultadoFinal.setItems(FXCollections.observableArrayList(results));
        showSessionSummary(results);

        Integer selectedId = preferredDriverId != null
                ? preferredDriverId
                : session.getConfig() == null ? null : session.getConfig().getPilotoId();
        LapResult selected = results.stream()
                .filter(result -> selectedId != null && result.getPilotoId() == selectedId)
                .findFirst()
                .orElse(results.isEmpty() ? null : results.get(0));
        if (selected == null) {
            showDriver(null);
        } else {
            tablaResultadoFinal.getSelectionModel().select(selected);
            tablaResultadoFinal.scrollTo(results.indexOf(selected));
        }
        tablaResultadoFinal.refresh();
    }

    private void showSessionSummary(List<LapResult> results) {
        LapResult pole = results.stream()
                .filter(LapResult::isVueltaValida)
                .findFirst()
                .orElse(null);
        long validLaps = results.stream().filter(LapResult::isVueltaValida).count();
        double average = results.stream().filter(LapResult::isVueltaValida)
                .mapToDouble(LapResult::getTiempoSegundos).average().orElse(0);
        lblResultadoCircuito.setText(session.getCircuito() == null
                ? "SESIÓN DE CLASIFICACIÓN" : session.getCircuito().toUpperCase(Locale.ROOT));
        lblResultadoSesion.setText((session.getClima() == null ? "Clima —"
                : session.getClima().getEtiqueta()) + " · "
                + valueOrDash(session.getFecha()) + " · " + results.size() + " participantes");
        lblResultadoPole.setText(pole == null ? "Sin vueltas válidas"
                : "POLE · " + pole.getPiloto() + " · "
                        + FormatUtils.formatLapTime(pole.getTiempoSegundos()));
        lblResultadoEstadisticas.setText(validLaps + " vueltas válidas · "
                + session.getEventos().size() + " eventos · "
                + session.getParadasBoxes().size() + " paradas · media "
                + (average <= 0 ? "—" : FormatUtils.formatLapTime(average)));
        lblResultadoConfiguracion.setText(configurationSummary(session.getConfig()));
    }

    private void showDriver(LapResult result) {
        listaRadioResultado.getChildren().clear();
        if (result == null || session == null) {
            lblDetallePiloto.setText("Sin resultado seleccionado");
            lblDetalleEquipo.setText("—");
            lblDetalleResultado.setText("—");
            lblDetalleSectores.setText("—");
            lblDetalleNeumaticos.setText("—");
            lblDetalleBoxes.setText("—");
            lblDetalleEventos.setText("—");
            lblDetalleVehiculo.setText("—");
            lblDetalleConfiguracion.setText("—");
            addRadioLine("Sin mensajes relevantes", "post-radio-muted");
            return;
        }

        List<PitStopRecord> stops = pitStopsFor(result.getPilotoId());
        List<EventOccurrence> events = eventsFor(result.getPilotoId());
        List<TireChangeRecord> changes = tireChangesFor(result.getPilotoId());
        TireCompound initial = initialCompound(result.getPilotoId());
        TireCompound current = finalCompound(result.getPilotoId());
        double pitLoss = stops.stream().mapToDouble(PitStopRecord::tiempoPerdidoSegundos).sum();

        lblDetallePiloto.setText("P" + result.getPosicion() + " · " + result.getPiloto());
        lblDetalleEquipo.setText(result.getEquipo() + " · " + result.getEstadoVuelta().getEtiqueta());
        lblDetalleResultado.setText(FormatUtils.formatLapResult(result) + " · gap "
                + (result.getPosicion() == 1 ? "pole" : FormatUtils.formatGap(result.getGap())));
        lblDetalleSectores.setText(sectorSummary(result));
        lblDetalleNeumaticos.setText(initial.getCodigo() + " → " + current.getCodigo()
                + " · " + changes.size() + " cambios · desgaste "
                + String.format(Locale.ROOT, "%.1f %%", result.getDesgasteEstimado()));
        lblDetalleBoxes.setText(stops.size() + " paradas · pérdida "
                + String.format(Locale.ROOT, "+%.3f s", pitLoss));
        lblDetalleEventos.setText(events.size() + " eventos registrados");
        lblDetalleVehiculo.setText(result.getVehiculo() + " · " + result.getEquipo());
        lblDetalleConfiguracion.setText(driverConfiguration(result.getPilotoId()));
        populateRadio(events, stops, changes);
    }

    private void populateRadio(List<EventOccurrence> events, List<PitStopRecord> stops,
                               List<TireChangeRecord> changes) {
        events.forEach(event -> addRadioLine(
                "EVENTO · " + event.tipo().getEtiqueta() + " · " + event.sector().getEtiqueta()
                        + String.format(Locale.ROOT, " · %+.3f s",
                                event.impacto().deltaTiempoSegundos()),
                "post-radio-event"));
        stops.forEach(stop -> addRadioLine(
                "BOX · " + stop.motivo().getEtiqueta() + String.format(Locale.ROOT,
                        " · detenido %.3f s · pérdida +%.3f s",
                        stop.tiempoDetenidoSegundos(), stop.tiempoPerdidoSegundos()),
                "post-radio-pit"));
        changes.forEach(change -> addRadioLine(
                "NEUMÁTICOS · " + change.anterior().getCodigo() + " → "
                        + change.nuevo().getCodigo() + " · segmento " + change.segmento(),
                "post-radio-tire"));
        if (events.isEmpty() && stops.isEmpty() && changes.isEmpty()) {
            addRadioLine("Vuelta limpia · sin mensajes relevantes", "post-radio-muted");
        }
    }

    private void addRadioLine(String text, String styleClass) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        label.getStyleClass().addAll("post-radio-line", styleClass);
        listaRadioResultado.getChildren().add(label);
    }

    private SimpleStringProperty sector(LapResult result, TrackSector sector) {
        return new SimpleStringProperty(!result.hasSectorTimes() ? "—"
                : FormatUtils.formatLapTime(result.getSectorTimes().tiempoDe(sector)));
    }

    private String sectorSummary(LapResult result) {
        if (!result.hasSectorTimes()) {
            return "S1 — · S2 — · S3 —";
        }
        return "S1 " + FormatUtils.formatLapTime(result.getSectorTimes().sector1Seconds())
                + " · S2 " + FormatUtils.formatLapTime(result.getSectorTimes().sector2Seconds())
                + " · S3 " + FormatUtils.formatLapTime(result.getSectorTimes().sector3Seconds());
    }

    private TireCompound initialCompound(int driverId) {
        SimulationConfig config = session == null ? null : session.getConfig();
        return config != null && config.getPilotoId() != null
                && config.getPilotoId() == driverId
                ? config.getCompuestoInicial() : TireCompound.MEDIUM;
    }

    private TireCompound finalCompound(int driverId) {
        return tireChangesFor(driverId).stream()
                .max(Comparator.comparingInt(TireChangeRecord::segmento))
                .map(TireChangeRecord::nuevo)
                .orElse(initialCompound(driverId));
    }

    private List<EventOccurrence> eventsFor(int driverId) {
        return session == null ? List.of() : session.getEventos().stream()
                .filter(EventOccurrence::ocurrio)
                .filter(event -> event.pilotoId() == driverId)
                .toList();
    }

    private List<PitStopRecord> pitStopsFor(int driverId) {
        return session == null ? List.of() : session.getParadasBoxes().stream()
                .filter(stop -> stop.pilotoId() == driverId)
                .toList();
    }

    private List<TireChangeRecord> tireChangesFor(int driverId) {
        return session == null ? List.of() : session.getCambiosNeumaticos().stream()
                .filter(change -> change.pilotoId() == driverId)
                .toList();
    }

    private String driverConfiguration(int driverId) {
        SimulationConfig config = session.getConfig();
        if (config == null || config.getPilotoId() == null || config.getPilotoId() != driverId) {
            return "Estrategia automática de clasificación";
        }
        return configurationSummary(config);
    }

    private String configurationSummary(SimulationConfig config) {
        if (config == null) {
            return "Configuración no disponible";
        }
        return (config.getModo() == null ? "Modo —" : config.getModo().getEtiqueta())
                + " · aero " + (config.getAerodinamica() == null
                        ? "—" : config.getAerodinamica().getEtiqueta())
                + " · presión " + (config.getPresion() == null
                        ? "—" : config.getPresion().getEtiqueta()) + " · compuesto "
                + config.getCompuestoInicial().getCodigo() + " · combustible "
                + (config.getCombustible() == null
                        ? "—" : config.getCombustible().getEtiqueta());
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "Fecha —" : value;
    }

    private void clear() {
        tablaResultadoFinal.getItems().clear();
        lblResultadoCircuito.setText("RESULTADOS");
        lblResultadoSesion.setText("Sin sesión terminada");
        lblResultadoPole.setText("—");
        lblResultadoEstadisticas.setText("—");
        lblResultadoConfiguracion.setText("—");
        showDriver(null);
    }
}
