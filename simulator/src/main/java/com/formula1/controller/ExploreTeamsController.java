package com.formula1.controller;

import com.formula1.model.Driver;
import com.formula1.model.Team;
import com.formula1.model.Vehicle;
import com.formula1.service.TeamService;
import com.formula1.service.VehicleService;
import com.formula1.util.F1Assets;
import com.formula1.util.ImageCrop;
import com.formula1.util.Imagenes;
import com.formula1.util.InputValidation;
import com.formula1.util.TeamColors;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.CacheHint;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import java.util.List;
import java.util.Locale;

/**
 * Catálogo de escuderías, al estilo de formula1.com/en/teams.
 *
 * Sigue el mismo patrón que {@link ExploreDriversController}: rejilla de
 * tarjetas de tamaño fijo, cada una con el color del equipo, y clic para
 * abrir la ficha. La tarjeta reparte el espacio como la referencia: logo y
 * nombre arriba, monoplaza cruzando la parte baja y los dos pilotos a la
 * derecha.
 */
public class ExploreTeamsController {

    private static final double ANCHO = 340;
    private static final double ALTO = 250;
    private static final double ANCHO_COCHE = 300;
    private static final double ALTO_COCHE = 96;
    private static final double LADO_LOGO = 40;
    private static final double ANCHO_PILOTO = 66;

    @FXML private FlowPane tarjetas;
    @FXML private TextField buscador;
    @FXML private Label lblConteo;

    private final TeamService equipos;
    private final VehicleService vehiculos;

    public ExploreTeamsController() {
        this(new TeamService(), new VehicleService());
    }

    /** Constructor inyectable: es el que usan las pruebas. */
    public ExploreTeamsController(TeamService equipos, VehicleService vehiculos) {
        this.equipos = equipos;
        this.vehiculos = vehiculos;
    }

    @FXML
    public void initialize() {
        InputValidation.busqueda(buscador);
        buscador.textProperty().addListener((obs, antes, ahora) -> refrescar());
        refrescar();
    }

    private void refrescar() {
        List<Team> resultado = equipos.buscar(buscador.getText());
        tarjetas.getChildren().setAll(resultado.stream().map(this::tarjeta).toList());
        lblConteo.setText(resultado.size() + " EQUIPOS");
    }

    // --- tarjeta ----------------------------------------------------------

    private StackPane tarjeta(Team equipo) {
        String vivo = TeamColors.hex(equipo.getNombre());
        String fondo = TeamColors.accesible(equipo.getNombre());

        // El lienzo interior se recorta con esquinas redondeadas; el halo del
        // hover vive en el nodo de fuera para que el recorte no se lo coma.
        StackPane lienzo = new StackPane();
        fijar(lienzo, ANCHO, ALTO);
        Rectangle recorte = new Rectangle(ANCHO, ALTO);
        recorte.setArcWidth(24);
        recorte.setArcHeight(24);
        lienzo.setClip(recorte);

        lienzo.getChildren().add(capa(fondo));
        franjaColor(vivo).ifPresent(lienzo.getChildren()::add);
        coche(equipo).ifPresent(lienzo.getChildren()::add);
        lienzo.getChildren().add(degradado(fondo));

        VBox identidad = identidad(equipo);
        StackPane.setAlignment(identidad, Pos.TOP_LEFT);
        lienzo.getChildren().add(identidad);

        HBox pilotos = pilotos(equipo);
        StackPane.setAlignment(pilotos, Pos.BOTTOM_RIGHT);
        lienzo.getChildren().add(pilotos);

        StackPane tarjeta = new StackPane(lienzo);
        tarjeta.getStyleClass().add("team-card");
        fijar(tarjeta, ANCHO, ALTO);
        tarjeta.setCursor(Cursor.HAND);
        activar(tarjeta, vivo, equipo.getNombre());
        return tarjeta;
    }

    private static void fijar(Region region, double ancho, double alto) {
        region.setMinSize(ancho, alto);
        region.setPrefSize(ancho, alto);
        region.setMaxSize(ancho, alto);
    }

    private Region capa(String color) {
        Region fondo = new Region();
        fondo.setStyle("-fx-background-color: " + color + ";");
        return fondo;
    }

    /** Banda diagonal en el color vivo del equipo, como acento de marca. */
    private java.util.Optional<Region> franjaColor(String colorEquipo) {
        Region franja = new Region();
        franja.setStyle("-fx-background-color: linear-gradient(from 0% 100% to 100% 0%, "
                + colorEquipo + "00 38%, " + colorEquipo + "66 72%, " + colorEquipo + "cc 100%);");
        return java.util.Optional.of(franja);
    }

