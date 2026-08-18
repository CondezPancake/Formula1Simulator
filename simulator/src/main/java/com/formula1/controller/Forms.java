package com.formula1.controller;

import com.formula1.model.Circuit;
import com.formula1.model.Driver;
import com.formula1.model.DriverRole;
import com.formula1.model.DrivingMode;
import com.formula1.model.Team;
import com.formula1.model.Vehicle;
import com.formula1.model.WeatherCondition;
import com.formula1.util.FormatUtils;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Diálogos de alta y edición.
 *
 * Se construyen en código en lugar de con un FXML y un controlador por
 * entidad: son formularios cortos y muy parecidos entre sí, así que
 * agruparlos aquí ahorra ocho archivos sin perder claridad.
 */
public final class Forms {

    private Forms() {
    }

    private static <T> Dialog<T> base(String titulo) {
        Dialog<T> dialogo = new Dialog<>();
        dialogo.setTitle(titulo);
        dialogo.setHeaderText(titulo);
        dialogo.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialogo.getDialogPane().getStylesheets().addAll(
                Forms.class.getResource("/css/style.css").toExternalForm());
        return dialogo;
    }

    private static GridPane rejilla() {
        GridPane rejilla = new GridPane();
        rejilla.setHgap(10);
        rejilla.setVgap(10);
        rejilla.setPadding(new Insets(16));
        return rejilla;
    }

    private static Spinner<Integer> spinner(int min, int max, int valor) {
        Spinner<Integer> control = new Spinner<>(min, max, valor);
        control.setEditable(true);
        control.setPrefWidth(110);
        return control;
    }

    // ------------------------------------------------------------ pilotos

    public static Optional<Driver> piloto(Driver original, List<Team> equipos, int idSugerido) {
        boolean nuevo = original == null;
        Driver piloto = nuevo ? new Driver() : original;

        TextField nombre = new TextField(nuevo ? "" : piloto.getNombre());
        ComboBox<String> equipo = new ComboBox<>();
        equipos.forEach(e -> equipo.getItems().add(e.getNombre()));
        equipo.setValue(nuevo ? (equipos.isEmpty() ? null : equipos.get(0).getNombre()) : piloto.getEquipo());
        ComboBox<DriverRole> rol = new ComboBox<>();
        rol.getItems().addAll(DriverRole.values());
        rol.setValue(nuevo ? DriverRole.ESCUDERO : piloto.getRol());
        Spinner<Integer> experiencia = spinner(0, 30, nuevo ? 0 : piloto.getExperiencia());
        Spinner<Integer> velocidad = spinner(0, 100, piloto.getHabilidad(Driver.HABILIDAD_VELOCIDAD));
        Spinner<Integer> consistencia = spinner(0, 100, piloto.getHabilidad(Driver.HABILIDAD_CONSISTENCIA));
        Spinner<Integer> lluvia = spinner(0, 100, piloto.getHabilidad(Driver.HABILIDAD_LLUVIA));
        Tooltip ayudaVelocidad = new Tooltip(
                "Ritmo puro del piloto. 0 es bajo y 100 representa nivel élite.");
        Tooltip ayudaConsistencia = new Tooltip(
                "Capacidad para repetir un rendimiento estable. Escala de 0 a 100.");
        Tooltip ayudaLluvia = new Tooltip(
                "Rendimiento y control sobre pista mojada. Escala de 0 a 100.");
        velocidad.setTooltip(ayudaVelocidad);
        consistencia.setTooltip(ayudaConsistencia);
        lluvia.setTooltip(ayudaLluvia);

        GridPane rejilla = rejilla();
        rejilla.addRow(0, new Label("Nombre"), nombre);
        rejilla.addRow(1, new Label("Equipo"), equipo);
        rejilla.addRow(2, new Label("Rol"), rol);
        rejilla.addRow(3, new Label("Experiencia (años)"), experiencia);
        rejilla.addRow(4, etiquetaConAyuda("Velocidad (0–100)", ayudaVelocidad), velocidad);
        rejilla.addRow(5, etiquetaConAyuda("Consistencia (0–100)", ayudaConsistencia), consistencia);
        rejilla.addRow(6, etiquetaConAyuda("Rendimiento en lluvia (0–100)", ayudaLluvia), lluvia);

        Dialog<Driver> dialogo = base(nuevo ? "Nuevo piloto" : "Editar piloto");
        dialogo.getDialogPane().setContent(rejilla);
        dialogo.setResultConverter(boton -> {
            if (boton != ButtonType.OK) {
                return null;
            }
            piloto.setId(nuevo ? idSugerido : piloto.getId());
            piloto.setNombre(nombre.getText());
            piloto.setEquipo(equipo.getValue());
            piloto.setRol(rol.getValue());
            piloto.setExperiencia(experiencia.getValue());
            piloto.setHabilidad(Driver.HABILIDAD_VELOCIDAD, velocidad.getValue());
            piloto.setHabilidad(Driver.HABILIDAD_CONSISTENCIA, consistencia.getValue());
            piloto.setHabilidad(Driver.HABILIDAD_LLUVIA, lluvia.getValue());
            return piloto;
        });
        return dialogo.showAndWait();
    }

