package com.formula1.controller;

import com.formula1.domain.model.Driver;
import com.formula1.domain.model.LapStatus;
import com.formula1.domain.model.QualifyingSession;
import com.formula1.domain.model.Vehicle;
import com.formula1.service.DriverService;
import com.formula1.service.QualifyingService;
import com.formula1.service.VehicleService;
import com.formula1.util.F1Assets;
import com.formula1.util.FormatUtils;
import com.formula1.util.ImageCrop;
import com.formula1.util.Imagenes;
import com.formula1.util.TeamColors;
import com.formula1.util.VehicleImages;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.transform.Shear;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Ficha completa de un piloto, con el reparto de
 * formula1.com/en/drivers/&lt;piloto&gt;: hero con su render y su equipo,
 * estadísticas, biografía y una galería con visor a pantalla completa.
 *
 * <p>Es la hermana de {@link TeamDetailController} —misma sección Explorar,
 * mismo acabado— y comparte con ella el patrón de galería/visor y de tarjeta
 * de cifra ({@code cifra}), así que un cambio en cómo se ve una encaja con el
 * de la otra sin que se note la costura.
 *
 * <p>Dos orígenes de datos que no se mezclan a propósito: el
 * <b>palmarés real</b> sale del catálogo (victorias, campeonatos, temporadas)
 * y <b>en este simulador</b> se calcula al vuelo sobre las sesiones ya
 * disputadas, igual que en la ficha de equipo.
 */
public class DriverDetailController {

    private static final double ANCHO_HERO_RETRATO = 340;
    private static final double ALTO_HERO = 300;
    private static final double LADO_BANDERA = 40;
    private static final double LADO_MINIATURA = 168;
    private static final double ALTO_MINIATURA = 104;

    /** Inclinación del nombre: Titillium Web no trae cursiva. */
    private static final double SESGO_CURSIVA = -0.16;

    private static final Duration HOVER = Duration.millis(160);
    private static final Duration APERTURA = Duration.millis(200);

    @FXML private ScrollPane scroll;
    @FXML private StackPane hero;
    @FXML private StackPane marcoRetrato;
    @FXML private StackPane marcoBandera;
    @FXML private Label lblNombre;
    @FXML private Label lblMeta;
    @FXML private Label lblDorsal;
    @FXML private Button btnVerEquipo;
    @FXML private Button btnVolver;

    @FXML private Label lblResumenSesiones;
    @FXML private HBox estadisticasSimulador;
    @FXML private HBox palmares;
    @FXML private VBox atributos;

    @FXML private HBox datosNacimiento;
    @FXML private Label lblBiografia;

    @FXML private TilePane galeria;

    @FXML private StackPane visor;
    @FXML private StackPane escenarioVisor;
    @FXML private Label lblVisorTitulo;
    @FXML private Label lblVisorEquipo;
    @FXML private Label lblVisorContador;

    @FXML private TableView<Fila> tablaSesiones;
    @FXML private TableColumn<Fila, String> colFecha;
    @FXML private TableColumn<Fila, String> colCircuito;
    @FXML private TableColumn<Fila, String> colPosicion;
    @FXML private TableColumn<Fila, String> colTiempo;
    @FXML private TableColumn<Fila, String> colGap;
    @FXML private TableColumn<Fila, String> colEstado;

    private final DriverService pilotos;
    private final VehicleService vehiculos;
    private final QualifyingService sesiones;

    /** Rutas de las imágenes de la galería, en el orden en que se ven. */
    private final List<String> imagenesGaleria = new ArrayList<>();
    private int indiceVisor;
    private String colorEquipo = "#6c6c80";

    public DriverDetailController() {
        this(new DriverService(), new VehicleService(), new QualifyingService());
    }

    public DriverDetailController(DriverService pilotos, VehicleService vehiculos,
                                  QualifyingService sesiones) {
        this.pilotos = pilotos;
        this.vehiculos = vehiculos;
        this.sesiones = sesiones;
    }

