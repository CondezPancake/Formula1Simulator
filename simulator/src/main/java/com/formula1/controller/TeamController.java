package com.formula1.controller;

import com.formula1.model.Team;
import com.formula1.service.TeamService;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;

public class TeamController {

    @FXML
    private TableView<Team> tableTeams;

    private final TeamService equipos;

    public TeamController() {
        this(new TeamService());
    }

    public TeamController(TeamService equipos) {
        this.equipos = equipos;
    }

    @FXML
    public void initialize() {
        tableTeams.setItems(FXCollections.observableArrayList(equipos.listar()));
    }

    @FXML
    private void onAddTeam() {
    }

    @FXML
    private void onDeleteTeam() {
    }
}