    private static Label etiquetaConAyuda(String texto, Tooltip ayuda) {
        Label etiqueta = new Label(texto + "  ?");
        etiqueta.setTooltip(new Tooltip(ayuda.getText()));
        return etiqueta;
    }

    // ------------------------------------------------------------ equipos

    public static Optional<Team> equipo(Team original) {
        boolean nuevo = original == null;
        Team equipo = nuevo ? new Team() : original;

        TextField nombre = new TextField(nuevo ? "" : equipo.getNombre());
        nombre.setDisable(!nuevo); // el nombre es la clave: no se renombra
        TextField pais = new TextField(nuevo ? "" : equipo.getPais());
        TextField motor = new TextField(nuevo ? "" : equipo.getMotor());

        GridPane rejilla = rejilla();
        rejilla.addRow(0, new Label("Nombre"), nombre);
        rejilla.addRow(1, new Label("País"), pais);
        rejilla.addRow(2, new Label("Motor"), motor);

        Dialog<Team> dialogo = base(nuevo ? "Nuevo equipo" : "Editar equipo");
        dialogo.getDialogPane().setContent(rejilla);
        dialogo.setResultConverter(boton -> {
            if (boton != ButtonType.OK) {
                return null;
            }
            equipo.setNombre(nombre.getText());
            equipo.setPais(pais.getText());
            equipo.setMotor(motor.getText());
            return equipo;
        });
        return dialogo.showAndWait();
    }

    // ---------------------------------------------------------- vehículos

    public static Optional<Vehicle> vehiculo(Vehicle original, List<Team> equipos) {
        boolean nuevo = original == null;
        Vehicle vehiculo = nuevo ? new Vehicle() : original;

        TextField modelo = new TextField(nuevo ? "" : vehiculo.getModelo());
        modelo.setDisable(!nuevo);
        ComboBox<String> equipo = new ComboBox<>();
        equipos.forEach(e -> equipo.getItems().add(e.getNombre()));
        equipo.setValue(nuevo ? (equipos.isEmpty() ? null : equipos.get(0).getNombre()) : vehiculo.getEquipo());
        TextField motor = new TextField(nuevo ? "" : vehiculo.getMotor());
        Spinner<Integer> velocidad = spinner(100, 400, nuevo ? 340 : vehiculo.getVelocidadMaximaKmh());
        TextField aceleracion = new TextField(nuevo ? "2.7" : String.valueOf(vehiculo.getAceleracion0100()));

        GridPane datos = rejilla();
        datos.addRow(0, new Label("Modelo"), modelo);
        datos.addRow(1, new Label("Equipo"), equipo);
        datos.addRow(2, new Label("Motor"), motor);
        datos.addRow(3, new Label("Velocidad máx. (km/h)"), velocidad);
        datos.addRow(4, new Label("Aceleración 0-100 (s)"), aceleracion);

        // Una pestaña por modo de conducción, con su velocidad media y las
        // tablas de consumo y desgaste por clima.
        TabPane pestanas = new TabPane();
        pestanas.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        List<ModoControles> controles = new ArrayList<>();
        for (DrivingMode modo : DrivingMode.values()) {
            Vehicle.Performance actual = nuevo ? null : vehiculo.getRendimiento().get(modo);
            ModoControles fila = new ModoControles(modo, actual);
            controles.add(fila);
            pestanas.getTabs().add(new Tab(modo.getEtiqueta(), fila.panel()));
        }

        VBox contenido = new VBox(10, datos, pestanas);
        Dialog<Vehicle> dialogo = base(nuevo ? "Nuevo vehículo" : "Editar vehículo");
        dialogo.getDialogPane().setContent(contenido);
        dialogo.getDialogPane().setPrefWidth(520);
        dialogo.setResultConverter(boton -> {
            if (boton != ButtonType.OK) {
                return null;
            }
            vehiculo.setModelo(modelo.getText());
            vehiculo.setEquipo(equipo.getValue());
            vehiculo.setMotor(motor.getText());
            vehiculo.setVelocidadMaximaKmh(velocidad.getValue());
            vehiculo.setAceleracion0100(parseDouble(aceleracion.getText(), 2.7));
            controles.forEach(fila -> vehiculo.getRendimiento().put(fila.modo, fila.aPerformance()));
            return vehiculo;
        });
        return dialogo.showAndWait();
    }

