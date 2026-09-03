package com.formula1.adapter.in.javafx;

import com.formula1.domain.model.Driver;
import com.formula1.application.usecase.DriverService;
import com.formula1.application.usecase.TeamService;
import com.formula1.domain.service.ValidationException;
import com.formula1.util.InputValidation;

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
    @FXML private TableColumn<Driver, Void> colAcciones;
    @FXML private TextField buscador;
    @FXML private Label lblConteo;

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
        InputValidation.busqueda(buscador);
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
        colAcciones.setCellFactory(col -> celdaAcciones());

        buscador.textProperty().addListener((obs, antes, ahora) -> refrescar(ahora));
        refrescar("");
    }

    private void refrescar(String filtro) {
        var items = pilotos.buscar(filtro);
        tabla.setItems(FXCollections.observableArrayList(items));
        lblConteo.setText(String.valueOf(items.size()));
    }

    private TableCell<Driver, Void> celdaAcciones() {
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
    private void onNuevo() {
        Forms.piloto(null, equipos.listar(), pilotos.siguienteId())
                .ifPresent(this::guardar);
    }

    private void editar(Driver piloto) {
        Forms.piloto(piloto, equipos.listar(), piloto.getId())
                .ifPresent(this::guardar);
    }

    private void eliminar(Driver piloto) {
        if (Navigator.confirmar("¿Eliminar a " + piloto.getNombre() + "?")) {
            pilotos.eliminar(piloto.getId());
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
