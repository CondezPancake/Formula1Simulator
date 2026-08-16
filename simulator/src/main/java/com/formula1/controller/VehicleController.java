package com.formula1.controller;

import com.formula1.model.Vehicle;
import com.formula1.service.VehicleService;
import com.formula1.service.VehicleServiceImpl;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;

public class VehicleController {

    @FXML
    private TableView<Vehicle> tableVehicles;

    private final VehicleService vehicleService;

    public VehicleController() {
        this(new VehicleServiceImpl());
    }

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @FXML
    public void initialize() {
        tableVehicles.setItems(FXCollections.observableArrayList(vehicleService.findAll()));
    }

    @FXML
    private void onAddVehicle() {
    }

    @FXML
    private void onDeleteVehicle() {
    }
}
