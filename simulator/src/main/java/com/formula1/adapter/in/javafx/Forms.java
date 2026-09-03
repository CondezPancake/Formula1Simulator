package com.formula1.adapter.in.javafx;

import com.formula1.domain.model.Circuit;
import com.formula1.domain.model.Driver;
import com.formula1.domain.model.DriverRole;
import com.formula1.domain.model.DrivingMode;
import com.formula1.domain.model.Team;
import com.formula1.domain.model.Vehicle;
import com.formula1.domain.model.WeatherCondition;
import com.formula1.application.usecase.DriverService;
import com.formula1.domain.service.ValidationException;
import com.formula1.util.InputValidation;

import javafx.event.ActionEvent;
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
import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

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
        dialogo.getDialogPane().getStyleClass().add("form-dialog");
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
        InputValidation.entero(control, min, max);
        control.setPrefWidth(110);
        return control;
    }

    private static void validarAlAceptar(Dialog<?> dialogo, Runnable validacion) {
        Node aceptar = dialogo.getDialogPane().lookupButton(ButtonType.OK);
        aceptar.addEventFilter(ActionEvent.ACTION, evento -> {
            try {
                validacion.run();
            } catch (ValidationException error) {
                evento.consume();
                Navigator.error("Revisa los datos", error.getMessage());
            } catch (RuntimeException error) {
                evento.consume();
                Navigator.error("No se pudo validar el formulario",
                        error.getMessage() == null ? "Comprueba los valores ingresados." : error.getMessage());
            }
        });
    }

    // ------------------------------------------------------------ pilotos

    public static Optional<Driver> piloto(Driver original, List<Team> equipos, int idSugerido) {
        boolean nuevo = original == null;
        Driver piloto = nuevo ? new Driver() : original;

        TextField nombre = new TextField(nuevo ? "" : piloto.getNombre());
        InputValidation.texto(nombre, 60);
        ComboBox<String> selectorEquipo = new ComboBox<>();
        equipos.forEach(e -> selectorEquipo.getItems().add(e.getNombre()));
        selectorEquipo.setValue(equipos.isEmpty() ? null : equipos.get(0).getNombre());
        ComboBox<DriverRole> selectorRol = new ComboBox<>();
        selectorRol.getItems().addAll(DriverRole.values());
        selectorRol.setValue(DriverRole.ESCUDERO);
        Node campoEquipo = nuevo ? selectorEquipo : soloLectura(piloto.getEquipo());
        Node campoRol = nuevo ? selectorRol : soloLectura(
                piloto.getRol() == null ? "Sin rol" : piloto.getRol().toString());
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
        rejilla.addRow(1, new Label("Equipo"), campoEquipo);
        rejilla.addRow(2, new Label("Rol"), campoRol);
        rejilla.addRow(3, new Label("Experiencia (años)"), experiencia);
        rejilla.addRow(4, etiquetaConAyuda("Velocidad (0–100)", ayudaVelocidad), velocidad);
        rejilla.addRow(5, etiquetaConAyuda("Consistencia (0–100)", ayudaConsistencia), consistencia);
        rejilla.addRow(6, etiquetaConAyuda("Rendimiento en lluvia (0–100)", ayudaLluvia), lluvia);

        Dialog<Driver> dialogo = base(nuevo ? "Nuevo piloto" : "Editar piloto");
        dialogo.getDialogPane().setContent(rejilla);
        validarAlAceptar(dialogo, () -> {
            InputValidation.requerido(nombre, "El nombre del piloto", 60);
            if (nuevo) {
                InputValidation.seleccionar(selectorEquipo.getValue() != null, selectorEquipo,
                        "Debes seleccionar un equipo.");
                InputValidation.seleccionar(selectorRol.getValue() != null, selectorRol,
                        "Debes seleccionar un rol.");
            }
            InputValidation.valorEntero(experiencia, "La experiencia", 0, 30);
            InputValidation.valorEntero(velocidad, "La velocidad", 0, 100);
            InputValidation.valorEntero(consistencia, "La consistencia", 0, 100);
            InputValidation.valorEntero(lluvia, "El rendimiento en lluvia", 0, 100);
        });
        dialogo.setResultConverter(boton -> {
            if (boton != ButtonType.OK) {
                return null;
            }
            piloto.setId(nuevo ? idSugerido : piloto.getId());
            piloto.setNombre(nombre.getText().trim());
            if (nuevo) {
                piloto.setEquipo(selectorEquipo.getValue());
                piloto.setRol(selectorRol.getValue());
            }
            piloto.setExperiencia(InputValidation.valorEntero(experiencia, "La experiencia", 0, 30));
            piloto.setHabilidad(Driver.HABILIDAD_VELOCIDAD,
                    InputValidation.valorEntero(velocidad, "La velocidad", 0, 100));
            piloto.setHabilidad(Driver.HABILIDAD_CONSISTENCIA,
                    InputValidation.valorEntero(consistencia, "La consistencia", 0, 100));
            piloto.setHabilidad(Driver.HABILIDAD_LLUVIA,
                    InputValidation.valorEntero(lluvia, "El rendimiento en lluvia", 0, 100));
            return piloto;
        });
        return dialogo.showAndWait();
    }

    private static Label etiquetaConAyuda(String texto, Tooltip ayuda) {
        Label etiqueta = new Label(texto + "  ?");
        etiqueta.setTooltip(new Tooltip(ayuda.getText()));
        return etiqueta;
    }

    private static Label soloLectura(String valor) {
        Label campo = new Label(valor == null || valor.isBlank() ? "Sin asignar" : valor);
        campo.getStyleClass().add("readonly-field");
        campo.setMaxWidth(Double.MAX_VALUE);
        campo.setTooltip(new Tooltip(
                "Solo lectura. Este dato se gestiona desde las funciones específicas de asignación."));
        return campo;
    }

    // ------------------------------------------------------------ equipos

    public static Optional<Team> equipo(Team original) {
        boolean nuevo = original == null;
        Team equipo = nuevo ? new Team() : original;

        TextField nombre = new TextField(nuevo ? "" : equipo.getNombre());
        InputValidation.identificador(nombre, 60);
        nombre.setDisable(!nuevo); // el nombre es la clave: no se renombra
        TextField pais = new TextField(nuevo ? "" : equipo.getPais());
        TextField motor = new TextField(nuevo ? "" : equipo.getMotor());
        InputValidation.texto(pais, 50);
        InputValidation.identificador(motor, 50);

        GridPane rejilla = rejilla();
        rejilla.addRow(0, new Label("Nombre"), nombre);
        rejilla.addRow(1, new Label("País"), pais);
        rejilla.addRow(2, new Label("Motor"), motor);

        Dialog<Team> dialogo = base(nuevo ? "Nuevo equipo" : "Editar equipo");
        dialogo.getDialogPane().setContent(rejilla);
        validarAlAceptar(dialogo, () -> {
            InputValidation.requerido(nombre, "El nombre del equipo", 60);
            InputValidation.requerido(pais, "El país", 50);
            InputValidation.requerido(motor, "El motor", 50);
        });
        dialogo.setResultConverter(boton -> {
            if (boton != ButtonType.OK) {
                return null;
            }
            equipo.setNombre(nombre.getText().trim());
            equipo.setPais(pais.getText().trim());
            equipo.setMotor(motor.getText().trim());
            return equipo;
        });
        return dialogo.showAndWait();
    }

    // ---------------------------------------------------------- vehículos

    /** Conserva la entrada anterior para los consumidores que usan el almacén global. */
    public static Optional<Vehicle> vehiculo(Vehicle original, List<Team> equipos) {
        return vehiculo(original, equipos, new DriverService().listar());
    }

    public static Optional<Vehicle> vehiculo(
            Vehicle original, List<Team> equipos, List<Driver> pilotosDisponibles) {
        boolean nuevo = original == null;
        Vehicle vehiculo = nuevo ? new Vehicle() : original;

        TextField modelo = new TextField(nuevo ? "" : vehiculo.getModelo());
        InputValidation.identificador(modelo, 40);
        modelo.setDisable(!nuevo);
        ComboBox<String> equipo = new ComboBox<>();
        equipos.forEach(e -> equipo.getItems().add(e.getNombre()));
        equipo.setValue(nuevo ? (equipos.isEmpty() ? null : equipos.get(0).getNombre()) : vehiculo.getEquipo());
        TextField motor = new TextField(nuevo ? "" : vehiculo.getMotor());
        InputValidation.identificador(motor, 50);
        Spinner<Integer> velocidad = spinner(100, 400, nuevo ? 340 : vehiculo.getVelocidadMaximaKmh());
        TextField aceleracion = new TextField(nuevo ? "2.7" : String.valueOf(vehiculo.getAceleracion0100()));
        InputValidation.decimal(aceleracion, 2, 2);

        GridPane datos = rejilla();
        datos.addRow(0, new Label("Modelo"), modelo);
        datos.addRow(1, new Label("Equipo"), equipo);
        datos.addRow(2, new Label("Motor"), motor);
        datos.addRow(3, new Label("Velocidad máx. (km/h)"), velocidad);
        datos.addRow(4, new Label("Aceleración 0-100 (s)"), aceleracion);

        List<CheckBox> candidatos = new ArrayList<>();
        VBox listaPilotos = new VBox(6);
        listaPilotos.getStyleClass().add("assignment-list");
        Consumer<String> mostrarCandidatos = nombreEquipo -> {
            candidatos.clear();
            listaPilotos.getChildren().clear();
            for (Driver piloto : pilotosElegibles(pilotosDisponibles, nombreEquipo)) {
                CheckBox casilla = new CheckBox(piloto.getNombre() + "  ·  "
                        + (piloto.getRol() == null ? "Sin rol" : piloto.getRol().getEtiqueta()));
                casilla.setUserData(piloto.getId());
                candidatos.add(casilla);
                listaPilotos.getChildren().add(casilla);
            }
            if (candidatos.isEmpty()) {
                Label vacio = new Label("No hay pilotos disponibles para el equipo seleccionado.");
                vacio.getStyleClass().add("hint");
                listaPilotos.getChildren().add(vacio);
            }
        };
        equipo.valueProperty().addListener((o, anterior, actual) -> mostrarCandidatos.accept(actual));
        mostrarCandidatos.accept(equipo.getValue());

        VBox asignacion = new VBox(7,
                new Label("Pilotos disponibles del equipo"), listaPilotos);
        asignacion.getStyleClass().add("assignment-panel");

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

        VBox contenido = new VBox(10, datos);
        if (nuevo) {
            contenido.getChildren().add(asignacion);
        }
        contenido.getChildren().add(pestanas);
        Dialog<Vehicle> dialogo = base(nuevo ? "Nuevo vehículo" : "Editar vehículo");
        dialogo.getDialogPane().setContent(contenido);
        dialogo.getDialogPane().setPrefWidth(520);
        validarAlAceptar(dialogo, () -> {
            InputValidation.requerido(modelo, "El modelo", 40);
            InputValidation.seleccionar(equipo.getValue() != null, equipo,
                    "Debes seleccionar un equipo.");
            InputValidation.requerido(motor, "El motor", 50);
            InputValidation.valorEntero(velocidad, "La velocidad máxima", 100, 400);
            InputValidation.valorDecimal(aceleracion, "La aceleración 0-100", 1.0, 10.0);
            controles.forEach(ModoControles::validar);
        });
        dialogo.setResultConverter(boton -> {
            if (boton != ButtonType.OK) {
                return null;
            }
            vehiculo.setModelo(modelo.getText().trim());
            vehiculo.setEquipo(equipo.getValue());
            vehiculo.setMotor(motor.getText().trim());
            vehiculo.setVelocidadMaximaKmh(
                    InputValidation.valorEntero(velocidad, "La velocidad máxima", 100, 400));
            vehiculo.setAceleracion0100(
                    InputValidation.valorDecimal(aceleracion, "La aceleración 0-100", 1.0, 10.0));
            if (nuevo) {
                vehiculo.setPilotos(candidatos.stream()
                        .filter(CheckBox::isSelected)
                        .map(c -> (Integer) c.getUserData())
                        .toList());
            }
            controles.forEach(fila -> vehiculo.getRendimiento().put(fila.modo, fila.aPerformance()));
            return vehiculo;
        });
        return dialogo.showAndWait();
    }

    static List<Driver> pilotosElegibles(List<Driver> pilotos, String equipo) {
        if (pilotos == null || equipo == null) {
            return List.of();
        }
        return pilotos.stream()
                .filter(p -> p.getEquipo() != null && p.getEquipo().equalsIgnoreCase(equipo))
                .toList();
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
        InputValidation.identificador(nombre, 80);
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
        InputValidation.texto(pais, 50);
        InputValidation.decimal(longitud, 2, 3);
        InputValidation.descripcion(descripcion, 500);
        InputValidation.tiempoVuelta(record);
        InputValidation.texto(recordPiloto, 60);
        InputValidation.decimal(consumo, 1, 3);
        InputValidation.decimal(desgaste, 1, 3);

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
        validarAlAceptar(dialogo, () -> {
            InputValidation.requerido(nombre, "El nombre del circuito", 80);
            InputValidation.requerido(pais, "El país", 50);
            InputValidation.valorDecimal(longitud, "La longitud", 0.1, 30.0);
            InputValidation.valorEntero(vueltas, "El número de vueltas", 1, 200);
            InputValidation.requerido(descripcion, "La descripción", 500);
            InputValidation.valorDecimal(consumo, "El factor de consumo", 0.1, 5.0);
            InputValidation.valorDecimal(desgaste, "El factor de desgaste", 0.1, 5.0);
            boolean hayTiempo = !record.getText().isBlank();
            boolean hayAutor = !recordPiloto.getText().isBlank();
            if (hayTiempo != hayAutor) {
                throw new ValidationException(
                        "El récord de vuelta requiere tanto el tiempo como el nombre del piloto.");
            }
            if (hayTiempo) {
                InputValidation.valorTiempoVuelta(record, "El récord de vuelta");
                InputValidation.requerido(recordPiloto, "El autor del récord", 60);
            }
        });
        dialogo.setResultConverter(boton -> {
            if (boton != ButtonType.OK) {
                return null;
            }
            circuito.setNombre(nombre.getText().trim());
            circuito.setPais(pais.getText().trim());
            circuito.setLongitudKm(InputValidation.valorDecimal(longitud, "La longitud", 0.1, 30.0));
            circuito.setVueltas(InputValidation.valorEntero(vueltas, "El número de vueltas", 1, 200));
            circuito.setDescripcion(descripcion.getText().trim());
            circuito.setFactorConsumo(
                    InputValidation.valorDecimal(consumo, "El factor de consumo", 0.1, 5.0));
            circuito.setFactorDesgaste(
                    InputValidation.valorDecimal(desgaste, "El factor de desgaste", 0.1, 5.0));
            if (!record.getText().isBlank()) {
                circuito.setRecordVuelta(new Circuit.LapRecord(
                        InputValidation.valorTiempoVuelta(record, "El récord de vuelta"),
                        recordPiloto.getText().trim(), 0));
            } else {
                circuito.setRecordVuelta(null);
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
                InputValidation.decimal(consumo[i], 2, 3);
                InputValidation.decimal(desgaste[i], 3, 3);
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
                rendimiento.getConsumo().put(climas[i], InputValidation.valorDecimal(
                        consumo[i], "El consumo en " + climas[i].getEtiqueta(), 0.01, 20.0));
                rendimiento.getDesgaste().put(climas[i], InputValidation.valorDecimal(
                        desgaste[i], "El desgaste en " + climas[i].getEtiqueta(), 0.01, 100.0));
            }
            return rendimiento;
        }

        void validar() {
            InputValidation.valorEntero(velocidad,
                    "La velocidad media en modo " + modo.getEtiqueta(), 50, 400);
            WeatherCondition[] climas = WeatherCondition.values();
            for (int i = 0; i < climas.length; i++) {
                InputValidation.valorDecimal(consumo[i],
                        "El consumo en " + climas[i].getEtiqueta(), 0.01, 20.0);
                InputValidation.valorDecimal(desgaste[i],
                        "El desgaste en " + climas[i].getEtiqueta(), 0.01, 100.0);
            }
        }
    }
}