    /** Selección de pilotos de un vehículo, limitada a los de su equipo. */
    public static Optional<List<Integer>> asignarPilotos(Vehicle vehiculo, List<Driver> candidatos) {
        VBox lista = new VBox(8);
        lista.setPadding(new Insets(16));
        lista.getChildren().add(new Label("Pilotos de " + vehiculo.getEquipo() + ":"));

        List<CheckBox> casillas = new ArrayList<>();
        for (Driver piloto : candidatos) {
            CheckBox casilla = new CheckBox(piloto.getNombre() + "  (" + piloto.getRol() + ")");
            casilla.setSelected(vehiculo.conduce(piloto.getId()));
            casilla.setUserData(piloto.getId());
            casillas.add(casilla);
            lista.getChildren().add(casilla);
        }
        if (candidatos.isEmpty()) {
            lista.getChildren().add(new Label("Este equipo no tiene pilotos registrados."));
        }

        Dialog<List<Integer>> dialogo = base("Asignar pilotos — " + vehiculo.getModelo());
        dialogo.getDialogPane().setContent(lista);
        dialogo.setResultConverter(boton -> {
            if (boton != ButtonType.OK) {
                return null;
            }
            List<Integer> elegidos = new ArrayList<>();
            casillas.stream().filter(CheckBox::isSelected)
                    .forEach(c -> elegidos.add((Integer) c.getUserData()));
            return elegidos;
        });
        return dialogo.showAndWait();
    }

    // ---------------------------------------------------------- circuitos

