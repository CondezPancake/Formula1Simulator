package com.formula1.controller;

import com.formula1.model.Driver;
import com.formula1.service.DriverService;
import com.formula1.service.DriverServiceImpl;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;

public class DriverController {

    @FXML
    private TableView<Driver> tableDrivers;

    private final DriverService driverService;

    public DriverController() {
        this(new DriverServiceImpl());
    }

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    @FXML
    public void initialize() {
        tableDrivers.setItems(FXCollections.observableArrayList(driverService.findAll()));
    }

    @FXML
    private void onAddDriver() {
    }

    @FXML
    private void onDeleteDriver() {
    }
}
