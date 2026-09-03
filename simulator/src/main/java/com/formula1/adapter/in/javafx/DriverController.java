package com.formula1.adapter.in.javafx;

import com.formula1.domain.model.Driver;
import com.formula1.domain.model.DriverRole;
import com.formula1.application.usecase.DriverService;
import com.formula1.application.usecase.TeamService;
import com.formula1.domain.service.ValidationException;
import com.formula1.util.DriverRating;
import com.formula1.util.F1Assets;
import com.formula1.util.ImageCrop;
import com.formula1.util.Imagenes;
import com.formula1.util.Iniciales;
import com.formula1.util.InputValidation;
import com.formula1.util.TeamColors;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.CacheHint;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.effect.Blend;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.ColorInput;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.ToIntFunction;

/**
 * Gestión de pilotos: tabla filtrable con una ficha lateral de detalle,
 * edición y baja. El alta, la edición y la baja siguen pasando por
 * {@link Forms#piloto} y {@link DriverService}; esta clase solo decide qué
 * se ve y cuándo se refresca.
 *
 * <p>La cadena {@code ObservableList -> FilteredList -> SortedList} hace el
 * filtrado y el orden de la tabla.
 */
public class DriverController {

    private static final String TODOS_EQUIPOS = "TODOS LOS EQUIPOS";
    private static final String TODOS_ROLES = "TODOS LOS ROLES";
    private static final String TODAS_NACIONALIDADES = "NACIONALIDAD";

    /**
     * Ancho fijo, deliberadamente por debajo de los 360 del panel: así el
     * arte del hero nunca empuja el ancho real del contenido (que incluye la
     * barra de scroll vertical) y no hace falta scroll horizontal.
     */
    private static final double DETALLE_ANCHO_ARTE = 330;
    private static final double DETALLE_ALTO_ARTE = 300;
    private static final double DETALLE_HEADROOM = 40;

    private static final Map<String, Comparator<Driver>> COMPARADORES = new LinkedHashMap<>();
    static {
        COMPARADORES.put("OVR", Comparator.comparingInt(DriverRating::ovr));
        COMPARADORES.put("VELOCIDAD", Comparator.comparingInt(d -> d.getHabilidad(Driver.HABILIDAD_VELOCIDAD)));
        COMPARADORES.put("CONSISTENCIA", Comparator.comparingInt(d -> d.getHabilidad(Driver.HABILIDAD_CONSISTENCIA)));
        COMPARADORES.put("LLUVIA", Comparator.comparingInt(d -> d.getHabilidad(Driver.HABILIDAD_LLUVIA)));
        COMPARADORES.put("EXPERIENCIA", Comparator.comparingInt(Driver::getExperiencia));
        COMPARADORES.put("NOMBRE", Comparator.comparing(Driver::getNombre, String.CASE_INSENSITIVE_ORDER));
    }

    @FXML private Label lblConteo;
    @FXML private TextField buscador;
    @FXML private ComboBox<String> filtroEquipo;
    @FXML private ComboBox<String> filtroRol;
    @FXML private ComboBox<String> filtroNacionalidad;
    @FXML private ComboBox<String> ordenarPor;
    @FXML private Button btnOrdenDireccion;
    @FXML private Button btnNuevo;

    @FXML private TableView<Driver> tabla;
    @FXML private TableColumn<Driver, Number> colId;
    @FXML private TableColumn<Driver, String> colNombre;
    @FXML private TableColumn<Driver, String> colEquipo;
    @FXML private TableColumn<Driver, String> colRol;
    @FXML private TableColumn<Driver, Number> colOvr;
    @FXML private TableColumn<Driver, Number> colVelocidad;
    @FXML private TableColumn<Driver, Number> colConsistencia;
    @FXML private TableColumn<Driver, Number> colLluvia;
    @FXML private TableColumn<Driver, Number> colExperiencia;

