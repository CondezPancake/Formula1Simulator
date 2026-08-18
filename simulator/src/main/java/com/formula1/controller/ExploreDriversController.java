package com.formula1.controller;

import com.formula1.model.Driver;
import com.formula1.model.Team;
import com.formula1.service.DriverService;
import com.formula1.service.TeamService;
import com.formula1.service.ValidationException;
import com.formula1.util.TeamColors;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

import java.util.List;
import java.util.Locale;

/**
 * Catálogo de pilotos en tarjetas, de solo lectura.
 *
 * Reproduce la tarjeta del diseño: foto con el nombre superpuesto, dorsal
 * con el color del equipo, distintivo de rol y una fila de estadísticas.
 */
public class ExploreDriversController {

    private static final double ANCHO_FOTO = 242;
    /** Algo mas alta que en el mockup: recorta menos los retratos cuadrados. */
    private static final double ALTO_FOTO = 170;
    /**
     * 0 = recorte pegado arriba, 0,5 = centrado. Se queda cerca del borde
     * superior porque en un retrato la cabeza esta arriba: bajarlo mas la
     * recortaba en las fotos de plano corto.
     */
    private static final double SESGO_VERTICAL = 0.1;

    @FXML private TextField buscador;
    @FXML private FlowPane chips;
    @FXML private FlowPane tarjetas;
    @FXML private Label lblConteo;

    private final DriverService pilotos;
    private final TeamService equipos;
    private String equipoSeleccionado;

    public ExploreDriversController() {
        this(new DriverService(), new TeamService());
    }

    public ExploreDriversController(DriverService pilotos, TeamService equipos) {
        this.pilotos = pilotos;
        this.equipos = equipos;
    }

    @FXML
    public void initialize() {
        construirChips();
        buscador.textProperty().addListener((obs, antes, ahora) -> refrescar());
        refrescar();
    }

    private void construirChips() {
        chips.getChildren().add(chip("TODOS", null));
        for (Team equipo : equipos.listar()) {
            chips.getChildren().add(chip(abreviar(equipo.getNombre()), equipo.getNombre()));
        }
    }

    /** Los chips del diseño usan la primera palabra del equipo. */
    private String abreviar(String equipo) {
        return equipo.split("[ -]")[0].toUpperCase(Locale.ROOT);
    }

    private Button chip(String texto, String equipo) {
        Button boton = new Button(texto);
        boton.getStyleClass().add("filter-chip");
        if (equipo == null) {
            boton.getStyleClass().add("chip-selected");
        }
        boton.setOnAction(e -> {
            equipoSeleccionado = equipo;
            for (var nodo : chips.getChildren()) {
                nodo.getStyleClass().remove("chip-selected");
            }
            boton.getStyleClass().add("chip-selected");
            refrescar();
        });
        return boton;
    }

    private void refrescar() {
        List<Driver> resultado = pilotos.buscar(buscador.getText()).stream()
                .filter(p -> equipoSeleccionado == null || equipoSeleccionado.equalsIgnoreCase(p.getEquipo()))
                .toList();
        tarjetas.getChildren().setAll(resultado.stream().map(this::tarjeta).toList());
        lblConteo.setText(resultado.size() + " PILOTOS");
    }

    private VBox tarjeta(Driver piloto) {
        String color = TeamColors.hex(piloto.getEquipo());

        VBox card = new VBox(0);
        card.getStyleClass().add("explore-card");
        card.setStyle("-fx-border-color: " + color + ";");

        card.getChildren().addAll(cabecera(piloto, color), cuerpo(piloto, color));
        return card;
    }

