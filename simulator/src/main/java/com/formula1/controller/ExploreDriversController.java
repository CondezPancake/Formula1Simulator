package com.formula1.controller;

import com.formula1.model.Driver;
import com.formula1.model.Team;
import com.formula1.service.DriverService;
import com.formula1.service.TeamService;
import com.formula1.service.ValidationException;
import com.formula1.util.F1Assets;
import com.formula1.util.TeamColors;
import com.formula1.util.InputValidation;

import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.effect.Blend;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.ColorInput;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.List;
import java.util.Locale;

/**
 * Catálogo de pilotos en tarjetas, de solo lectura.
 *
 * La tarjeta reproduce la de <a href="https://www.formula1.com/en/drivers">la
 * web oficial de la F1</a>, que se lee de un vistazo porque no compite consigo
 * misma: el fondo es el color del equipo oscurecido, el piloto ocupa la mitad
 * derecha y a la izquierda solo hay nombre, equipo, dorsal y bandera. Las
 * cifras del piloto —victorias, campeonatos, habilidades— viven en su ficha,
 * que es donde se consultan de verdad; aquí sobrecargaban la rejilla.
 */
public class ExploreDriversController {

    private static final double ANCHO = 340;
    private static final double ALTO = 250;
    /**
     * El render se ancla por el ancho, no por el alto.
     *
     * El CDN de la F1 sirve unos pilotos de cuerpo entero y otros de medio
     * cuerpo, todos a la misma altura, asi que escalarlos por altura deja a
     * unos diminutos y a otros gigantes. El script de descarga los recorta al
     * mismo ancho, de modo que los hombros miden igual en los veinte; aqui solo
     * hay que fijar ese ancho y anclarlos por la cabeza, dejando que el cuerpo
     * se salga por abajo como en la referencia.
     */
    private static final double ANCHO_FOTO = 152;
    private static final double ANCHO_PATRON = 420;
    private static final Duration HOVER = Duration.millis(160);

    /** Máscara alfa del patrón de velocidad; se tiñe por equipo en cada tarjeta. */
    private static final Image PATRON_DRS = cargar(F1Assets.texturaDrs(), ANCHO_PATRON, ALTO);

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
        InputValidation.busqueda(buscador);
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

    /** Ficha de solo lectura del piloto, con retorno a este catalogo. */
    static void abrirFicha(int pilotoId) {
        Navigator.irConRetorno("driver-detail");
        if (Navigator.ultimoControlador() instanceof DriverDetailController ficha) {
            ficha.mostrar(pilotoId);
        }
    }

    /**
     * La tarjeta va en dos contenedores: el interior recorta las capas a las
     * esquinas redondeadas y el exterior queda libre para el halo del hover,
     * que si no lo cortaría el mismo recorte.
     */
    private StackPane tarjeta(Driver piloto) {
        String vivo = TeamColors.hex(piloto.getEquipo());
        String fondo = TeamColors.accesible(piloto.getEquipo());

        StackPane lienzo = new StackPane();
        lienzo.setMinSize(ANCHO, ALTO);
        lienzo.setPrefSize(ANCHO, ALTO);
        lienzo.setMaxSize(ANCHO, ALTO);
        lienzo.setClip(new Rectangle(ANCHO, ALTO) {{
            setArcWidth(24);
            setArcHeight(24);
        }});
        lienzo.getChildren().add(capa(fondo));
        patron(vivo).ifPresent(lienzo.getChildren()::add);
        lienzo.getChildren().add(degradado(fondo));

        ImageView foto = foto(piloto);
        if (foto != null) {
            lienzo.getChildren().add(foto);
        } else {
            lienzo.getChildren().add(marcaDeAgua(piloto));
        }

        VBox identidad = identidad(piloto);
        StackPane.setAlignment(identidad, Pos.TOP_LEFT);
        HBox pie = pie(piloto);
        StackPane.setAlignment(pie, Pos.BOTTOM_LEFT);
        lienzo.getChildren().addAll(identidad, pie);

        StackPane tarjeta = new StackPane(lienzo);
        tarjeta.getStyleClass().add("driver-card");
        tarjeta.setMinSize(ANCHO, ALTO);
        tarjeta.setPrefSize(ANCHO, ALTO);
        tarjeta.setMaxSize(ANCHO, ALTO);
        tarjeta.setCursor(Cursor.HAND);
        activar(tarjeta, foto, identidad, vivo, piloto.getId());
        return tarjeta;
    }