    @FXML private StackPane panelDetalle;
    @FXML private VBox detalleVacio;
    @FXML private ScrollPane detalleScroll;
    @FXML private VBox detalleContenido;
    @FXML private StackPane detalleFoto;
    @FXML private Label detalleNombrePila;
    @FXML private Label detalleApellido;
    @FXML private Label detalleOvr;
    @FXML private Label detalleEquipo;
    @FXML private Label detalleRol;
    @FXML private VBox tabResumenAtributos;
    @FXML private VBox tabResumenInfo;
    @FXML private VBox tabEstadisticas;
    @FXML private VBox tabHistorial;
    @FXML private Button btnEditar;
    @FXML private Button btnEliminar;

    private final DriverService pilotos;
    private final TeamService equipos;

    private final ObservableList<Driver> datos = FXCollections.observableArrayList();
    private FilteredList<Driver> filtrados;
    private SortedList<Driver> ordenados;
    private boolean ordenDescendente = true;

    public DriverController() {
        this(new DriverService(), new TeamService());
    }

    public DriverController(DriverService pilotos, TeamService equipos) {
        this.pilotos = pilotos;
        this.equipos = equipos;
    }

    @FXML
    public void initialize() {
        InputValidation.busqueda(buscador);
        SplitPane.setResizableWithParent(panelDetalle, false);
        tabla.setPlaceholder(estadoVacio());

        configurarFiltros();
        configurarOrden();
        configurarTabla();

        filtrados = new FilteredList<>(datos, d -> true);
        ordenados = new SortedList<>(filtrados);
        tabla.setItems(ordenados);
        tabla.getSelectionModel().selectedItemProperty().addListener((obs, antes, ahora) -> {
            if (ahora != null) {
                mostrarDetalle(ahora);
            } else {
                mostrarDetalleVacio();
            }
        });

        buscador.textProperty().addListener((obs, antes, ahora) -> aplicarFiltros());
        filtroEquipo.valueProperty().addListener((obs, antes, ahora) -> aplicarFiltros());
        filtroRol.valueProperty().addListener((obs, antes, ahora) -> aplicarFiltros());
        filtroNacionalidad.valueProperty().addListener((obs, antes, ahora) -> aplicarFiltros());
        ordenarPor.valueProperty().addListener((obs, antes, ahora) -> aplicarOrden());

        btnNuevo.setOnAction(e -> onNuevo());
        btnEditar.setOnAction(e -> onEditar());
        btnEliminar.setOnAction(e -> onEliminar());

        aplicarOrden();
        cargarDatos();
        mostrarDetalleVacio();
    }

    private VBox estadoVacio() {
        Label titulo = new Label("NO SE ENCONTRARON PILOTOS");
        titulo.getStyleClass().add("mgmt-empty-title");
        Label subtitulo = new Label("Prueba con otro nombre, equipo o filtro.");
        subtitulo.getStyleClass().add("mgmt-empty-subtitle");
        VBox caja = new VBox(6, titulo, subtitulo);
        caja.setAlignment(Pos.CENTER);
        return caja;
    }

    // ------------------------------------------------------------- filtros

    private void configurarFiltros() {
        filtroEquipo.getItems().add(TODOS_EQUIPOS);
        equipos.listar().forEach(t -> filtroEquipo.getItems().add(t.getNombre()));
        filtroEquipo.setValue(TODOS_EQUIPOS);

        filtroRol.getItems().add(TODOS_ROLES);
        for (DriverRole rol : DriverRole.values()) {
            filtroRol.getItems().add(rol.getEtiqueta());
        }
        filtroRol.setValue(TODOS_ROLES);

        filtroNacionalidad.getItems().add(TODAS_NACIONALIDADES);
        filtroNacionalidad.setValue(TODAS_NACIONALIDADES);
    }