    /** Vela la parte baja para que el monoplaza no pelee con el texto. */
    private Region degradado(String color) {
        Region velo = new Region();
        velo.setStyle("-fx-background-color: linear-gradient(to bottom, "
                + color + "e6 0%, " + color + "59 42%, transparent 78%);");
        return velo;
    }

    private java.util.Optional<ImageView> coche(Team equipo) {
        return vehiculos.delEquipo(equipo.getNombre())
                .map(Vehicle::getImagen)
                .flatMap(ruta -> ImageCrop.desdeClasspath(ruta, ANCHO_COCHE, ALTO_COCHE,
                        ImageCrop.CENTRADO))
                .map(vista -> {
                    vista.setCache(true);
                    vista.setCacheHint(CacheHint.SPEED);
                    StackPane.setAlignment(vista, Pos.BOTTOM_LEFT);
                    StackPane.setMargin(vista, new Insets(0, 0, 8, -10));
                    return vista;
                });
    }

    private VBox identidad(Team equipo) {
        HBox cabecera = new HBox(10);
        cabecera.setAlignment(Pos.CENTER_LEFT);
        logo(equipo).ifPresent(cabecera.getChildren()::add);

        Label nombre = new Label(equipo.getNombre().toUpperCase(Locale.ROOT));
        nombre.getStyleClass().add("team-card-name");
        nombre.setWrapText(true);
        nombre.setMaxWidth(ANCHO - LADO_LOGO - 60);
        cabecera.getChildren().add(nombre);

        Label motor = new Label(equipo.getMotor() + "  ·  " + equipo.getPais());
        motor.getStyleClass().add("team-card-meta");

        VBox identidad = new VBox(4, cabecera, motor);
        identidad.setPadding(new Insets(16, 16, 0, 18));
        identidad.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        return identidad;
    }

    private java.util.Optional<ImageView> logo(Team equipo) {
        Image imagen = Imagenes.cargar(F1Assets.logo(equipo.getNombre()), LADO_LOGO * 2, 0);
        if (imagen == null) {
            return java.util.Optional.empty();
        }
        ImageView vista = new ImageView(imagen);
        vista.setFitWidth(LADO_LOGO);
        vista.setPreserveRatio(true);
        vista.setSmooth(true);
        return java.util.Optional.of(vista);
    }

    /** Los dos pilotos del equipo, recortados de medio cuerpo. */
    private HBox pilotos(Team equipo) {
        HBox caja = new HBox(-8);
        caja.setAlignment(Pos.BOTTOM_RIGHT);
        caja.setPadding(new Insets(0, 10, 0, 0));
        caja.setMouseTransparent(true);
        for (Driver piloto : equipos.pilotosDe(equipo)) {
            Image render = Imagenes.cargar(F1Assets.render(piloto.getCodigo()), ANCHO_PILOTO * 2, 0);
            if (render == null) {
                continue;
            }
            ImageView vista = new ImageView(render);
            vista.setFitWidth(ANCHO_PILOTO);
            vista.setPreserveRatio(true);
            vista.setSmooth(true);
            caja.getChildren().add(vista);
        }
        return caja;
    }

    // --- interacción ------------------------------------------------------

    private void activar(StackPane tarjeta, String colorEquipo, String nombreEquipo) {
        String halo = "-fx-effect: dropshadow(gaussian, " + colorEquipo + ", 22, 0.3, 0, 6);";
        tarjeta.setFocusTraversable(true);
        tarjeta.setOnMouseClicked(e -> abrirFicha(nombreEquipo));
        tarjeta.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.SPACE) {
                abrirFicha(nombreEquipo);
            }
        });
        tarjeta.setOnMouseEntered(e -> tarjeta.setStyle(halo));
        tarjeta.setOnMouseExited(e -> tarjeta.setStyle(""));
        tarjeta.focusedProperty().addListener((obs, antes, enfocada) ->
                tarjeta.setStyle(enfocada ? halo : ""));
    }

    /** Ficha del equipo, con retorno a este catálogo. */
    static void abrirFicha(String nombreEquipo) {
        Navigator.irConRetorno("team-detail");
        if (Navigator.ultimoControlador() instanceof TeamDetailController ficha) {
            ficha.mostrar(nombreEquipo);
        }
    }
}
