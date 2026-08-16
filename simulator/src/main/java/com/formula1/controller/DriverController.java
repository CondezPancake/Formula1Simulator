package com.formula1.controller;

import com.formula1.model.Driver;
import com.formula1.service.DriverService;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;

public class DriverController {

    @FXML
    private TableView<Driver> tableDrivers;

    private final DriverService pilotos;

    public DriverController() {
        this(new DriverService());
    }

    public DriverController(DriverService pilotos) {
        this.pilotos = pilotos;
    }

    @FXML
    public void initialize() {
        tableDrivers.setItems(FXCollections.observableArrayList(pilotos.listar()));
    }

    @FXML
    private void onAddDriver() {
    }

    @FXML
    private void onDeleteDriver() {
    }
}
