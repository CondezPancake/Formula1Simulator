package com.formula1.controller;

import com.formula1.model.Driver;
import com.formula1.model.Team;
import com.formula1.model.Vehicle;
import com.formula1.service.TeamService;
import com.formula1.service.VehicleService;
import com.formula1.util.F1Assets;
import com.formula1.util.ImageCrop;
import com.formula1.util.Imagenes;
import com.formula1.util.TeamColors;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Ficha de una escudería, al estilo de formula1.com/en/teams/&lt;equipo&gt;.
 *
 * Se abre desde el catálogo con {@link ExploreTeamsController#abrirFicha} y
 * vuelve a él con {@code Navigator.volver()}, que restaura el nodo exacto y
 * su pestaña, igual que hace la ficha de piloto.
 */
public class TeamDetailController {

    private static final double LADO_LOGO = 72;
    private static final double ANCHO_COCHE = 420;
    private static final double ALTO_COCHE = 236;
    private static final double ANCHO_RETRATO = 96;
    private static final double ALTO_RETRATO = 120;

    @FXML private StackPane marcoLogo;
    @FXML private Label lblNombre;
    @FXML private Label lblMeta;
    @FXML private StackPane marcoCoche;
    @FXML private Label lblModelo;
    @FXML private Label lblMotorCoche;
    @FXML private HBox fichaCoche;
    @FXML private FlowPane tarjetasPilotos;
    @FXML private Button btnVolver;

    private final TeamService equipos;
    private final VehicleService vehiculos;

    public TeamDetailController() {
        this(new TeamService(), new VehicleService());
    }

    /** Constructor inyectable: es el que usan las pruebas. */
    public TeamDetailController(TeamService equipos, VehicleService vehiculos) {
        this.equipos = equipos;
        this.vehiculos = vehiculos;
    }

    /** Vuelca el equipo pedido. Se llama justo después de navegar aquí. */
    public void mostrar(String nombreEquipo) {
        Optional<Team> encontrado = equipos.porNombre(nombreEquipo);
        if (encontrado.isEmpty()) {
            lblNombre.setText("Equipo no encontrado");
            return;
        }
        Team equipo = encontrado.get();
        String color = TeamColors.hex(equipo.getNombre());

        lblNombre.setText(equipo.getNombre().toUpperCase(Locale.ROOT));
        lblNombre.setStyle("-fx-text-fill: " + color + ";");
        lblMeta.setText(equipo.getPais() + "  ·  MOTOR " + equipo.getMotor().toUpperCase(Locale.ROOT));
        marcoLogo.setStyle("-fx-border-color: " + color + ";");

        pintarLogo(equipo);
        pintarCoche(equipo, color);
        pintarPilotos(equipo, color);
    }

    private void pintarLogo(Team equipo) {
        marcoLogo.getChildren().clear();
        Image logo = Imagenes.cargar(F1Assets.logo(equipo.getNombre()), LADO_LOGO * 2, 0);
        if (logo == null) {
            return;
        }
        ImageView vista = new ImageView(logo);
        vista.setFitWidth(LADO_LOGO);
        vista.setPreserveRatio(true);
        vista.setSmooth(true);
        marcoLogo.getChildren().add(vista);
    }

    private void pintarCoche(Team equipo, String color) {
        marcoCoche.getChildren().clear();
        fichaCoche.getChildren().clear();

        Optional<Vehicle> coche = vehiculos.delEquipo(equipo.getNombre());
        if (coche.isEmpty()) {
            lblModelo.setText("SIN MONOPLAZA ASIGNADO");
            lblMotorCoche.setText("Este equipo aún no tiene un vehículo en el catálogo.");
            return;
        }
        Vehicle vehiculo = coche.get();
        lblModelo.setText(vehiculo.getModelo().toUpperCase(Locale.ROOT));
        lblMotorCoche.setText("Unidad de potencia " + vehiculo.getMotor() + ".");
        marcoCoche.setStyle("-fx-border-color: " + color + ";");

        ImageCrop.desdeClasspath(vehiculo.getImagen(), ANCHO_COCHE, ALTO_COCHE, ImageCrop.CENTRADO)
                .ifPresent(marcoCoche.getChildren()::add);

        fichaCoche.getChildren().addAll(
                dato(String.valueOf(vehiculo.getVelocidadMaximaKmh()), "KM/H PUNTA"),
                dato(String.format(Locale.ROOT, "%.1f s", vehiculo.getAceleracion0100()), "0-100 KM/H"),
                dato(String.valueOf(equipos.pilotosDe(equipo).size()), "PILOTOS"));
    }

    private VBox dato(String valor, String etiqueta) {
        Label lblValor = new Label(valor);
        lblValor.getStyleClass().add("card-value");
        Label lblEtiqueta = new Label(etiqueta);
        lblEtiqueta.getStyleClass().add("card-label");
        VBox caja = new VBox(2, lblValor, lblEtiqueta);
        caja.getStyleClass().add("card");
        return caja;
    }

    private void pintarPilotos(Team equipo, String color) {
        List<Driver> pilotos = equipos.pilotosDe(equipo);
        tarjetasPilotos.getChildren().setAll(pilotos.stream()
                .map(piloto -> tarjetaPiloto(piloto, color))
                .toList());
    }

    /** Tarjeta compacta que enlaza con la ficha del piloto ya existente. */
    private HBox tarjetaPiloto(Driver piloto, String color) {
        StackPane marco = new StackPane();
        marco.getStyleClass().add("driver-detail-photo");
        marco.setMinSize(ANCHO_RETRATO, ALTO_RETRATO);
        marco.setPrefSize(ANCHO_RETRATO, ALTO_RETRATO);
        marco.setMaxSize(ANCHO_RETRATO, ALTO_RETRATO);
        marco.setStyle("-fx-border-color: " + color + ";");
        ImageCrop.desdeClasspath(piloto.getImagen(), ANCHO_RETRATO, ALTO_RETRATO,
                        ImageCrop.SESGO_RETRATO)
                .ifPresent(marco.getChildren()::add);

        Label dorsal = new Label(String.valueOf(piloto.getNumero()));
        dorsal.getStyleClass().add("number-badge");
        dorsal.setStyle("-fx-background-color: " + color + ";");

        Label nombre = new Label(piloto.getNombre());
        nombre.getStyleClass().add("detail-value");
        Label codigo = new Label(piloto.getCodigo() + "  ·  " + piloto.getNacionalidad());
        codigo.getStyleClass().add("hint");

        VBox texto = new VBox(4, dorsal, nombre, codigo);
        texto.setAlignment(Pos.CENTER_LEFT);

        HBox tarjeta = new HBox(12, marco, texto);
        tarjeta.setAlignment(Pos.CENTER_LEFT);
        tarjeta.setPadding(new Insets(10));
        tarjeta.getStyleClass().add("card");
        tarjeta.setCursor(Cursor.HAND);
        tarjeta.setFocusTraversable(true);
        tarjeta.setOnMouseClicked(e -> ExploreDriversController.abrirFicha(piloto.getId()));
        tarjeta.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.SPACE) {
                ExploreDriversController.abrirFicha(piloto.getId());
            }
        });
        return tarjeta;
    }

    @FXML
    private void onVolver() {
        if (!Navigator.volver()) {
            Navigator.ir("explorar");
        }
    }
}
