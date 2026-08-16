package com.formula1.controller;

import com.formula1.model.Team;
import com.formula1.service.TeamService;
import com.formula1.service.ValidationException;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.util.stream.Collectors;

public class TeamController {

    @FXML private TableView<Team> tabla;
    @FXML private TableColumn<Team, String> colNombre;
    @FXML private TableColumn<Team, String> colPais;
    @FXML private TableColumn<Team, String> colMotor;
    @FXML private TableColumn<Team, Number> colNumPilotos;
    @FXML private TableColumn<Team, String> colPilotos;
    @FXML private TextField buscador;

    private final TeamService equipos;

    public TeamController() {
        this(new TeamService());
    }

    public TeamController(TeamService equipos) {
        this.equipos = equipos;
    }

    @FXML
    public void initialize() {
        colNombre.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().getNombre()));
        colPais.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().getPais()));
        colMotor.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().getMotor()));
        colNumPilotos.setCellValueFactory(f -> new SimpleIntegerProperty(f.getValue().getPilotos().size()));
        colPilotos.setCellValueFactory(f -> new SimpleStringProperty(
                equipos.pilotosDe(f.getValue()).stream()
                        .map(p -> p.getNombre())
                        .collect(Collectors.joining(", "))));

        buscador.textProperty().addListener((obs, antes, ahora) -> refrescar(ahora));
        refrescar("");
    }

    private void refrescar(String filtro) {
        tabla.setItems(FXCollections.observableArrayList(equipos.buscar(filtro)));
    }

    @FXML
    private void onNuevo() {
        Forms.equipo(null).ifPresent(this::guardar);
    }

    @FXML
    private void onEditar() {
        Team seleccionado = tabla.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            Navigator.aviso("Sin selección", "Elige un equipo de la tabla.");
            return;
        }
        Forms.equipo(seleccionado).ifPresent(this::guardar);
    }

    @FXML
    private void onEliminar() {
        Team seleccionado = tabla.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            Navigator.aviso("Sin selección", "Elige un equipo de la tabla.");
            return;
        }
        if (!Navigator.confirmar("¿Eliminar " + seleccionado.getNombre() + "?")) {
            return;
        }
        try {
            equipos.eliminar(seleccionado.getNombre());
            refrescar(buscador.getText());
        } catch (ValidationException e) {
            Navigator.error("No se puede eliminar", e.getMessage());
        }
    }

    private void guardar(Team equipo) {
        try {
            equipos.guardar(equipo);
            refrescar(buscador.getText());
        } catch (ValidationException e) {
            Navigator.error("Datos no válidos", e.getMessage());
        }
    }
}
