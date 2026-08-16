package com.formula1.controller;

import com.formula1.model.Driver;
import com.formula1.service.DriverService;
import com.formula1.service.TeamService;
import com.formula1.service.ValidationException;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

public class DriverController {

    @FXML private TableView<Driver> tabla;
    @FXML private TableColumn<Driver, Number> colId;
    @FXML private TableColumn<Driver, String> colNombre;
    @FXML private TableColumn<Driver, String> colEquipo;
    @FXML private TableColumn<Driver, String> colRol;
    @FXML private TableColumn<Driver, Number> colExperiencia;
    @FXML private TableColumn<Driver, Number> colVelocidad;
    @FXML private TableColumn<Driver, Number> colConsistencia;
    @FXML private TableColumn<Driver, Number> colLluvia;
    @FXML private TextField buscador;

    private final DriverService pilotos;
    private final TeamService equipos;

    public DriverController() {
        this(new DriverService(), new TeamService());
    }

    public DriverController(DriverService pilotos, TeamService equipos) {
        this.pilotos = pilotos;
        this.equipos = equipos;
    }

    @FXML
    public void initialize() {
        colId.setCellValueFactory(f -> new SimpleIntegerProperty(f.getValue().getId()));
        colNombre.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().getNombre()));
        colEquipo.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().getEquipo()));
        colRol.setCellValueFactory(f -> new SimpleStringProperty(
                f.getValue().getRol() == null ? "" : f.getValue().getRol().getEtiqueta()));
        colExperiencia.setCellValueFactory(f -> new SimpleIntegerProperty(f.getValue().getExperiencia()));
        colVelocidad.setCellValueFactory(f -> new SimpleIntegerProperty(
                f.getValue().getHabilidad(Driver.HABILIDAD_VELOCIDAD)));
        colConsistencia.setCellValueFactory(f -> new SimpleIntegerProperty(
                f.getValue().getHabilidad(Driver.HABILIDAD_CONSISTENCIA)));
        colLluvia.setCellValueFactory(f -> new SimpleIntegerProperty(
                f.getValue().getHabilidad(Driver.HABILIDAD_LLUVIA)));

        buscador.textProperty().addListener((obs, antes, ahora) -> refrescar(ahora));
        refrescar("");
    }

    private void refrescar(String filtro) {
        tabla.setItems(FXCollections.observableArrayList(pilotos.buscar(filtro)));
    }

    @FXML
    private void onNuevo() {
        Forms.piloto(null, equipos.listar(), pilotos.siguienteId())
                .ifPresent(this::guardar);
    }

    @FXML
    private void onEditar() {
        Driver seleccionado = tabla.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            Navigator.aviso("Sin selección", "Elige un piloto de la tabla.");
            return;
        }
        Forms.piloto(seleccionado, equipos.listar(), seleccionado.getId())
                .ifPresent(this::guardar);
    }

    @FXML
    private void onEliminar() {
        Driver seleccionado = tabla.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            Navigator.aviso("Sin selección", "Elige un piloto de la tabla.");
            return;
        }
        if (Navigator.confirmar("¿Eliminar a " + seleccionado.getNombre() + "?")) {
            pilotos.eliminar(seleccionado.getId());
            refrescar(buscador.getText());
        }
    }

    private void guardar(Driver piloto) {
        try {
            pilotos.guardar(piloto);
            refrescar(buscador.getText());
        } catch (ValidationException e) {
            Navigator.error("Datos no válidos", e.getMessage());
        }
    }
}
