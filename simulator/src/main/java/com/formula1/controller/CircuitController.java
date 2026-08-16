package com.formula1.controller;

import com.formula1.model.Circuit;
import com.formula1.service.CircuitService;
import com.formula1.service.CircuitServiceImpl;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;

public class CircuitController {

    @FXML
    private TableView<Circuit> tableCircuits;

    private final CircuitService circuitService;

    public CircuitController() {
        this(new CircuitServiceImpl());
    }

    public CircuitController(CircuitService circuitService) {
        this.circuitService = circuitService;
    }

    @FXML
    public void initialize() {
        tableCircuits.setItems(FXCollections.observableArrayList(circuitService.findAll()));
    }

    @FXML
    private void onAddCircuit() {
    }

    @FXML
    private void onDeleteCircuit() {
    }
}
