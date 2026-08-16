package com.formula1.controller;

import com.formula1.model.Circuit;
import com.formula1.service.CircuitService;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;

public class CircuitController {

    @FXML
    private TableView<Circuit> tableCircuits;

    private final CircuitService circuitos;

    public CircuitController() {
        this(new CircuitService());
    }

    public CircuitController(CircuitService circuitos) {
        this.circuitos = circuitos;
    }

    @FXML
    public void initialize() {
        tableCircuits.setItems(FXCollections.observableArrayList(circuitos.listar()));
    }

    @FXML
    private void onAddCircuit() {
    }

    @FXML
    private void onDeleteCircuit() {
    }
}
