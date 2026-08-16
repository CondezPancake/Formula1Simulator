package com.formula1.controller;

import com.formula1.model.Vehicle;
import com.formula1.service.VehicleService;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;

public class VehicleController {

    @FXML
    private TableView<Vehicle> tableVehicles;

    private final VehicleService vehiculos;

    public VehicleController() {
        this(new VehicleService());
    }

    public VehicleController(VehicleService vehiculos) {
        this.vehiculos = vehiculos;
    }

    @FXML
    public void initialize() {
        tableVehicles.setItems(FXCollections.observableArrayList(vehiculos.listar()));
    }

    @FXML
    private void onAddVehicle() {
    }

    @FXML
    private void onDeleteVehicle() {
    }
}
