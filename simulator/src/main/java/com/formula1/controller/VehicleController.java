package com.formula1.controller;

import com.formula1.model.DrivingMode;
import com.formula1.model.Vehicle;
import com.formula1.service.TeamService;
import com.formula1.service.DriverService;
import com.formula1.service.ValidationException;
import com.formula1.service.VehicleService;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

import java.util.stream.Collectors;

public class VehicleController {

    @FXML private TableView<Vehicle> tabla;
    @FXML private TableColumn<Vehicle, String> colModelo;
    @FXML private TableColumn<Vehicle, String> colEquipo;
    @FXML private TableColumn<Vehicle, String> colMotor;
    @FXML private TableColumn<Vehicle, Number> colVelMax;
    @FXML private TableColumn<Vehicle, Number> colAceleracion;
    @FXML private TableColumn<Vehicle, Number> colVelNormal;
    @FXML private TableColumn<Vehicle, String> colPilotos;
    @FXML private TableColumn<Vehicle, Void> colAcciones;
    @FXML private TextField buscador;
    @FXML private Spinner<Integer> velocidadMinima;
    @FXML private Label lblConteo;

    private final VehicleService vehiculos;
    private final TeamService equipos;
    private final DriverService pilotos;

    public VehicleController() {
        this(new VehicleService(), new TeamService(), new DriverService());
    }

    public VehicleController(VehicleService vehiculos, TeamService equipos) {
        this(vehiculos, equipos, new DriverService());
    }

    public VehicleController(VehicleService vehiculos, TeamService equipos, DriverService pilotos) {
        this.vehiculos = vehiculos;
        this.equipos = equipos;
        this.pilotos = pilotos;
    }

    @FXML
    public void initialize() {
        tabla.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        colModelo.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().getModelo()));
        colEquipo.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().getEquipo()));
        colMotor.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().getMotor()));
        colVelMax.setCellValueFactory(f -> new SimpleIntegerProperty(f.getValue().getVelocidadMaximaKmh()));
        colAceleracion.setCellValueFactory(f -> new SimpleDoubleProperty(f.getValue().getAceleracion0100()));
        colVelNormal.setCellValueFactory(f -> new SimpleIntegerProperty(
                f.getValue().rendimientoDe(DrivingMode.NORMAL).getVelocidadPromedioKmh()));
        colPilotos.setCellValueFactory(f -> new SimpleStringProperty(
                vehiculos.pilotosDe(f.getValue()).stream()
                        .map(p -> p.getNombre())
                        .collect(Collectors.joining(", "))));
        colAcciones.setCellFactory(col -> celdaAcciones());

        velocidadMinima.setValueFactory(
                new javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory(0, 400, 0, 5));
        buscador.textProperty().addListener((obs, antes, ahora) -> refrescar());
        velocidadMinima.valueProperty().addListener((obs, antes, ahora) -> refrescar());
        refrescar();
    }

    private void refrescar() {
        Integer minima = velocidadMinima.getValue() == null || velocidadMinima.getValue() == 0
                ? null : velocidadMinima.getValue();
        var items = vehiculos.buscar(buscador.getText(), minima);
        tabla.setItems(FXCollections.observableArrayList(items));
        lblConteo.setText(String.valueOf(items.size()));
    }

    private TableCell<Vehicle, Void> celdaAcciones() {
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
        Forms.vehiculo(null, equipos.listar(), pilotos.listar()).ifPresent(this::guardar);
    }

    private void editar(Vehicle vehiculo) {
        Forms.vehiculo(vehiculo, equipos.listar(), pilotos.listar()).ifPresent(this::guardar);
    }

    private void eliminar(Vehicle vehiculo) {
        if (Navigator.confirmar("¿Eliminar el " + vehiculo.getModelo() + "?")) {
            vehiculos.eliminar(vehiculo.getModelo());
            refrescar();
        }
    }

    /** Asigna pilotos al vehículo; solo se ofrecen los de su mismo equipo. */
    @FXML
    private void onAsignarPilotos() {
        Vehicle seleccionado = tabla.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            Navigator.aviso("Sin selección", "Elige un vehículo de la tabla.");
            return;
        }
        Forms.asignarPilotos(seleccionado, pilotos.porEquipo(seleccionado.getEquipo()))
                .ifPresent(ids -> {
                    try {
                        vehiculos.asignarPilotos(seleccionado, ids);
                        refrescar();
                    } catch (ValidationException e) {
                        Navigator.error("Asignación no válida", e.getMessage());
                    }
                });
    }

    @FXML
    private void onComparar() {
        var seleccion = tabla.getSelectionModel().getSelectedItems();
        if (seleccion.size() < 2) {
            Navigator.aviso("Selecciona al menos dos",
                    "Marca dos o más vehículos (Ctrl + clic) para compararlos.");
            return;
        }
        var modelos = seleccion.stream().map(Vehicle::getModelo).collect(Collectors.toList());
        Navigator.ir("vehicle-compare");
        if (Navigator.ultimoControlador() instanceof VehicleCompareController comparador) {
            comparador.comparar(modelos);
        }
    }

    private void guardar(Vehicle vehiculo) {
        try {
            vehiculos.guardar(vehiculo);
            refrescar();
        } catch (ValidationException e) {
            Navigator.error("Datos no válidos", e.getMessage());
        }
    }
}
