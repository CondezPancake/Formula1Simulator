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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

public class CircuitController {

    @FXML private TableView<Circuit> tabla;
    @FXML private TableColumn<Circuit, String> colNombre;
    @FXML private TableColumn<Circuit, String> colPais;
    @FXML private TableColumn<Circuit, Number> colLongitud;
    @FXML private TableColumn<Circuit, Number> colVueltas;
    @FXML private TableColumn<Circuit, String> colRecord;
    @FXML private TableColumn<Circuit, Number> colFactor;
    @FXML private TextField buscador;

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

        tabla.setOnMouseClicked(evento -> {
            if (evento.getClickCount() == 2) {
                abrirDetalle();
            }
        });

        buscador.textProperty().addListener((obs, antes, ahora) -> refrescar(ahora));
        refrescar("");
    }

    private void refrescar(String filtro) {
        tabla.setItems(FXCollections.observableArrayList(circuitos.buscar(filtro)));
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
        Navigator.ir("circuit-detail");
        if (Navigator.ultimoControlador() instanceof CircuitDetailController detalle) {
            detalle.mostrar(seleccionado.getNombre());
        }
    }

    @FXML
    private void onNuevo() {
        Forms.circuito(null).ifPresent(this::guardar);
    }

    @FXML
    private void onEditar() {
        Circuit seleccionado = tabla.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            Navigator.aviso("Sin selección", "Elige un circuito de la tabla.");
            return;
        }
        Forms.circuito(seleccionado).ifPresent(this::guardar);
    }

    @FXML
    private void onEliminar() {
        Circuit seleccionado = tabla.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            Navigator.aviso("Sin selección", "Elige un circuito de la tabla.");
            return;
        }
        if (Navigator.confirmar("¿Eliminar " + seleccionado.getNombre() + "?")) {
            circuitos.eliminar(seleccionado.getNombre());
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
