package com.formula1.controller;

import com.formula1.model.Team;
import com.formula1.service.TeamService;
import com.formula1.service.TeamServiceImpl;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;

public class TeamController {

    @FXML
    private TableView<Team> tableTeams;

    private final TeamService teamService;

    public TeamController() {
        this(new TeamServiceImpl());
    }

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @FXML
    public void initialize() {
        tableTeams.setItems(FXCollections.observableArrayList(teamService.findAll()));
    }

    @FXML
    private void onAddTeam() {
    }

    @FXML
    private void onDeleteTeam() {
    }
}