    private void actualizarNacionalidades() {
        String actual = filtroNacionalidad.getValue();
        List<String> nacionalidades = datos.stream()
                .map(Driver::getNacionalidad)
                .filter(n -> n != null && !n.isBlank())
                .distinct()
                .sorted()
                .toList();
        filtroNacionalidad.getItems().setAll(TODAS_NACIONALIDADES);
        filtroNacionalidad.getItems().addAll(nacionalidades);
        filtroNacionalidad.setValue(
                nacionalidades.contains(actual) || TODAS_NACIONALIDADES.equals(actual)
                        ? actual : TODAS_NACIONALIDADES);
    }

    private void aplicarFiltros() {
        String texto = buscador.getText() == null ? "" : buscador.getText().trim().toLowerCase(Locale.ROOT);
        String equipo = filtroEquipo.getValue();
        String rol = filtroRol.getValue();
        String nacionalidad = filtroNacionalidad.getValue();

        filtrados.setPredicate(piloto -> {
            boolean coincideTexto = texto.isEmpty()
                    || contiene(piloto.getNombre(), texto)
                    || contiene(piloto.getEquipo(), texto)
                    || (piloto.getRol() != null && contiene(piloto.getRol().getEtiqueta(), texto));
            boolean coincideEquipo = equipo == null || TODOS_EQUIPOS.equals(equipo)
                    || equipo.equalsIgnoreCase(piloto.getEquipo());
            boolean coincideRol = rol == null || TODOS_ROLES.equals(rol)
                    || (piloto.getRol() != null && rol.equalsIgnoreCase(piloto.getRol().getEtiqueta()));
            boolean coincideNacionalidad = nacionalidad == null || TODAS_NACIONALIDADES.equals(nacionalidad)
                    || nacionalidad.equalsIgnoreCase(piloto.getNacionalidad());
            return coincideTexto && coincideEquipo && coincideRol && coincideNacionalidad;
        });
        actualizarConteo();
    }

    private boolean contiene(String valor, String q) {
        return valor != null && valor.toLowerCase(Locale.ROOT).contains(q);
    }

    // --------------------------------------------------------------- orden

    private void configurarOrden() {
        ordenarPor.getItems().addAll(COMPARADORES.keySet());
        ordenarPor.setValue("OVR");
        btnOrdenDireccion.setText("↓");
        btnOrdenDireccion.setOnAction(e -> {
            ordenDescendente = !ordenDescendente;
            btnOrdenDireccion.setText(ordenDescendente ? "↓" : "↑");
            aplicarOrden();
        });
    }

    private void aplicarOrden() {
        Comparator<Driver> base = COMPARADORES.getOrDefault(ordenarPor.getValue(), COMPARADORES.get("OVR"));
        ordenados.setComparator(ordenDescendente ? base.reversed() : base);
    }

    // --------------------------------------------------------------- tabla

