package com.formula1.controller;

import com.formula1.model.Result;
import com.formula1.service.SimulationService;
import com.formula1.simulation.SimulationFacade;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;

public class SimulationController {

    @FXML
    private TableView<Result> tableResults;

    @FXML
    private Button btnStartQualifying;

    private final SimulationService simulationService;

    public SimulationController() {
        this(new SimulationFacade());
    }

    public SimulationController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @FXML
    public void initialize() {
        tableResults.setItems(FXCollections.observableArrayList());
    }

    @FXML
    private void onStartQualifying() {
    }
}
