package com.formula1.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class DashboardController {

    @FXML
    private Label lblTitle;

    @FXML
    public void initialize() {
        lblTitle.setText("Formula 1 Qualifying Simulator");
    }
}