    /** Foto con el dorsal arriba a la derecha y el rol abajo a la izquierda. */
    private StackPane cabecera(Driver piloto, String color) {
        StackPane foto = new StackPane();
        foto.getStyleClass().add("explore-card-photo");
        foto.setPrefSize(ANCHO_FOTO, ALTO_FOTO);
        foto.setMinSize(ANCHO_FOTO, ALTO_FOTO);
        foto.setMaxSize(ANCHO_FOTO, ALTO_FOTO);
        foto.setClip(esquinasSuperioresRedondeadas());

        imagenDe(piloto).ifPresentOrElse(
                foto.getChildren()::add,
                () -> {
                    Label iniciales = new Label(inicialesDe(piloto.getNombre()));
                    iniciales.setStyle("-fx-text-fill: " + color
                            + "; -fx-font-size: 34px; -fx-font-weight: bold;");
                    foto.getChildren().add(iniciales);
                });

        Label dorsal = new Label("#" + piloto.getNumero());
        dorsal.getStyleClass().add("number-badge");
        dorsal.setStyle("-fx-background-color: " + color + ";");
        StackPane.setAlignment(dorsal, Pos.TOP_RIGHT);
        StackPane.setMargin(dorsal, new Insets(10));

        String rol = piloto.getRol() == null ? "ESCUDERO"
                : piloto.getRol().getEtiqueta().toUpperCase(Locale.ROOT);
        Label distintivo = new Label(rol);
        distintivo.getStyleClass().addAll("role-badge",
                piloto.getRol() == null ? "escudero" : piloto.getRol().name().toLowerCase(Locale.ROOT));
        StackPane.setAlignment(distintivo, Pos.BOTTOM_LEFT);
        StackPane.setMargin(distintivo, new Insets(10));

        foto.getChildren().addAll(dorsal, distintivo);
        return foto;
    }

    /**
     * Redondea solo arriba, para que la foto siga la curva de la tarjeta sin
     * separarse del cuerpo por abajo. Un {@code Rectangle} redondea las cuatro
     * esquinas, de ahí la unión con otro recto que cubre la mitad inferior.
     */
    private Shape esquinasSuperioresRedondeadas() {
        Rectangle redondeado = new Rectangle(ANCHO_FOTO, ALTO_FOTO);
        redondeado.setArcWidth(14);
        redondeado.setArcHeight(14);
        Rectangle inferior = new Rectangle(0, ALTO_FOTO / 2, ANCHO_FOTO, ALTO_FOTO / 2);
        return Shape.union(redondeado, inferior);
    }

    private VBox cuerpo(Driver piloto, String color) {
        VBox cuerpo = new VBox(6);
        cuerpo.getStyleClass().add("explore-card-body");

        Label nombre = new Label(piloto.getNombre());
        nombre.getStyleClass().add("explore-card-name");

        Label equipo = new Label(piloto.getEquipo() == null ? ""
                : piloto.getEquipo().toUpperCase(Locale.ROOT));
        equipo.getStyleClass().add("explore-card-team");
        equipo.setStyle("-fx-text-fill: " + color + ";");

        HBox stats = new HBox(20);
        stats.setPadding(new Insets(6, 0, 5, 0));
        stats.getChildren().addAll(
                estadistica(String.valueOf(piloto.getVictorias()), "VICTORIAS", "#FFC906"),
                estadistica(String.valueOf(piloto.getCampeonatos()), "CAMPS.", "#E10600"),
                estadistica(piloto.getExperiencia() + " años", "EXPERIENCIA", "#39B54A"));

        Label tituloHabilidades = new Label("HABILIDADES · ESCALA 0–100");
        tituloHabilidades.getStyleClass().add("card-label");
        HBox habilidades = new HBox(12,
                habilidad(piloto, Driver.HABILIDAD_VELOCIDAD, "VELOCIDAD", "#59A5FF",
                        "Capacidad para marcar un ritmo rápido y reducir el tiempo de vuelta."),
                habilidad(piloto, Driver.HABILIDAD_CONSISTENCIA, "CONSIST.", "#FFC906",
                        "Capacidad para mantener un rendimiento estable durante toda la vuelta."),
                habilidad(piloto, Driver.HABILIDAD_LLUVIA, "LLUVIA", "#64D8CB",
                        "Capacidad para conservar ritmo y control con la pista mojada."));

        HBox pie = new HBox();
        pie.setAlignment(Pos.CENTER_LEFT);
        Label nacionalidad = new Label(piloto.getNacionalidad() == null ? "" : piloto.getNacionalidad());
        nacionalidad.getStyleClass().add("explore-card-foot");
        Region relleno = new Region();
        HBox.setHgrow(relleno, Priority.ALWAYS);
        Button detalle = new Button("VER DETALLE ▸");
        detalle.getStyleClass().add("card-link");
        detalle.setStyle("-fx-text-fill: " + color + ";");
        detalle.setOnAction(e -> Forms.piloto(piloto, equipos.listar(), piloto.getId())
                .ifPresent(this::guardar));
        pie.getChildren().addAll(nacionalidad, relleno, detalle);
        pie.getStyleClass().add("explore-card-actions");

        cuerpo.getChildren().addAll(nombre, equipo, stats, tituloHabilidades, habilidades, pie);
        return cuerpo;
    }