    public static Optional<Circuit> circuito(Circuit original) {
        boolean nuevo = original == null;
        Circuit circuito = nuevo ? new Circuit() : original;

        TextField nombre = new TextField(nuevo ? "" : circuito.getNombre());
        nombre.setDisable(!nuevo);
        TextField pais = new TextField(nuevo ? "" : circuito.getPais());
        TextField longitud = new TextField(nuevo ? "5.0" : String.valueOf(circuito.getLongitudKm()));
        Spinner<Integer> vueltas = spinner(1, 200, nuevo ? 50 : circuito.getVueltas());
        TextArea descripcion = new TextArea(nuevo ? "" : circuito.getDescripcion());
        descripcion.setPrefRowCount(3);
        TextField record = new TextField(nuevo || circuito.getRecordVuelta() == null
                ? "" : circuito.getRecordVuelta().getTiempo());
        TextField recordPiloto = new TextField(nuevo || circuito.getRecordVuelta() == null
                ? "" : circuito.getRecordVuelta().getPiloto());
        TextField consumo = new TextField(String.valueOf(nuevo ? 1.0 : circuito.getFactorConsumo()));
        TextField desgaste = new TextField(String.valueOf(nuevo ? 1.0 : circuito.getFactorDesgaste()));

        GridPane rejilla = rejilla();
        rejilla.addRow(0, new Label("Nombre"), nombre);
        rejilla.addRow(1, new Label("País"), pais);
        rejilla.addRow(2, new Label("Longitud (km)"), longitud);
        rejilla.addRow(3, new Label("Vueltas"), vueltas);
        rejilla.addRow(4, new Label("Descripción"), descripcion);
        rejilla.addRow(5, new Label("Récord (m:ss.mmm)"), record);
        rejilla.addRow(6, new Label("Autor del récord"), recordPiloto);
        rejilla.addRow(7, new Label("Factor consumo"), consumo);
        rejilla.addRow(8, new Label("Factor desgaste"), desgaste);

        Label ayuda = new Label("El factor técnico se calcula solo a partir del récord.");
        ayuda.getStyleClass().add("hint");

        Dialog<Circuit> dialogo = base(nuevo ? "Nuevo circuito" : "Editar circuito");
        dialogo.getDialogPane().setContent(new VBox(6, rejilla, ayuda));
        dialogo.setResultConverter(boton -> {
            if (boton != ButtonType.OK) {
                return null;
            }
            circuito.setNombre(nombre.getText());
            circuito.setPais(pais.getText());
            circuito.setLongitudKm(parseDouble(longitud.getText(), 0));
            circuito.setVueltas(vueltas.getValue());
            circuito.setDescripcion(descripcion.getText());
            circuito.setFactorConsumo(parseDouble(consumo.getText(), 1.0));
            circuito.setFactorDesgaste(parseDouble(desgaste.getText(), 1.0));
            if (!record.getText().isBlank()) {
                circuito.setRecordVuelta(new Circuit.LapRecord(
                        FormatUtils.parseLapTime(record.getText()), recordPiloto.getText(), 0));
            }
            if (circuito.getProbabilidadClima().isEmpty()) {
                circuito.getProbabilidadClima().put(WeatherCondition.SECO, 0.7);
                circuito.getProbabilidadClima().put(WeatherCondition.LLUVIOSO, 0.25);
                circuito.getProbabilidadClima().put(WeatherCondition.EXTREMO, 0.05);
            }
            return circuito;
        });
        return dialogo.showAndWait();
    }

    private static double parseDouble(String texto, double porDefecto) {
        try {
            return Double.parseDouble(texto.trim().replace(',', '.'));
        } catch (RuntimeException e) {
            return porDefecto;
        }
    }

    /** Controles de rendimiento de un modo de conducción. */
    private static class ModoControles {

        private final DrivingMode modo;
        private final Spinner<Integer> velocidad;
        private final TextField[] consumo = new TextField[3];
        private final TextField[] desgaste = new TextField[3];

        ModoControles(DrivingMode modo, Vehicle.Performance actual) {
            this.modo = modo;
            this.velocidad = spinner(50, 400, actual != null ? actual.getVelocidadPromedioKmh() : 300);
            WeatherCondition[] climas = WeatherCondition.values();
            for (int i = 0; i < climas.length; i++) {
                consumo[i] = new TextField(actual != null ? String.valueOf(actual.consumoCon(climas[i])) : "2.0");
                desgaste[i] = new TextField(actual != null ? String.valueOf(actual.desgasteCon(climas[i])) : "1.5");
                consumo[i].setPrefWidth(70);
                desgaste[i].setPrefWidth(70);
            }
        }

        GridPane panel() {
            GridPane rejilla = rejilla();
            rejilla.addRow(0, new Label("Velocidad media (km/h)"), velocidad);
            WeatherCondition[] climas = WeatherCondition.values();
            rejilla.addRow(1, new Label(""), new Label("Consumo"), new Label("Desgaste"));
            for (int i = 0; i < climas.length; i++) {
                rejilla.addRow(2 + i, new Label(climas[i].getEtiqueta()), consumo[i], desgaste[i]);
            }
            return rejilla;
        }

        Vehicle.Performance aPerformance() {
            Vehicle.Performance rendimiento = new Vehicle.Performance(velocidad.getValue());
            WeatherCondition[] climas = WeatherCondition.values();
            for (int i = 0; i < climas.length; i++) {
                rendimiento.getConsumo().put(climas[i], parseDouble(consumo[i].getText(), 2.0));
                rendimiento.getDesgaste().put(climas[i], parseDouble(desgaste[i].getText(), 1.5));
            }
            return rendimiento;
        }
    }
}