    @FXML
    public void initialize() {
        hero.setMinHeight(ALTO_HERO);
        hero.setPrefHeight(ALTO_HERO);
        lblNombre.getTransforms().add(new Shear(SESGO_CURSIVA, 0));

        visor.sceneProperty().addListener((obs, vieja, nueva) -> {
            if (nueva != null) {
                nueva.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
                    if (!visor.isVisible()) {
                        return;
                    }
                    switch (e.getCode()) {
                        case ESCAPE -> onCerrarVisor();
                        case LEFT -> onAnterior();
                        case RIGHT -> onSiguiente();
                        default -> { }
                    }
                });
            }
        });

        colFecha.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().fecha()));
        colCircuito.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().circuito()));
        colPosicion.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().posicion()));
        colTiempo.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().tiempo()));
        colGap.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().gap()));
        colEstado.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().estado()));
        colTiempo.getStyleClass().add("mono-col");
        colGap.getStyleClass().add("mono-col");
        tablaSesiones.setPlaceholder(new Label("Este piloto todavía no ha disputado ninguna sesión."));
    }

    /** Punto de entrada tras {@code Navigator.irConRetorno("driver-detail")}. */
    public void mostrar(int pilotoId) {
        Optional<Driver> encontrado = pilotos.porId(pilotoId);
        if (encontrado.isEmpty()) {
            lblNombre.setText("Piloto no encontrado");
            return;
        }
        Driver piloto = encontrado.get();
        colorEquipo = TeamColors.hex(piloto.getEquipo());

        pintarHero(piloto);
        List<Fila> historial = historialDe(piloto);
        pintarEstadisticasSimulador(historial);
        pintarPalmares(piloto);
        pintarAtributos(piloto);
        pintarBiografia(piloto);
        pintarGaleria(piloto);
        tablaSesiones.setItems(FXCollections.observableArrayList(historial));
    }

    // ------------------------------------------------------------------ hero

    private void pintarHero(Driver piloto) {
        String fondo = TeamColors.accesible(piloto.getEquipo());

        hero.getChildren().removeIf(nodo -> nodo.getStyleClass().contains("team-hero-layer"));
        hero.getChildren().add(0, capa(velo(fondo)));
        texturaVelocidad().ifPresent(textura -> hero.getChildren().add(0, textura));
        hero.getChildren().add(0, capa(base(fondo)));

        marcoRetrato.getChildren().clear();
        Image render = Imagenes.cargar(F1Assets.render(piloto.getCodigo()), ANCHO_HERO_RETRATO * 2, 0);
        if (render == null) {
            render = Imagenes.cargar(piloto.getImagen(), ANCHO_HERO_RETRATO * 2, 0);
        }
        if (render != null) {
            ImageView vista = new ImageView(render);
            vista.setFitHeight(ALTO_HERO);
            vista.setPreserveRatio(true);
            vista.setSmooth(true);
            vista.setMouseTransparent(true);
            StackPane.setAlignment(vista, Pos.BOTTOM_RIGHT);
            marcoRetrato.getChildren().add(vista);
        }

        marcoBandera.getChildren().clear();
        Image bandera = Imagenes.cargar(F1Assets.bandera(piloto.getNacionalidad()), LADO_BANDERA * 2, 0);
        if (bandera != null) {
            ImageView vista = new ImageView(bandera);
            vista.setFitHeight(LADO_BANDERA * 0.6);
            vista.setPreserveRatio(true);
            vista.setSmooth(true);
            marcoBandera.getChildren().add(vista);
        }

        lblNombre.setText(piloto.getNombre().toUpperCase(Locale.ROOT));
        lblMeta.setText(texto(piloto.getNacionalidad()) + "  ·  " + texto(piloto.getEquipo()).toUpperCase(Locale.ROOT));
        lblDorsal.setText("#" + piloto.getNumero());
        lblDorsal.setStyle("-fx-background-color: " + colorEquipo + ";");

        btnVerEquipo.setOnAction(e -> ExploreTeamsController.abrirFicha(piloto.getEquipo()));
    }

    private Region capa(String estilo) {
        Region region = new Region();
        region.getStyleClass().add("team-hero-layer");
        region.setStyle(estilo);
        region.setMouseTransparent(true);
        return region;
    }

    private String base(String fondo) {
        return "-fx-background-color: linear-gradient(from 0% 100% to 100% 0%, "
                + "#08080A 22%, " + fondo + " 100%);";
    }

    private String velo(String fondo) {
        return "-fx-background-color: linear-gradient(to right, "
                + fondo + "f2 0%, " + fondo + "99 42%, transparent 82%);";
    }

    /** El mismo patrón de velocidad de la ficha de equipo, teñido igual. */
    private Optional<Node> texturaVelocidad() {
        Image textura = Imagenes.cargar(F1Assets.texturaDrs(), 960, 0);
        if (textura == null) {
            return Optional.empty();
        }
        ImageView vista = new ImageView(textura);
        vista.setFitWidth(760);
        vista.setPreserveRatio(true);
        vista.setEffect(new javafx.scene.effect.Blend(
                javafx.scene.effect.BlendMode.SRC_ATOP, null,
                new javafx.scene.effect.ColorInput(0, 0, 760, ALTO_HERO,
                        javafx.scene.paint.Color.web(colorEquipo))));
        vista.setOpacity(0.18);
        vista.setCache(true);
        vista.setCacheHint(javafx.scene.CacheHint.SPEED);

        StackPane caja = new StackPane(vista);
        caja.getStyleClass().add("team-hero-layer");
        caja.setMouseTransparent(true);
        StackPane.setAlignment(vista, Pos.CENTER_LEFT);
        return Optional.of(caja);
    }

    // ---------------------------------------------------------- estadísticas

    /**
     * Recorre las sesiones guardadas sumando lo que ha hecho este piloto: no
     * hay sistema de puntos —esto es una clasificación—, así que se cuenta lo
     * que el motor sí produce: poles, podios, mejor vuelta y abandonos.
     */
    private void pintarEstadisticasSimulador(List<Fila> historial) {
        long poles = historial.stream().filter(f -> f.orden() == 1).count();
        long podios = historial.stream().filter(f -> f.orden() <= 3).count();
        long abandonos = historial.stream().filter(f -> f.estado().equals(LapStatus.OUT.getEtiqueta())).count();
        var validas = historial.stream().filter(f -> f.segundos() > 0).toList();
        String mejorVuelta = validas.isEmpty() ? "—"
                : FormatUtils.formatLapTime(validas.stream()
                        .min(Comparator.comparingDouble(Fila::segundos))
                        .orElseThrow().segundos());

        lblResumenSesiones.setText(historial.isEmpty()
                ? "Todavía no ha salido a pista en ninguna clasificación guardada."
                : "Resultados en las clasificaciones ya disputadas.");

        estadisticasSimulador.getChildren().setAll(
                cifra(String.valueOf(historial.size()), "SESIONES", false),
                cifra(historial.isEmpty() ? "—" : "P" + historial.stream()
                        .mapToInt(Fila::orden).min().orElse(0), "MEJOR POSICIÓN", true),
                cifra(historial.isEmpty() ? "—" : String.format(Locale.ROOT, "%.1f",
                        historial.stream().mapToInt(Fila::orden).average().orElse(0)), "POSICIÓN MEDIA", false),
                cifra(mejorVuelta, "MEJOR VUELTA", false),
                cifra(String.valueOf(poles), "POLES", false),
                cifra(String.valueOf(podios), "PODIOS", false),
                cifra(String.valueOf(abandonos), "ABANDONOS", false));
    }

    /** Palmarés real: lo que el catálogo dice de la carrera del piloto. */
    private void pintarPalmares(Driver piloto) {
        palmares.getChildren().setAll(
                cifra(numero(piloto.getVictorias()), "VICTORIAS", true),
                cifra(numero(piloto.getCampeonatos()), "CAMPEONATOS", false),
                cifra(numero(piloto.getExperiencia()), "TEMPORADAS", false),
                cifra("#" + piloto.getNumero(), "DORSAL", false),
                cifra(vehiculos.listar().stream()
                        .filter(v -> v.conduce(piloto.getId()))
                        .map(Vehicle::getModelo)
                        .findFirst()
                        .orElse("—"), "MONOPLAZA", false));
    }

    /** Baldosa de cifra; la destacada se tiñe con el color del equipo. */
    private VBox cifra(String valor, String etiqueta, boolean destacada) {
        Label lblValor = new Label(valor);
        lblValor.getStyleClass().add("team-stat-value");
        lblValor.getTransforms().add(new Shear(SESGO_CURSIVA, 0));
        if (destacada) {
            lblValor.setStyle("-fx-text-fill: " + colorEquipo + ";");
        }
        Label lblEtiqueta = new Label(etiqueta);
        lblEtiqueta.getStyleClass().add("team-stat-label");

        VBox caja = new VBox(2, lblValor, lblEtiqueta);
        caja.getStyleClass().add("team-stat");
        HBox.setHgrow(caja, javafx.scene.layout.Priority.ALWAYS);
        return caja;
    }

    /** Habilidades del piloto como barras de atributos, al estilo videojuego. */
    private void pintarAtributos(Driver piloto) {
        atributos.getChildren().setAll(
                atributo("VELOCIDAD", piloto.getHabilidad(Driver.HABILIDAD_VELOCIDAD)),
                atributo("CONSISTENCIA", piloto.getHabilidad(Driver.HABILIDAD_CONSISTENCIA)),
                atributo("CONDUCCIÓN EN LLUVIA", piloto.getHabilidad(Driver.HABILIDAD_LLUVIA)));
    }

    private VBox atributo(String etiqueta, int valor) {
        Label lbl = new Label(etiqueta);
        lbl.getStyleClass().add("team-profile-label");
        Label lblValor = new Label(valor + " / 100");
        lblValor.getStyleClass().add("team-profile-value");
        Region espaciador = new Region();
        HBox.setHgrow(espaciador, javafx.scene.layout.Priority.ALWAYS);
        HBox cabecera = new HBox(8, lbl, espaciador, lblValor);
        cabecera.setAlignment(Pos.CENTER_LEFT);

        ProgressBar barra = new ProgressBar(valor / 100.0);
        barra.getStyleClass().add("team-skill-bar");
        barra.setMaxWidth(Double.MAX_VALUE);
        barra.setStyle("-fx-accent: " + colorEquipo + ";");
        return new VBox(4, cabecera, barra);
    }

    // ----------------------------------------------------------- biografía

    private void pintarBiografia(Driver piloto) {
        datosNacimiento.getChildren().setAll(
                dato("FECHA DE NACIMIENTO", texto(piloto.getFechaNacimiento())),
                dato("LUGAR DE NACIMIENTO", texto(piloto.getLugarNacimiento())),
                dato("NACIONALIDAD", texto(piloto.getNacionalidad())));
        lblBiografia.setText(piloto.getBiografia() == null || piloto.getBiografia().isBlank()
                ? "Sin biografía disponible para este piloto."
                : piloto.getBiografia());
    }

    private VBox dato(String etiqueta, String valor) {
        Label lblValor = new Label(valor);
        lblValor.getStyleClass().add("team-summary-value");
        Label lblEtiqueta = new Label(etiqueta);
        lblEtiqueta.getStyleClass().add("team-stat-label");
        return new VBox(1, lblValor, lblEtiqueta);
    }

    // --------------------------------------------------------------- galería

    /**
     * El retrato de catálogo, el render oficial, el logo del equipo y las
     * vistas de su monoplaza: hasta seis imágenes reales, sin rellenar con
     * marcadores vacíos si alguna falta.
     */
    private void pintarGaleria(Driver piloto) {
        imagenesGaleria.clear();
        if (piloto.getImagen() != null) {
            imagenesGaleria.add(piloto.getImagen());
        }
        String render = F1Assets.render(piloto.getCodigo());
        if (render != null) {
            imagenesGaleria.add(render);
        }
        String logo = F1Assets.logo(piloto.getEquipo());
        if (logo != null) {
            imagenesGaleria.add(logo);
        }
        vehiculos.listar().stream()
                .filter(v -> v.conduce(piloto.getId()))
                .map(Vehicle::getModelo)
                .findFirst()
                .map(VehicleImages::de)
                .ifPresent(imagenesGaleria::addAll);
        while (imagenesGaleria.size() > 6) {
            imagenesGaleria.remove(imagenesGaleria.size() - 1);
        }

        lblVisorEquipo.setText(texto(piloto.getEquipo()).toUpperCase(Locale.ROOT));
        lblVisorEquipo.setStyle("-fx-text-fill: " + colorEquipo + ";");

        List<Node> miniaturas = new ArrayList<>();
        for (int i = 0; i < imagenesGaleria.size(); i++) {
            miniatura(imagenesGaleria.get(i), i).ifPresent(miniaturas::add);
        }
        galeria.getChildren().setAll(miniaturas);
    }

    private Optional<StackPane> miniatura(String ruta, int indice) {
        Image imagen = Imagenes.cargar(ruta, LADO_MINIATURA * 2, 0);
        if (imagen == null) {
            return Optional.empty();
        }
        ImageView vista = ImageCrop.encajar(imagen, LADO_MINIATURA, ALTO_MINIATURA,
                ImageCrop.CENTRADO);
        StackPane marco = new StackPane(vista);
        marco.getStyleClass().addAll("team-gallery-item", "explore-card-photo-clickable");
        marco.setMinSize(LADO_MINIATURA, ALTO_MINIATURA);
        marco.setPrefSize(LADO_MINIATURA, ALTO_MINIATURA);
        marco.setMaxSize(LADO_MINIATURA, ALTO_MINIATURA);
        marco.setFocusTraversable(true);

        marco.setOnMouseEntered(e -> {
            marco.setStyle("-fx-border-color: " + colorEquipo + ";");
            escalar(vista, 1.06);
        });
        marco.setOnMouseExited(e -> {
            marco.setStyle("");
            escalar(vista, 1);
        });
        marco.setOnMouseClicked(e -> abrirVisor(indice));
        marco.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER
                    || e.getCode() == javafx.scene.input.KeyCode.SPACE) {
                abrirVisor(indice);
            }
        });
        return Optional.of(marco);
    }

    private void escalar(Node nodo, double destino) {
        ScaleTransition zoom = new ScaleTransition(HOVER, nodo);
        zoom.setToX(destino);
        zoom.setToY(destino);
        zoom.setInterpolator(Interpolator.EASE_BOTH);
        zoom.play();
    }

    private void abrirVisor(int indice) {
        if (imagenesGaleria.isEmpty()) {
            return;
        }
        indiceVisor = indice;
        visor.setVisible(true);
        visor.setManaged(true);
        visor.setOpacity(0);
        pintarVisor();

        FadeTransition entrada = new FadeTransition(APERTURA, visor);
        entrada.setToValue(1);
        entrada.setInterpolator(Interpolator.EASE_BOTH);
        entrada.play();
        visor.requestFocus();
    }

    private void pintarVisor() {
        String ruta = imagenesGaleria.get(indiceVisor);
        escenarioVisor.getChildren().clear();
        Image imagen = Imagenes.cargar(ruta, 1400, 0);
        if (imagen != null) {
            ImageView vista = new ImageView(imagen);
            vista.setPreserveRatio(true);
            vista.setSmooth(true);
            vista.fitWidthProperty().bind(escenarioVisor.widthProperty().subtract(48));
            vista.fitHeightProperty().bind(escenarioVisor.heightProperty().subtract(48));
            escenarioVisor.getChildren().add(vista);
        }
        lblVisorTitulo.setText(nombreDeArchivo(ruta));
        lblVisorContador.setText((indiceVisor + 1) + " / " + imagenesGaleria.size());
    }

    private String nombreDeArchivo(String ruta) {
        String archivo = ruta.substring(ruta.lastIndexOf('/') + 1);
        int punto = archivo.lastIndexOf('.');
        return (punto < 0 ? archivo : archivo.substring(0, punto)).toUpperCase(Locale.ROOT);
    }

    @FXML
    private void onAnterior() {
        if (imagenesGaleria.isEmpty()) {
            return;
        }
        indiceVisor = (indiceVisor - 1 + imagenesGaleria.size()) % imagenesGaleria.size();
        pintarVisor();
    }

    @FXML
    private void onSiguiente() {
        if (imagenesGaleria.isEmpty()) {
            return;
        }
        indiceVisor = (indiceVisor + 1) % imagenesGaleria.size();
        pintarVisor();
    }

    @FXML
    private void onCerrarVisor() {
        FadeTransition salida = new FadeTransition(APERTURA, visor);
        salida.setToValue(0);
        salida.setOnFinished(e -> {
            visor.setVisible(false);
            visor.setManaged(false);
            escenarioVisor.getChildren().clear();
        });
        salida.play();
    }

    // -------------------------------------------------------------- historial

    /** Recorre las sesiones guardadas buscando la vuelta de este piloto. */
    private List<Fila> historialDe(Driver piloto) {
        return sesiones.historial().stream()
                .flatMap(s -> vueltaDe(s, piloto).stream())
                .toList();
    }

    private Optional<Fila> vueltaDe(QualifyingSession sesion, Driver piloto) {
        return sesion.getResultados().stream()
                .filter(r -> r.getPilotoId() == piloto.getId())
                .findFirst()
                .map(r -> new Fila(
                        sesion.getFecha(),
                        sesion.getCircuito(),
                        "P" + r.getPosicion(),
                        r.isVueltaValida() ? FormatUtils.formatLapTime(r.getTiempoSegundos()) : "—",
                        r.isVueltaValida() ? FormatUtils.formatGap(r.getGap()) : "—",
                        r.getEstadoVuelta().getEtiqueta(),
                        r.getPosicion(),
                        r.isVueltaValida() ? r.getTiempoSegundos() : 0));
    }

    @FXML
    private void onVolver() {
        if (!Navigator.volver()) {
            Navigator.ir("explorar");
        }
    }

    // ---------------------------------------------------------------- apoyos

    private static String texto(String valor) {
        return valor == null || valor.isBlank() ? "—" : valor;
    }

    private static String numero(int valor) {
        return valor <= 0 ? "—" : String.valueOf(valor);
    }

    /** Fila de la tabla; se expone como record para las cell factories. */
    public record Fila(String fecha, String circuito, String posicion, String tiempo,
                       String gap, String estado, int orden, double segundos) {
    }
}