    private VBox habilidad(Driver piloto, String clave, String etiqueta,
                           String color, String explicacion) {
        VBox celda = estadistica(piloto.getHabilidad(clave) + "/100", etiqueta, color);
        Tooltip.install(celda, new Tooltip(explicacion + " Escala: 0 (baja) a 100 (élite)."));
        return celda;
    }

    void guardar(Driver piloto) {
        try {
            pilotos.guardar(piloto);
            refrescar();
        } catch (ValidationException e) {
            Navigator.error("Datos no válidos", e.getMessage());
        }
    }

    private VBox estadistica(String valor, String etiqueta, String color) {
        VBox celda = new VBox(1);
        Label valorLbl = new Label(valor);
        valorLbl.getStyleClass().add("tile-value");
        valorLbl.setStyle("-fx-text-fill: " + color + ";");
        Label etiquetaLbl = new Label(etiqueta);
        etiquetaLbl.getStyleClass().add("tile-label");
        celda.getChildren().addAll(valorLbl, etiquetaLbl);
        return celda;
    }

    /**
     * Las fotos vienen con proporciones muy dispares, así que se recortan
     * centradas en vez de estirarse.
     */
    private java.util.Optional<ImageView> imagenDe(Driver piloto) {
        if (piloto.getImagen() == null || piloto.getImagen().isBlank()) {
            return java.util.Optional.empty();
        }
        var recurso = getClass().getResourceAsStream(piloto.getImagen());
        if (recurso == null) {
            return java.util.Optional.empty();
        }
        Image imagen = new Image(recurso);
        ImageView vista = new ImageView(imagen);

        // Se recorta sobre la imagen de origen, no sobre el nodo ya escalado:
        // asi la vista mide exactamente la caja y no puede desbordarla. Las
        // fotos van de 224x224 a 3444x4429, y recortarlas a una proporcion
        // comun es lo que hace que todas las tarjetas se vean iguales.
        double ancho = imagen.getWidth();
        double alto = imagen.getHeight();
        double proporcion = ANCHO_FOTO / ALTO_FOTO;

        double recorteAncho;
        double recorteAlto;
        if (ancho / alto > proporcion) {
            recorteAlto = alto;                       // sobra a los lados
            recorteAncho = alto * proporcion;
        } else {
            recorteAncho = ancho;                     // sobra arriba y abajo
            recorteAlto = ancho / proporcion;
        }

        vista.setViewport(new Rectangle2D(
                (ancho - recorteAncho) / 2,                   // centrado
                (alto - recorteAlto) * SESGO_VERTICAL,        // la cara va arriba
                recorteAncho, recorteAlto));
        vista.setFitWidth(ANCHO_FOTO);
        vista.setFitHeight(ALTO_FOTO);
        // El recorte ya tiene la proporcion exacta, asi que ajustar a la caja
        // no deforma nada.
        vista.setPreserveRatio(false);
        vista.setSmooth(true);
        return java.util.Optional.of(vista);
    }

    private String inicialesDe(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "?";
        }
        String[] partes = nombre.trim().split("\\s+");
        String primera = partes[0].substring(0, 1);
        String ultima = partes.length > 1 ? partes[partes.length - 1].substring(0, 1) : "";
        return (primera + ultima).toUpperCase(Locale.ROOT);
    }
}
