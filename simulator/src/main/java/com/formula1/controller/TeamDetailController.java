package com.formula1.controller;

import com.formula1.domain.model.Driver;
import com.formula1.domain.model.DrivingMode;
import com.formula1.domain.model.LapResult;
import com.formula1.domain.model.LapStatus;
import com.formula1.domain.model.QualifyingSession;
import com.formula1.domain.model.Team;
import com.formula1.domain.model.Vehicle;
import com.formula1.service.QualifyingService;
import com.formula1.service.TeamService;
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
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
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
 * Ficha de una escudería, con el reparto de formula1.com/en/teams/&lt;equipo&gt;:
 * hero con el monoplaza, palmarés, pilotos, resumen y perfil, y galería.
 *
 * <p>Se abre desde el catálogo con {@link ExploreTeamsController#abrirFicha} y
 * vuelve a él con {@code Navigator.volver()}, que restaura el nodo exacto y su
 * pestaña, igual que hace la ficha de piloto.
 *
 * <p>Dos orígenes de datos que no se mezclan a propósito: el <b>palmarés</b>
 * sale del catálogo ({@link Team}, datos históricos reales) y el bloque
 * <b>en este simulador</b> se calcula al vuelo sobre las sesiones ya
 * disputadas. Rotularlos por separado evita dar por ganado en la partida algo
 * que solo ocurrió en la realidad.
 */
public class TeamDetailController {

    private static final double LADO_LOGO = 58;
    private static final double ANCHO_HERO_COCHE = 720;
    private static final double ALTO_HERO = 300;
    private static final double ANCHO_RENDER = 132;
    private static final double LADO_MINIATURA = 168;
    private static final double ALTO_MINIATURA = 104;
    private static final double ANCHO_TARJETA_PILOTO = 340;
    private static final double ALTO_TARJETA_PILOTO = 200;

    /** Inclinación del nombre y las cifras: Titillium Web no trae cursiva. */
    private static final double SESGO_CURSIVA = -0.16;

    private static final Duration HOVER = Duration.millis(160);
    private static final Duration APERTURA = Duration.millis(200);

    @FXML private ScrollPane scroll;
    @FXML private StackPane hero;
    @FXML private StackPane marcoCoche;
    @FXML private StackPane marcoLogo;
    @FXML private Label lblNombre;
    @FXML private Label lblMeta;
    @FXML private Label lblChasis;
    @FXML private HBox retratosHero;
    @FXML private Button btnVolver;

    @FXML private HBox palmares;
    @FXML private HBox prestaciones;
    @FXML private VBox rendimientoModos;
    @FXML private Label lblRendimientoNota;
    @FXML private HBox estadisticasSimulador;
    @FXML private Label lblResumenSesiones;
    @FXML private FlowPane tarjetasPilotos;

    @FXML private Label lblDescripcion;
    @FXML private HBox resumenCifras;
    @FXML private VBox perfil;

    @FXML private TilePane galeria;

    @FXML private StackPane visor;
    @FXML private StackPane escenarioVisor;
    @FXML private Label lblVisorTitulo;
    @FXML private Label lblVisorEquipo;
    @FXML private Label lblVisorContador;

    private final TeamService equipos;
    private final VehicleService vehiculos;
    private final QualifyingService sesiones;

    /** Rutas de las seis imágenes de la galería, en el orden en que se ven. */
    private final List<String> imagenesGaleria = new ArrayList<>();
    private int indiceVisor;
    private String colorEquipo = "#6c6c80";

    public TeamDetailController() {
        this(new TeamService(), new VehicleService(), new QualifyingService());
    }

    /**
     * Constructor de dos servicios que conservan las pruebas de carga de
     * vistas. Delega en el completo con un {@link QualifyingService} propio.
     */
    public TeamDetailController(TeamService equipos, VehicleService vehiculos) {
        this(equipos, vehiculos, new QualifyingService());
    }

    /** Constructor inyectable completo. */
    public TeamDetailController(TeamService equipos, VehicleService vehiculos,
                                QualifyingService sesiones) {
        this.equipos = equipos;
        this.vehiculos = vehiculos;
        this.sesiones = sesiones;
    }

    @FXML
    public void initialize() {
        hero.setMinHeight(ALTO_HERO);
        hero.setPrefHeight(ALTO_HERO);
        // El nombre y las cifras grandes van inclinados: la tipografía
        // empaquetada no tiene cara itálica y JavaFX no la sintetiza.
        lblNombre.getTransforms().add(new Shear(SESGO_CURSIVA, 0));
        // Esc cierra el visor esté donde esté el foco dentro de la ficha.
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
    }

    /** Vuelca el equipo pedido. Se llama justo después de navegar aquí. */
    public void mostrar(String nombreEquipo) {
        Optional<Team> encontrado = equipos.porNombre(nombreEquipo);
        if (encontrado.isEmpty()) {
            lblNombre.setText("Equipo no encontrado");
            return;
        }
        Team equipo = encontrado.get();
        colorEquipo = TeamColors.hex(equipo.getNombre());
        Optional<Vehicle> coche = vehiculos.delEquipo(equipo.getNombre());
        List<Driver> pilotos = equipos.pilotosDe(equipo);

        pintarHero(equipo, coche, pilotos);
        pintarPalmares(equipo);
        pintarPrestaciones(coche);
        pintarEstadisticasSimulador(pilotos);
        pintarPilotos(pilotos);
        pintarResumen(equipo);
        pintarPerfil(equipo, coche);
        pintarGaleria(equipo, coche, pilotos);
    }

    // ------------------------------------------------------------------ hero

    private void pintarHero(Team equipo, Optional<Vehicle> coche, List<Driver> pilotos) {
        String fondo = TeamColors.accesible(equipo.getNombre());

        hero.getChildren().removeIf(nodo -> nodo.getStyleClass().contains("team-hero-layer"));
        // Las capas van al fondo en orden inverso para quedar bajo el coche.
        hero.getChildren().add(0, capa(velo(fondo)));
        texturaVelocidad().ifPresent(textura -> hero.getChildren().add(0, textura));
        hero.getChildren().add(0, capa(base(fondo)));

        marcoCoche.getChildren().clear();
        coche.map(Vehicle::getImagen)
                .map(ruta -> Imagenes.cargar(ruta, ANCHO_HERO_COCHE, 0))
                .ifPresent(imagen -> marcoCoche.getChildren()
                        .add(ImageCrop.encajar(imagen, ANCHO_HERO_COCHE, ALTO_HERO,
                                ImageCrop.CENTRADO)));

        marcoLogo.getChildren().clear();
        Image logo = Imagenes.cargar(F1Assets.logo(equipo.getNombre()), LADO_LOGO * 2, 0);
        if (logo != null) {
            ImageView vista = new ImageView(logo);
            vista.setFitHeight(LADO_LOGO);
            vista.setPreserveRatio(true);
            vista.setSmooth(true);
            marcoLogo.getChildren().add(vista);
        }

        lblNombre.setText(equipo.getNombre().toUpperCase(Locale.ROOT));
        lblMeta.setText(texto(equipo.getPais()) + "  ·  MOTOR "
                + texto(equipo.getMotor()).toUpperCase(Locale.ROOT));
        lblChasis.setText(coche.map(Vehicle::getModelo).orElse("SIN MONOPLAZA"));
        lblChasis.setStyle("-fx-background-color: " + colorEquipo + ";");

        // Los dos renders de cuerpo entero, solapados como en el catálogo.
        retratosHero.getChildren().setAll(pilotos.stream()
                .map(this::renderPiloto)
                .flatMap(Optional::stream)
                .toList());
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

    /** Velo de izquierda a derecha para que el texto gane a la foto. */
    private String velo(String fondo) {
        return "-fx-background-color: linear-gradient(to right, "
                + fondo + "f2 0%, " + fondo + "99 42%, transparent 82%);";
    }

    /**
     * Patrón de velocidad de la F1 teñido con el color del equipo. La máscara
     * es blanca con alfa: se tiñe con un Blend y se deja muy translúcida para
     * que quede como textura de fondo, no como dibujo.
     */
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
        // Sin cachear, el Blend se recompone en cada fotograma del hero.
        vista.setCache(true);
        vista.setCacheHint(javafx.scene.CacheHint.SPEED);

        StackPane caja = new StackPane(vista);
        caja.getStyleClass().add("team-hero-layer");
        caja.setMouseTransparent(true);
        StackPane.setAlignment(vista, Pos.CENTER_LEFT);
        return Optional.of(caja);
    }

    private Optional<ImageView> renderPiloto(Driver piloto) {
        Image render = Imagenes.cargar(F1Assets.render(piloto.getCodigo()), ANCHO_RENDER * 2, 0);
        if (render == null) {
            return Optional.empty();
        }
        ImageView vista = new ImageView(render);
        vista.setFitWidth(ANCHO_RENDER);
        vista.setPreserveRatio(true);
        vista.setSmooth(true);
        vista.setMouseTransparent(true);
        return Optional.of(vista);
    }

    // -------------------------------------------------------------- palmarés

    private void pintarPalmares(Team equipo) {
        palmares.getChildren().setAll(
                cifra(String.valueOf(equipo.getCampeonatos()), "CAMPEONATOS", true),
                cifra(numero(equipo.getGranPremios()), "GRANDES PREMIOS", false),
                cifra(numero(equipo.getVictorias()), "VICTORIAS", false),
                cifra(numero(equipo.getPodios()), "PODIOS", false),
                cifra(numero(equipo.getPoles()), "POLES", false),
                cifra(equipo.getPrimeraParticipacion() > 0
                        ? String.valueOf(equipo.getPrimeraParticipacion()) : "—", "DESDE", false));
    }

    /**
     * Prestaciones del monoplaza: punta, aceleración y ritmo por modo.
     *
     * El rendimiento por modo de conducción es dato real del catálogo que hasta
     * ahora no se veía en ninguna pantalla, y es lo que explica por qué dos
     * coches con la misma punta no ruedan igual.
     */
    private void pintarPrestaciones(Optional<Vehicle> coche) {
        prestaciones.getChildren().clear();
        rendimientoModos.getChildren().clear();

        if (coche.isEmpty()) {
            lblRendimientoNota.setText("Sin monoplaza asignado en el catálogo.");
            prestaciones.getChildren().addAll(
                    cifra("—", "VELOCIDAD PUNTA", false),
                    cifra("—", "0-100 KM/H", false));
            return;
        }

        Vehicle vehiculo = coche.get();
        lblRendimientoNota.setText("Velocidad media por modo de conducción");
        prestaciones.getChildren().addAll(
                cifra(vehiculo.getVelocidadMaximaKmh() + " km/h", "VELOCIDAD PUNTA", true),
                cifra(String.format(java.util.Locale.ROOT, "%.1f s",
                        vehiculo.getAceleracion0100()), "0-100 KM/H", false),
                cifra(texto(vehiculo.getMotor()), "UNIDAD DE POTENCIA", false));

        // La escala arranca en la punta del coche para que las tres barras se
        // comparen entre sí y no contra un máximo inventado.
        double referencia = Math.max(1, vehiculo.getVelocidadMaximaKmh());
        for (DrivingMode modo : DrivingMode.values()) {
            int media = vehiculo.rendimientoDe(modo).getVelocidadPromedioKmh();
            rendimientoModos.getChildren().add(
                    barraModo(modo.getEtiqueta().toUpperCase(Locale.ROOT), media, referencia));
        }
    }

    private VBox barraModo(String etiqueta, int velocidadMedia, double referencia) {
        Label lbl = new Label(etiqueta);
        lbl.getStyleClass().add("team-profile-label");
        Label valor = new Label(velocidadMedia + " km/h");
        valor.getStyleClass().add("team-profile-value");
        Region espaciador = new Region();
        HBox.setHgrow(espaciador, javafx.scene.layout.Priority.ALWAYS);
        HBox cabecera = new HBox(8, lbl, espaciador, valor);
        cabecera.setAlignment(Pos.CENTER_LEFT);

        ProgressBar barra = new ProgressBar(Math.min(1, velocidadMedia / referencia));
        barra.getStyleClass().add("team-skill-bar");
        barra.setMaxWidth(Double.MAX_VALUE);
        barra.setStyle("-fx-accent: " + colorEquipo + ";");
        return new VBox(2, cabecera, barra);
    }

    /**
     * Recorre las sesiones guardadas sumando las vueltas de los dos pilotos.
     * No hay sistema de puntos —esto es una clasificación—, así que se cuenta
     * lo que el motor sí produce: poles, podios, mejor vuelta y abandonos.
     */
    private void pintarEstadisticasSimulador(List<Driver> pilotos) {
        List<Integer> ids = pilotos.stream().map(Driver::getId).toList();
        List<LapResult> vueltas = sesiones.historial().stream()
                .map(QualifyingSession::getResultados)
                .flatMap(List::stream)
                .filter(r -> ids.contains(r.getPilotoId()))
                .toList();

        long sesionesDisputadas = sesiones.historial().stream()
                .filter(s -> s.getResultados().stream()
                        .anyMatch(r -> ids.contains(r.getPilotoId())))
                .count();
        long poles = vueltas.stream().filter(r -> r.getPosicion() == 1).count();
        long podios = vueltas.stream().filter(r -> r.getPosicion() <= 3).count();
        long abandonos = vueltas.stream()
                .filter(r -> r.getEstadoVuelta() == LapStatus.OUT).count();
        String mejorVuelta = vueltas.stream()
                .filter(LapResult::isVueltaValida)
                .min(Comparator.comparingDouble(LapResult::getTiempoSegundos))
                .map(r -> FormatUtils.formatLapTime(r.getTiempoSegundos()))
                .orElse("—");

        lblResumenSesiones.setText(vueltas.isEmpty()
                ? "Todavía no ha salido a pista en ninguna clasificación guardada."
                : "Sumando las vueltas de sus dos pilotos.");

        estadisticasSimulador.getChildren().setAll(
                cifra(String.valueOf(sesionesDisputadas), "SESIONES", false),
                cifra(String.valueOf(poles), "POLES", true),
                cifra(String.valueOf(podios), "PODIOS", false),
                cifra(mejorVuelta, "MEJOR VUELTA", false),
                cifra(String.valueOf(abandonos), "ABANDONOS", false));
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

    // --------------------------------------------------------------- pilotos

    private void pintarPilotos(List<Driver> pilotos) {
        tarjetasPilotos.getChildren().setAll(pilotos.stream()
                .map(this::tarjetaPiloto)
                .toList());
    }

    /** Tarjeta grande con el render de cuerpo entero, al estilo del catálogo. */
    private StackPane tarjetaPiloto(Driver piloto) {
        Region fondo = new Region();
        fondo.setStyle("-fx-background-color: linear-gradient(from 0% 100% to 100% 0%, "
                + TeamColors.accesible(piloto.getEquipo()) + " 30%, " + colorEquipo + "55 100%);");

        Label dorsal = new Label(String.valueOf(piloto.getNumero()));
        dorsal.getStyleClass().add("team-driver-number");
        StackPane.setAlignment(dorsal, Pos.TOP_RIGHT);
        StackPane.setMargin(dorsal, new Insets(6, 12, 0, 0));

        VBox identidad = new VBox(-4);
        String[] partes = piloto.getNombre().split("\\s+", 2);
        Label nombre = new Label(partes[0].toUpperCase(Locale.ROOT));
        nombre.getStyleClass().add("team-driver-first");
        Label apellido = new Label(partes.length > 1
                ? partes[1].toUpperCase(Locale.ROOT) : "");
        apellido.getStyleClass().add("team-driver-last");
        apellido.getTransforms().add(new Shear(SESGO_CURSIVA, 0));
        Label codigo = new Label(texto(piloto.getCodigo()) + "  ·  " + texto(piloto.getNacionalidad()));
        codigo.getStyleClass().add("team-driver-code");
        identidad.getChildren().addAll(nombre, apellido, codigo);
        identidad.setPadding(new Insets(14, 14, 0, 14));
        // Sin este tope, el StackPane estira el VBox a los 200 px enteros de
        // la tarjeta; como el contenido de un VBox se alinea arriba por
        // defecto, la identidad y las habilidades -una "TOP_LEFT" y otra
        // "BOTTOM_LEFT"- acababan dibujando en el mismo sitio.
        identidad.setMaxHeight(Region.USE_PREF_SIZE);
        StackPane.setAlignment(identidad, Pos.TOP_LEFT);

        VBox habilidades = new VBox(4,
                habilidad("VELOCIDAD", piloto.getHabilidad(Driver.HABILIDAD_VELOCIDAD)),
                habilidad("CONSISTENCIA", piloto.getHabilidad(Driver.HABILIDAD_CONSISTENCIA)),
                habilidad("LLUVIA", piloto.getHabilidad(Driver.HABILIDAD_LLUVIA)));
        habilidades.setMaxWidth(128);
        habilidades.setMaxHeight(Region.USE_PREF_SIZE);
        habilidades.setPadding(new Insets(0, 0, 12, 14));
        StackPane.setAlignment(habilidades, Pos.BOTTOM_LEFT);

        StackPane tarjeta = new StackPane(fondo, identidad);
        renderPiloto(piloto).ifPresent(vista -> {
            // Los renders son mucho más altos que anchos (320x560): fijar solo
            // el ancho los hacía sobresalir por arriba de la tarjeta -sin
            // recortar- y taparse con el nombre. Con las dos cotas puestas,
            // JavaFX escala por la que primero toque, así que nunca desborda.
            vista.setFitWidth(150);
            vista.setFitHeight(ALTO_TARJETA_PILOTO);
            StackPane.setAlignment(vista, Pos.BOTTOM_RIGHT);
            tarjeta.getChildren().add(vista);
        });
        tarjeta.getChildren().addAll(dorsal, habilidades);
        tarjeta.getStyleClass().add("team-driver-card");
        tarjeta.setMinSize(ANCHO_TARJETA_PILOTO, ALTO_TARJETA_PILOTO);
        tarjeta.setPrefSize(ANCHO_TARJETA_PILOTO, ALTO_TARJETA_PILOTO);
        tarjeta.setMaxSize(ANCHO_TARJETA_PILOTO, ALTO_TARJETA_PILOTO);
        tarjeta.setCursor(Cursor.HAND);
        tarjeta.setFocusTraversable(true);

        String halo = "-fx-effect: dropshadow(gaussian, " + colorEquipo + ", 22, 0.3, 0, 6);";
        tarjeta.setOnMouseEntered(e -> tarjeta.setStyle(halo));
        tarjeta.setOnMouseExited(e -> tarjeta.setStyle(""));
        tarjeta.setOnMouseClicked(e -> ExploreDriversController.abrirFicha(piloto.getId()));
        tarjeta.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.SPACE) {
                ExploreDriversController.abrirFicha(piloto.getId());
            }
        });
        return tarjeta;
    }

    private VBox habilidad(String etiqueta, int valor) {
        Label lbl = new Label(etiqueta + "  " + valor);
        lbl.getStyleClass().add("team-skill-label");
        ProgressBar barra = new ProgressBar(valor / 100.0);
        barra.getStyleClass().add("team-skill-bar");
        barra.setMaxWidth(Double.MAX_VALUE);
        barra.setStyle("-fx-accent: " + colorEquipo + ";");
        return new VBox(1, lbl, barra);
    }

    // ------------------------------------------------------ resumen y perfil

    private void pintarResumen(Team equipo) {
        lblDescripcion.setText(equipo.getDescripcion() == null || equipo.getDescripcion().isBlank()
                ? "Sin reseña disponible para esta escudería."
                : equipo.getDescripcion());

        resumenCifras.getChildren().setAll(
                dato("GP DISPUTADOS", numero(equipo.getGranPremios())),
                dato("PODIOS", numero(equipo.getPodios())),
                dato("POLES", numero(equipo.getPoles())),
                dato("MUNDIALES", numero(equipo.getCampeonatos())));
    }

    private void pintarPerfil(Team equipo, Optional<Vehicle> coche) {
        // El título se conserva; debajo se reconstruyen las filas.
        perfil.getChildren().retainAll(perfil.getChildren().get(0));
        perfil.getChildren().addAll(
                fila("Nombre completo", equipo.getNombreCompleto()),
                fila("Base", equipo.getBase()),
                fila("Jefe de equipo", equipo.getJefeEquipo()),
                fila("Jefe técnico", equipo.getJefeTecnico()),
                fila("Chasis", coche.map(Vehicle::getModelo).orElse(null)),
                fila("Unidad de potencia", coche.map(Vehicle::getMotor).orElse(equipo.getMotor())),
                fila("Piloto reserva", equipo.getPilotoReserva()),
                fila("Primera participación", equipo.getPrimeraParticipacion() > 0
                        ? String.valueOf(equipo.getPrimeraParticipacion()) : null));
    }

    private HBox fila(String etiqueta, String valor) {
        Label lblEtiqueta = new Label(etiqueta);
        lblEtiqueta.getStyleClass().add("team-profile-label");
        Label lblValor = new Label(texto(valor));
        lblValor.getStyleClass().add("team-profile-value");
        Region espaciador = new Region();
        HBox.setHgrow(espaciador, javafx.scene.layout.Priority.ALWAYS);
        HBox fila = new HBox(10, lblEtiqueta, espaciador, lblValor);
        fila.setAlignment(Pos.CENTER_LEFT);
        fila.getStyleClass().add("team-profile-row");
        return fila;
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
     * Seis imágenes reales: las tres vistas del monoplaza, los dos renders de
     * los pilotos y el logo. Si alguna falta, la rejilla simplemente enseña
     * las que haya en vez de rellenar con marcadores vacíos.
     */
    private void pintarGaleria(Team equipo, Optional<Vehicle> coche, List<Driver> pilotos) {
        imagenesGaleria.clear();
        coche.map(Vehicle::getModelo).map(VehicleImages::de).ifPresent(imagenesGaleria::addAll);
        pilotos.stream()
                .map(piloto -> F1Assets.render(piloto.getCodigo()))
                .filter(java.util.Objects::nonNull)
                .forEach(imagenesGaleria::add);
        String logo = F1Assets.logo(equipo.getNombre());
        if (logo != null) {
            imagenesGaleria.add(logo);
        }
        while (imagenesGaleria.size() > 6) {
            imagenesGaleria.remove(imagenesGaleria.size() - 1);
        }

        lblVisorEquipo.setText(equipo.getNombre().toUpperCase(Locale.ROOT));
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
            if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.SPACE) {
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
            // Se ajusta al escenario sin recortar: aquí manda ver la foto entera.
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
}
