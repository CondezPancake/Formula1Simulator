package com.formula1.controller;

import com.formula1.model.Circuit;
import com.formula1.service.CircuitService;
import com.formula1.service.ValidationException;
import com.formula1.util.FormatUtils;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

public class CircuitController {

    @FXML private TableView<Circuit> tabla;
    @FXML private TableColumn<Circuit, String> colNombre;
    @FXML private TableColumn<Circuit, String> colPais;
    @FXML private TableColumn<Circuit, Number> colLongitud;
    @FXML private TableColumn<Circuit, Number> colVueltas;
    @FXML private TableColumn<Circuit, String> colRecord;
    @FXML private TableColumn<Circuit, Number> colFactor;
    @FXML private TableColumn<Circuit, Void> colAcciones;
    @FXML private TextField buscador;
    @FXML private Label lblConteo;

    private final CircuitService circuitos;

    public CircuitController() {
        this(new CircuitService());
    }

    public CircuitController(CircuitService circuitos) {
        this.circuitos = circuitos;
    }

    @FXML
    public void initialize() {
        colNombre.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().getNombre()));
        colPais.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().getPais()));
        colLongitud.setCellValueFactory(f -> new SimpleDoubleProperty(f.getValue().getLongitudKm()));
        colVueltas.setCellValueFactory(f -> new SimpleIntegerProperty(f.getValue().getVueltas()));
        colRecord.setCellValueFactory(f -> new SimpleStringProperty(
                f.getValue().getRecordVuelta() == null ? "—"
                        : FormatUtils.formatLapTime(f.getValue().getRecordVuelta().getTiempoSegundos())));
        colFactor.setCellValueFactory(f -> new SimpleDoubleProperty(f.getValue().getFactorTecnico()));
        colAcciones.setCellFactory(col -> celdaAcciones());

        tabla.setOnMouseClicked(evento -> {
            if (evento.getClickCount() == 2) {
                abrirDetalle();
            }
        });

        buscador.textProperty().addListener((obs, antes, ahora) -> refrescar(ahora));
        refrescar("");
    }

    private void refrescar(String filtro) {
        var items = circuitos.buscar(filtro);
        tabla.setItems(FXCollections.observableArrayList(items));
        lblConteo.setText(String.valueOf(items.size()));
    }

    private TableCell<Circuit, Void> celdaAcciones() {
        return new TableCell<>() {
            private final Button editar = new Button("Editar");
            private final Button eliminar = new Button("Eliminar");
            private final HBox caja = new HBox(6, editar, eliminar);
            {
                caja.setAlignment(Pos.CENTER_LEFT);
                editar.getStyleClass().add("icon-button");
                eliminar.getStyleClass().addAll("icon-button", "danger");
                editar.setOnAction(e -> editar(getTableView().getItems().get(getIndex())));
                eliminar.setOnAction(e -> eliminar(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void valor, boolean vacio) {
                super.updateItem(valor, vacio);
                setGraphic(vacio ? null : caja);
            }
        };
    }

    @FXML
    private void onVerDetalle() {
        abrirDetalle();
    }

    private void abrirDetalle() {
        Circuit seleccionado = tabla.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            Navigator.aviso("Sin selección", "Elige un circuito de la tabla.");
            return;
        }
        Navigator.irConRetorno("circuit-detail");
        if (Navigator.ultimoControlador() instanceof CircuitDetailController detalle) {
            detalle.mostrar(seleccionado.getNombre());
        }
    }

    @FXML
    private void onNuevo() {
        Forms.circuito(null).ifPresent(this::guardar);
    }

    private void editar(Circuit circuito) {
        Forms.circuito(circuito).ifPresent(this::guardar);
    }

    private void eliminar(Circuit circuito) {
        if (Navigator.confirmar("¿Eliminar " + circuito.getNombre() + "?")) {
            circuitos.eliminar(circuito.getNombre());
            refrescar(buscador.getText());
        }
    }

    private void guardar(Circuit circuito) {
        try {
            circuitos.guardar(circuito);
            refrescar(buscador.getText());
        } catch (ValidationException e) {
            Navigator.error("Datos no válidos", e.getMessage());
        }
    }
}
