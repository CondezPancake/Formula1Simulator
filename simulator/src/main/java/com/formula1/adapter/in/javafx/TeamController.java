package com.formula1.adapter.in.javafx;

import com.formula1.domain.model.Team;
import com.formula1.application.usecase.TeamService;
import com.formula1.domain.service.ValidationException;
import com.formula1.util.InputValidation;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

import java.util.stream.Collectors;

public class TeamController {

    @FXML private TableView<Team> tabla;
    @FXML private TableColumn<Team, String> colNombre;
    @FXML private TableColumn<Team, String> colPais;
    @FXML private TableColumn<Team, String> colMotor;
    @FXML private TableColumn<Team, Number> colNumPilotos;
    @FXML private TableColumn<Team, String> colPilotos;
    @FXML private TableColumn<Team, Void> colAcciones;
    @FXML private TextField buscador;
    @FXML private Label lblConteo;

    private final TeamService equipos;

    public TeamController() {
        this(new TeamService());
    }

    public TeamController(TeamService equipos) {
        this.equipos = equipos;
    }

    @FXML
    public void initialize() {
        InputValidation.busqueda(buscador);
        colNombre.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().getNombre()));
        colPais.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().getPais()));
        colMotor.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().getMotor()));
        colNumPilotos.setCellValueFactory(f -> new SimpleIntegerProperty(f.getValue().getPilotos().size()));
        colPilotos.setCellValueFactory(f -> new SimpleStringProperty(
                equipos.pilotosDe(f.getValue()).stream()
                        .map(p -> p.getNombre())
                        .collect(Collectors.joining(", "))));
        colAcciones.setCellFactory(col -> celdaAcciones());

        buscador.textProperty().addListener((obs, antes, ahora) -> refrescar(ahora));
        refrescar("");
    }

    private void refrescar(String filtro) {
        var items = equipos.buscar(filtro);
        tabla.setItems(FXCollections.observableArrayList(items));
        lblConteo.setText(String.valueOf(items.size()));
    }

    /** Recalcula columnas derivadas cuando otro catálogo modifica pilotos. */
    void refrescarVista() {
        refrescar(buscador.getText());
        tabla.refresh();
    }

    private TableCell<Team, Void> celdaAcciones() {
        return new TableCell<>() {
            private final Button editar = new Button("Editar");
            private final Button eliminar = new Button("Eliminar");
            private final HBox caja = new HBox(6, editar, eliminar);
            {
                caja.setAlignment(Pos.CENTER_LEFT);
                editar.getStyleClass().add("icon-button");
                eliminar.getStyleClass().addAll("icon-button", "danger");
                editar.setOnAction(e -> editar(getTableView().getItems().get(getIndex())));
                eliminar.setOnAction(e -> eliminar(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void valor, boolean vacio) {
                super.updateItem(valor, vacio);
                setGraphic(vacio ? null : caja);
            }
        };
    }

    @FXML
    private void onNuevo() {
        Forms.equipo(null).ifPresent(this::guardar);
    }

    private void editar(Team equipo) {
        Forms.equipo(equipo).ifPresent(this::guardar);
    }

    private void eliminar(Team equipo) {
        if (!Navigator.confirmar("¿Eliminar " + equipo.getNombre() + "?")) {
            return;
        }
        try {
            equipos.eliminar(equipo.getNombre());
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