    private void configurarTabla() {
        colId.setCellValueFactory(f -> new SimpleIntegerProperty(f.getValue().getId()));
        colNombre.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().getNombre()));
        colEquipo.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().getEquipo()));
        colRol.setCellValueFactory(f -> new SimpleStringProperty(
                f.getValue().getRol() == null ? "" : f.getValue().getRol().getEtiqueta()));
        colOvr.setCellValueFactory(f -> new SimpleIntegerProperty(DriverRating.ovr(f.getValue())));
        colVelocidad.setCellValueFactory(f -> new SimpleIntegerProperty(
                f.getValue().getHabilidad(Driver.HABILIDAD_VELOCIDAD)));
        colConsistencia.setCellValueFactory(f -> new SimpleIntegerProperty(
                f.getValue().getHabilidad(Driver.HABILIDAD_CONSISTENCIA)));
        colLluvia.setCellValueFactory(f -> new SimpleIntegerProperty(
                f.getValue().getHabilidad(Driver.HABILIDAD_LLUVIA)));
        colExperiencia.setCellValueFactory(f -> new SimpleIntegerProperty(f.getValue().getExperiencia()));
    }

    // ---------------------------------------------------------------- datos

    private void cargarDatos() {
        datos.setAll(pilotos.listar());
        actualizarNacionalidades();
        aplicarFiltros();
    }

    private void actualizarConteo() {
        lblConteo.setText(filtrados.size() + " / " + datos.size());
    }

    // -------------------------------------------------------------- detalle

    private void mostrarDetalleVacio() {
        detalleContenido.setVisible(false);
        detalleContenido.setManaged(false);
        detalleScroll.setVisible(false);
        detalleScroll.setManaged(false);
        detalleVacio.setVisible(true);
        detalleVacio.setManaged(true);
    }

    private void mostrarDetalle(Driver piloto) {
        detalleVacio.setVisible(false);
        detalleVacio.setManaged(false);
        detalleScroll.setVisible(true);
        detalleScroll.setManaged(true);
        detalleContenido.setVisible(true);
        detalleContenido.setManaged(true);

        pintarFoto(piloto);

        String nombre = piloto.getNombre() == null ? "" : piloto.getNombre().trim();
        int corte = nombre.lastIndexOf(' ');
        detalleNombrePila.setText(corte < 0 ? "" : nombre.substring(0, corte).toUpperCase(Locale.ROOT));
        detalleApellido.setText((corte < 0 ? nombre : nombre.substring(corte + 1)).toUpperCase(Locale.ROOT));
        detalleOvr.setText(String.valueOf(DriverRating.ovr(piloto)));
        detalleEquipo.setText(piloto.getEquipo() == null ? "" : piloto.getEquipo().toUpperCase(Locale.ROOT));
        detalleRol.setText(piloto.getRol() == null ? "SIN ROL" : piloto.getRol().getEtiqueta().toUpperCase(Locale.ROOT));

        pintarAtributos(piloto);
        pintarInfo(piloto);
        pintarEstadisticas(piloto);
        pintarHistorial(piloto);
    }

    /**
     * Fondo del color de equipo, patrón de velocidad teñido y retrato
     * recortado con la cabeza siempre visible y un hueco libre encima.
     * Aquí se prioriza la foto real del piloto ({@link Driver#getImagen()},
     * la que ya trae el catálogo) sobre el render recortado de F1Assets:
     * en la ficha grande interesa reconocer al piloto, y
     * {@link ImageCrop#encajar} ya se encarga de que ocupe el recuadro
     * entero sin dejar huecos.
     */
    private void pintarFoto(Driver piloto) {
        detalleFoto.getChildren().clear();
        detalleFoto.setMinSize(DETALLE_ANCHO_ARTE, DETALLE_ALTO_ARTE);
        detalleFoto.setPrefSize(DETALLE_ANCHO_ARTE, DETALLE_ALTO_ARTE);
        detalleFoto.setMaxSize(DETALLE_ANCHO_ARTE, DETALLE_ALTO_ARTE);

        String vivo = TeamColors.hex(piloto.getEquipo());
        String fondo = TeamColors.accesible(piloto.getEquipo());

        Region base = new Region();
        base.setStyle("-fx-background-color: " + fondo + ";");
        detalleFoto.getChildren().add(base);

        Image textura = Imagenes.cargar(F1Assets.texturaDrs(), DETALLE_ANCHO_ARTE, DETALLE_ALTO_ARTE);
        if (textura != null) {
            ImageView vistaTextura = new ImageView(textura);
            vistaTextura.setFitWidth(DETALLE_ANCHO_ARTE);
            vistaTextura.setFitHeight(DETALLE_ALTO_ARTE);
            vistaTextura.setEffect(new Blend(BlendMode.SRC_ATOP, null,
                    new ColorInput(0, 0, DETALLE_ANCHO_ARTE, DETALLE_ALTO_ARTE, Color.web(vivo))));
            vistaTextura.setOpacity(0.35);
            vistaTextura.setCache(true);
            vistaTextura.setCacheHint(CacheHint.SPEED);
            vistaTextura.setMouseTransparent(true);
            detalleFoto.getChildren().add(vistaTextura);
        }

        double altoFoto = DETALLE_ALTO_ARTE - DETALLE_HEADROOM;
        Image imagen = Imagenes.cargar(piloto.getImagen(), DETALLE_ANCHO_ARTE * 2, 0);
        if (imagen == null) {
            imagen = Imagenes.cargar(F1Assets.render(piloto.getCodigo()), DETALLE_ANCHO_ARTE * 2, 0);
        }
        if (imagen != null) {
            ImageView vista = ImageCrop.encajar(imagen, DETALLE_ANCHO_ARTE, altoFoto, ImageCrop.SESGO_RETRATO);
            StackPane.setAlignment(vista, Pos.BOTTOM_CENTER);
            detalleFoto.getChildren().add(vista);
        } else {
            Label iniciales = new Label(Iniciales.de(piloto.getNombre()));
            iniciales.getStyleClass().add("mgmt-watermark");
            detalleFoto.getChildren().add(iniciales);
        }
    }

    private void pintarAtributos(Driver piloto) {
        tabResumenAtributos.getChildren().setAll(
                filaAtributo("VELOCIDAD", piloto.getHabilidad(Driver.HABILIDAD_VELOCIDAD)),
                filaAtributo("CONSISTENCIA", piloto.getHabilidad(Driver.HABILIDAD_CONSISTENCIA)),
                filaAtributo("LLUVIA", piloto.getHabilidad(Driver.HABILIDAD_LLUVIA)));
    }

    private void pintarInfo(Driver piloto) {
        tabResumenInfo.getChildren().setAll(
                filaInfo("ROL", piloto.getRol() == null ? "—" : piloto.getRol().getEtiqueta().toUpperCase(Locale.ROOT)),
                filaInfo("EQUIPO ACTUAL", valorOTexto(piloto.getEquipo())),
                filaInfo("EXPERIENCIA", piloto.getExperiencia() + (piloto.getExperiencia() == 1 ? " año" : " años")),
                filaInfo("NACIONALIDAD", valorOTexto(piloto.getNacionalidad())),
                filaInfo("NÚMERO", piloto.getNumero() > 0 ? "#" + piloto.getNumero() : "—"),
                filaInfo("VICTORIAS", String.valueOf(piloto.getVictorias())),
                filaInfo("CAMPEONATOS", String.valueOf(piloto.getCampeonatos())));
    }

    /**
     * A diferencia de Resumen (los números del piloto), aquí se compara con
     * el resto de la parrilla: la posición real que ocupa en cada atributo
     * entre los pilotos ya cargados, no una cifra repetida.
     */
    private void pintarEstadisticas(Driver piloto) {
        int total = datos.size();
        tabEstadisticas.getChildren().setAll(
                filaRanking("OVR", DriverRating.ovr(piloto), rangoPor(DriverRating::ovr, piloto), total),
                filaRanking("VELOCIDAD", piloto.getHabilidad(Driver.HABILIDAD_VELOCIDAD),
                        rangoPor(d -> d.getHabilidad(Driver.HABILIDAD_VELOCIDAD), piloto), total),
                filaRanking("CONSISTENCIA", piloto.getHabilidad(Driver.HABILIDAD_CONSISTENCIA),
                        rangoPor(d -> d.getHabilidad(Driver.HABILIDAD_CONSISTENCIA), piloto), total),
                filaRanking("LLUVIA", piloto.getHabilidad(Driver.HABILIDAD_LLUVIA),
                        rangoPor(d -> d.getHabilidad(Driver.HABILIDAD_LLUVIA), piloto), total));
    }

    /** Puesto de {@code piloto} en la parrilla cargada según esa métrica (1 = el mejor). */
    private int rangoPor(ToIntFunction<Driver> metrica, Driver piloto) {
        int valor = metrica.applyAsInt(piloto);
        return (int) datos.stream().filter(d -> metrica.applyAsInt(d) > valor).count() + 1;
    }

    /** Igual que {@link #filaAtributo}, pero con el puesto en la parrilla junto al valor. */
    private VBox filaRanking(String etiqueta, int valor, int puesto, int total) {
        Label lbl = new Label(etiqueta);
        lbl.getStyleClass().add("mgmt-attr-label");
        Region espaciador = new Region();
        HBox.setHgrow(espaciador, Priority.ALWAYS);
        Label lblValor = new Label(String.valueOf(valor));
        lblValor.getStyleClass().add("mgmt-attr-value");
        Label puestoLbl = new Label("#" + puesto + " / " + total);
        puestoLbl.getStyleClass().add("mgmt-rank-badge");
        HBox cabecera = new HBox(8, lbl, espaciador, lblValor, puestoLbl);
        cabecera.setAlignment(Pos.CENTER_LEFT);

        ProgressBar barra = new ProgressBar(valor / 100.0);
        barra.getStyleClass().add("mgmt-stat-bar");
        barra.setMaxWidth(Double.MAX_VALUE);
        return new VBox(4, cabecera, barra);
    }

    private void pintarHistorial(Driver piloto) {
        Label texto = new Label("Consulta las sesiones de clasificación disputadas por "
                + piloto.getNombre() + " en su ficha completa de Explorar.");
        texto.setWrapText(true);
        texto.getStyleClass().add("hint");
        Button verFicha = new Button("VER FICHA COMPLETA →");
        verFicha.getStyleClass().add("icon-button");
        verFicha.setOnAction(e -> ExploreDriversController.abrirFicha(piloto.getId()));
        tabHistorial.getChildren().setAll(texto, verFicha);
    }

    private VBox filaAtributo(String etiqueta, int valor) {
        Label lbl = new Label(etiqueta);
        lbl.getStyleClass().add("mgmt-attr-label");
        Label lblValor = new Label(valor + " / 100");
        lblValor.getStyleClass().add("mgmt-attr-value");
        Region espaciador = new Region();
        HBox.setHgrow(espaciador, Priority.ALWAYS);
        HBox cabecera = new HBox(8, lbl, espaciador, lblValor);
        cabecera.setAlignment(Pos.CENTER_LEFT);

        ProgressBar barra = new ProgressBar(valor / 100.0);
        barra.getStyleClass().add("mgmt-stat-bar");
        barra.setMaxWidth(Double.MAX_VALUE);
        return new VBox(4, cabecera, barra);
    }

    private HBox filaInfo(String etiqueta, String valor) {
        Label lbl = new Label(etiqueta);
        lbl.getStyleClass().add("mgmt-info-label");
        Region espaciador = new Region();
        HBox.setHgrow(espaciador, Priority.ALWAYS);
        Label lblValor = new Label(valor);
        lblValor.getStyleClass().add("mgmt-info-value");
        HBox fila = new HBox(8, lbl, espaciador, lblValor);
        fila.setAlignment(Pos.CENTER_LEFT);
        return fila;
    }

    private String valorOTexto(String valor) {
        return valor == null || valor.isBlank() ? "—" : valor;
    }

    // ------------------------------------------------------------------ CRUD

    private void onNuevo() {
        Forms.piloto(null, equipos.listar(), pilotos.siguienteId()).ifPresent(this::guardar);
    }

    private void onEditar() {
        Driver actual = tabla.getSelectionModel().getSelectedItem();
        if (actual == null) {
            return;
        }
        Forms.piloto(actual, equipos.listar(), actual.getId()).ifPresent(this::guardar);
    }

    private void onEliminar() {
        Driver actual = tabla.getSelectionModel().getSelectedItem();
        if (actual == null) {
            return;
        }
        if (Navigator.confirmar("¿Eliminar a " + actual.getNombre() + "?")) {
            pilotos.eliminar(actual.getId());
            cargarDatos();
        }
    }

    private void guardar(Driver piloto) {
        try {
            pilotos.guardar(piloto);
            cargarDatos();
            tabla.getSelectionModel().select(piloto);
        } catch (ValidationException e) {
            Navigator.error("Datos no válidos", e.getMessage());
        }
    }
}