    private Region capa(String color) {
        Region fondo = new Region();
        fondo.setStyle("-fx-background-color: " + color + ";");
        return fondo;
    }

    /**
     * El halftone de velocidad de F1.com. La máscara es blanca con alfa, así que
     * en vez de recorrer píxeles se pinta el color encima respetando la
     * transparencia: {@code SRC_ATOP} solo cubre lo que ya era opaco.
     */
    private java.util.Optional<ImageView> patron(String colorEquipo) {
        if (PATRON_DRS == null) {
            return java.util.Optional.empty();
        }
        ImageView vista = new ImageView(PATRON_DRS);
        vista.setFitWidth(ANCHO_PATRON);
        vista.setFitHeight(ALTO);
        vista.setEffect(new Blend(BlendMode.SRC_ATOP, null,
                new ColorInput(0, 0, ANCHO_PATRON, ALTO, Color.web(colorEquipo))));
        vista.setOpacity(0.5);
        StackPane.setAlignment(vista, Pos.CENTER_RIGHT);
        return java.util.Optional.of(vista);
    }

    /** Vela el patrón bajo el texto: sin esto el nombre pelea con la textura. */
    private Region degradado(String color) {
        Region velo = new Region();
        velo.setStyle("-fx-background-color: linear-gradient(to right, "
                + color + " 0%, " + color + "e6 40%, transparent 88%);");
        return velo;
    }

    private ImageView foto(Driver piloto) {
        Image imagen = cargar(F1Assets.render(piloto.getCodigo()), ANCHO_FOTO * 2, 0);
        if (imagen == null) {
            return null;
        }
        ImageView vista = new ImageView(imagen);
        vista.setFitWidth(ANCHO_FOTO);
        vista.setPreserveRatio(true);
        vista.setSmooth(true);
        vista.getStyleClass().add("driver-card-photo");
        StackPane.setAlignment(vista, Pos.TOP_RIGHT);
        StackPane.setMargin(vista, new Insets(-4, -4, 0, 0));
        return vista;
    }

    /** Respaldo cuando falta el render: iniciales grandes, muy tenues. */
    private Label marcaDeAgua(Driver piloto) {
        Label iniciales = new Label(inicialesDe(piloto.getNombre()));
        iniciales.getStyleClass().add("driver-card-watermark");
        StackPane.setAlignment(iniciales, Pos.CENTER_RIGHT);
        StackPane.setMargin(iniciales, new Insets(0, 24, 0, 0));
        return iniciales;
    }

    private VBox identidad(Driver piloto) {
        String nombre = piloto.getNombre() == null ? "" : piloto.getNombre().trim();
        int corte = nombre.lastIndexOf(' ');

        Label pila = new Label(corte < 0 ? "" : nombre.substring(0, corte));
        pila.getStyleClass().add("driver-card-first");

        Label apellido = new Label(corte < 0 ? nombre : nombre.substring(corte + 1));
        apellido.getStyleClass().add("driver-card-last");

        Label equipo = new Label(piloto.getEquipo() == null ? ""
                : piloto.getEquipo().toUpperCase(Locale.ROOT));
        equipo.getStyleClass().add("driver-card-team");

        Label dorsal = new Label(String.valueOf(piloto.getNumero()));
        dorsal.getStyleClass().add("driver-card-number");

        VBox bloque = new VBox(pila, apellido, equipo, dorsal);
        bloque.setSpacing(0);
        bloque.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        bloque.setPadding(new Insets(16, 16, 0, 18));
        VBox.setMargin(dorsal, new Insets(2, 0, 0, 0));
        return bloque;
    }

    private HBox pie(Driver piloto) {
        HBox pie = new HBox(8);
        pie.setAlignment(Pos.CENTER_LEFT);
        pie.setPadding(new Insets(0, 18, 16, 18));
        pie.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        Image bandera = cargar(F1Assets.bandera(piloto.getNacionalidad()), 34, 0);
        if (bandera != null) {
            ImageView vista = new ImageView(bandera);
            vista.setFitWidth(34);
            vista.setPreserveRatio(true);
            vista.setSmooth(true);
            Rectangle recorte = new Rectangle(34, 34 * bandera.getHeight() / bandera.getWidth());
            recorte.setArcWidth(4);
            recorte.setArcHeight(4);
            vista.setClip(recorte);
            pie.getChildren().add(vista);
        }

        Label codigo = new Label(piloto.getCodigo() == null
                ? piloto.getNacionalidad() : piloto.getCodigo());
        codigo.getStyleClass().add("driver-card-code");
        pie.getChildren().add(codigo);
        return pie;
    }

    /**
     * Toda la tarjeta es el enlace, como en la referencia. Al no haber botón hay
     * que devolver el acceso por teclado a mano: sin esto la sección dejaría de
     * poder recorrerse con TAB.
     */
    private void activar(StackPane tarjeta, ImageView foto, VBox identidad,
                         String colorEquipo, int pilotoId) {
        String halo = "-fx-effect: dropshadow(gaussian, " + colorEquipo + ", 22, 0.3, 0, 6);";
        tarjeta.setFocusTraversable(true);
        tarjeta.setOnMouseClicked(e -> abrirFicha(pilotoId));
        tarjeta.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.SPACE) {
                abrirFicha(pilotoId);
            }
        });
        tarjeta.setOnMouseEntered(e -> {
            tarjeta.setStyle(halo);
            subrayar(identidad, true);
            escalar(foto, 1.04);
        });
        tarjeta.setOnMouseExited(e -> {
            tarjeta.setStyle("");
            subrayar(identidad, false);
            escalar(foto, 1);
        });
        tarjeta.focusedProperty().addListener((obs, antes, enfocada) ->
                tarjeta.setStyle(enfocada ? halo : ""));
    }

    private void subrayar(VBox identidad, boolean activo) {
        for (Node nodo : identidad.getChildren()) {
            if (nodo instanceof Label etiqueta
                    && (etiqueta.getStyleClass().contains("driver-card-first")
                        || etiqueta.getStyleClass().contains("driver-card-last"))) {
                etiqueta.setUnderline(activo);
            }
        }
    }

    private void escalar(ImageView foto, double destino) {
        if (foto == null) {
            return;
        }
        ScaleTransition zoom = new ScaleTransition(HOVER, foto);
        zoom.setToX(destino);
        zoom.setToY(destino);
        zoom.setInterpolator(Interpolator.EASE_BOTH);
        zoom.play();
    }

    void guardar(Driver piloto) {
        try {
            pilotos.guardar(piloto);
            refrescar();
        } catch (ValidationException e) {
            Navigator.error("Datos no válidos", e.getMessage());
        }
    }

    /**
     * Carga del classpath. Un cero en ancho o alto deja que JavaFX deduzca esa
     * dimensión por proporción.
     */
    private static Image cargar(String ruta, double ancho, double alto) {
        if (ruta == null) {
            return null;
        }
        var recurso = ExploreDriversController.class.getResource(ruta);
        if (recurso == null) {
            return null;
        }
        Image imagen = new Image(recurso.toExternalForm(), ancho, alto, true, true, false);
        return imagen.isError() || imagen.getWidth() <= 0 ? null : imagen;
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
