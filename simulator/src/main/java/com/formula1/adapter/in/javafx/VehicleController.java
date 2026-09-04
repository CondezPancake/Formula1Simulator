package com.formula1.adapter.in.javafx;

import com.formula1.domain.model.Driver;
import com.formula1.domain.model.DrivingMode;
import com.formula1.domain.model.Vehicle;
import com.formula1.application.usecase.DriverService;
import com.formula1.application.usecase.TeamService;
import com.formula1.domain.service.ValidationException;
import com.formula1.application.usecase.VehicleService;
import com.formula1.util.ImageCrop;
import com.formula1.util.Imagenes;
import com.formula1.util.Iniciales;
import com.formula1.util.InputValidation;
import com.formula1.util.TeamColors;
import com.formula1.util.VehicleImages;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Gestión de vehículos: tabla filtrable con una ficha lateral de detalle,
 * edición y baja. El alta, la edición y la baja siguen pasando por
 * {@link Forms#vehiculo} y {@link VehicleService}. Comparar y Asignar
 * pilotos siguen operando sobre la selección de la tabla, como antes.
 */
public class VehicleController {

    private static final String TODOS_EQUIPOS = "TODOS LOS EQUIPOS";
    private static final double DETALLE_ANCHO_ARTE = 330;
    private static final double DETALLE_ALTO_ARTE = 220;
    private static final int VELOCIDAD_REFERENCIA = 400;

    private static final Map<String, Comparator<Vehicle>> COMPARADORES = new LinkedHashMap<>();
    static {
        COMPARADORES.put("VELOCIDAD MÁX.", Comparator.comparingInt(Vehicle::getVelocidadMaximaKmh));
        COMPARADORES.put("ACELERACIÓN", Comparator.comparingDouble(Vehicle::getAceleracion0100));
        COMPARADORES.put("MODELO", Comparator.comparing(Vehicle::getModelo, String.CASE_INSENSITIVE_ORDER));
        COMPARADORES.put("EQUIPO", Comparator.comparing(v -> v.getEquipo() == null ? "" : v.getEquipo(), String.CASE_INSENSITIVE_ORDER));
    }

    @FXML private Label lblConteo;
    @FXML private TextField buscador;
    @FXML private ComboBox<String> filtroEquipo;
    @FXML private Spinner<Integer> velocidadMinima;
    @FXML private ComboBox<String> ordenarPor;
    @FXML private Button btnOrdenDireccion;
    @FXML private Button btnComparar;
    @FXML private Button btnAsignarPilotos;
    @FXML private Button btnNuevo;

    @FXML private TableView<Vehicle> tabla;
    @FXML private TableColumn<Vehicle, String> colModelo;
    @FXML private TableColumn<Vehicle, String> colEquipo;
    @FXML private TableColumn<Vehicle, String> colMotor;
    @FXML private TableColumn<Vehicle, Number> colVelMax;
    @FXML private TableColumn<Vehicle, Number> colAceleracion;
    @FXML private TableColumn<Vehicle, Number> colVelNormal;

    @FXML private StackPane panelDetalle;
    @FXML private VBox detalleVacio;
    @FXML private ScrollPane detalleScroll;
    @FXML private VBox detalleContenido;
    @FXML private StackPane detalleFoto;
    @FXML private Label detalleModelo;
    @FXML private Label detalleSubtitulo;
    @FXML private VBox tabResumenAtributos;
    @FXML private VBox tabResumenInfo;
    @FXML private VBox tabEstadisticas;
    @FXML private Button btnEditar;
    @FXML private Button btnEliminar;

    private final VehicleService vehiculos;
    private final TeamService equipos;
    private final DriverService pilotos;

    private final ObservableList<Vehicle> datos = FXCollections.observableArrayList();
    private FilteredList<Vehicle> filtrados;
    private SortedList<Vehicle> ordenados;
    private boolean ordenDescendente = true;

    public VehicleController() {
        this(new VehicleService(), new TeamService(), new DriverService());
    }

    public VehicleController(VehicleService vehiculos, TeamService equipos) {
        this(vehiculos, equipos, new DriverService());
    }

    public VehicleController(VehicleService vehiculos, TeamService equipos, DriverService pilotos) {
        this.vehiculos = vehiculos;
        this.equipos = equipos;
        this.pilotos = pilotos;
    }

    @FXML
    public void initialize() {
        InputValidation.busqueda(buscador);
        SplitPane.setResizableWithParent(panelDetalle, false);
        tabla.setPlaceholder(estadoVacio());
        tabla.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        configurarFiltros();
        configurarOrden();
        configurarTabla();

        filtrados = new FilteredList<>(datos, v -> true);
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
        velocidadMinima.valueProperty().addListener((obs, antes, ahora) -> aplicarFiltros());
        ordenarPor.valueProperty().addListener((obs, antes, ahora) -> aplicarOrden());

        btnNuevo.setOnAction(e -> onNuevo());
        btnEditar.setOnAction(e -> onEditar());
        btnEliminar.setOnAction(e -> onEliminar());
        btnComparar.setOnAction(e -> onComparar());
        btnAsignarPilotos.setOnAction(e -> onAsignarPilotos());

        aplicarOrden();
        cargarDatos();
        mostrarDetalleVacio();
    }

    private VBox estadoVacio() {
        Label titulo = new Label("NO SE ENCONTRARON VEHÍCULOS");
        titulo.getStyleClass().add("mgmt-empty-title");
        Label subtitulo = new Label("Prueba con otro modelo, equipo o filtro.");
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

        velocidadMinima.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 400, 0, 5));
        InputValidation.entero(velocidadMinima, 0, 400);
    }

    private void aplicarFiltros() {
        String texto = buscador.getText() == null ? "" : buscador.getText().trim().toLowerCase(Locale.ROOT);
        String equipo = filtroEquipo.getValue();
        Integer minima = velocidadMinima.getValue() == null || velocidadMinima.getValue() == 0
                ? null : velocidadMinima.getValue();

        filtrados.setPredicate(vehiculo -> {
            boolean coincideTexto = texto.isEmpty()
                    || contiene(vehiculo.getModelo(), texto)
                    || contiene(vehiculo.getEquipo(), texto)
                    || contiene(vehiculo.getMotor(), texto);
            boolean coincideEquipo = equipo == null || TODOS_EQUIPOS.equals(equipo)
                    || equipo.equalsIgnoreCase(vehiculo.getEquipo());
            boolean coincideVelocidad = minima == null || vehiculo.getVelocidadMaximaKmh() >= minima;
            return coincideTexto && coincideEquipo && coincideVelocidad;
        });
        actualizarConteo();
    }

    private boolean contiene(String valor, String q) {
        return valor != null && valor.toLowerCase(Locale.ROOT).contains(q);
    }

    // --------------------------------------------------------------- orden

    private void configurarOrden() {
        ordenarPor.getItems().addAll(COMPARADORES.keySet());
        ordenarPor.setValue("VELOCIDAD MÁX.");
        btnOrdenDireccion.setText("↓");
        btnOrdenDireccion.setOnAction(e -> {
            ordenDescendente = !ordenDescendente;
            btnOrdenDireccion.setText(ordenDescendente ? "↓" : "↑");
            aplicarOrden();
        });
    }

    private void aplicarOrden() {
        Comparator<Vehicle> base = COMPARADORES.getOrDefault(ordenarPor.getValue(), COMPARADORES.get("VELOCIDAD MÁX."));
        ordenados.setComparator(ordenDescendente ? base.reversed() : base);
    }

    // --------------------------------------------------------------- tabla

    private void configurarTabla() {
        colModelo.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().getModelo()));
        colEquipo.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().getEquipo()));
        colMotor.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().getMotor()));
        colVelMax.setCellValueFactory(f -> new SimpleIntegerProperty(f.getValue().getVelocidadMaximaKmh()));
        colAceleracion.setCellValueFactory(f -> new SimpleDoubleProperty(f.getValue().getAceleracion0100()));
        colVelNormal.setCellValueFactory(f -> new SimpleIntegerProperty(
                f.getValue().rendimientoDe(DrivingMode.NORMAL).getVelocidadPromedioKmh()));
    }

    // ---------------------------------------------------------------- datos

    private void cargarDatos() {
        datos.setAll(vehiculos.listar());
        aplicarFiltros();
    }

    /** Vuelve a consultar el catálogo, preservando la selección actual si sigue existiendo. */
    void refrescarVista() {
        Vehicle seleccionado = tabla.getSelectionModel().getSelectedItem();
        cargarDatos();
        if (seleccionado != null) {
            tabla.getSelectionModel().select(seleccionado);
        }
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

    private void mostrarDetalle(Vehicle vehiculo) {
        detalleVacio.setVisible(false);
        detalleVacio.setManaged(false);
        detalleScroll.setVisible(true);
        detalleScroll.setManaged(true);
        detalleContenido.setVisible(true);
        detalleContenido.setManaged(true);

        pintarFoto(vehiculo);
        detalleModelo.setText(vehiculo.getModelo() == null ? "" : vehiculo.getModelo().toUpperCase(Locale.ROOT));
        detalleSubtitulo.setText(valorOTexto(vehiculo.getEquipo()) + "  ·  Motor " + valorOTexto(vehiculo.getMotor()));

        pintarResumen(vehiculo);
        pintarEstadisticas(vehiculo);
    }

    /** Foto principal del monoplaza con el livery del equipo como fondo. */
    private void pintarFoto(Vehicle vehiculo) {
        detalleFoto.getChildren().clear();
        detalleFoto.setMinSize(DETALLE_ANCHO_ARTE, DETALLE_ALTO_ARTE);
        detalleFoto.setPrefSize(DETALLE_ANCHO_ARTE, DETALLE_ALTO_ARTE);
        detalleFoto.setMaxSize(DETALLE_ANCHO_ARTE, DETALLE_ALTO_ARTE);

        Region base = new Region();
        base.setStyle("-fx-background-color: " + TeamColors.accesible(vehiculo.getEquipo()) + ";");
        detalleFoto.getChildren().add(base);

        List<String> vistas = VehicleImages.de(vehiculo.getModelo());
        Image imagen = vistas.isEmpty() ? null : Imagenes.cargar(vistas.get(0), DETALLE_ANCHO_ARTE * 2, 0);
        if (imagen != null) {
            ImageView vista = ImageCrop.encajar(imagen, DETALLE_ANCHO_ARTE, DETALLE_ALTO_ARTE, ImageCrop.CENTRADO);
            detalleFoto.getChildren().add(vista);
        } else {
            Label iniciales = new Label(Iniciales.de(vehiculo.getModelo()));
            iniciales.getStyleClass().add("mgmt-watermark");
            detalleFoto.getChildren().add(iniciales);
        }
    }

    private void pintarResumen(Vehicle vehiculo) {
        tabResumenAtributos.getChildren().setAll(
                filaAtributo("VELOCIDAD MÁXIMA", vehiculo.getVelocidadMaximaKmh(), VELOCIDAD_REFERENCIA, " km/h"),
                filaAtributo("MEDIA (NORMAL)",
                        vehiculo.rendimientoDe(DrivingMode.NORMAL).getVelocidadPromedioKmh(), VELOCIDAD_REFERENCIA, " km/h"));

        List<Driver> asignados = vehiculos.pilotosDe(vehiculo);
        String nombresPilotos = asignados.isEmpty() ? "Sin pilotos asignados"
                : asignados.stream().map(Driver::getNombre).collect(Collectors.joining(", "));

        tabResumenInfo.getChildren().setAll(
                filaInfo("MOTOR", valorOTexto(vehiculo.getMotor())),
                filaInfo("EQUIPO", valorOTexto(vehiculo.getEquipo())),
                filaInfo("ACELERACIÓN 0-100", String.format(Locale.ROOT, "%.2f s", vehiculo.getAceleracion0100())),
                filaInfoLarga("PILOTOS ASIGNADOS", nombresPilotos));
    }

    /**
     * A diferencia de Resumen, aquí se compara con el resto de vehículos
     * cargados (posición real en velocidad y aceleración) y se desglosa la
     * velocidad media por modo de conducción.
     */
    private void pintarEstadisticas(Vehicle vehiculo) {
        int total = datos.size();
        int puestoVelocidad = (int) datos.stream()
                .filter(v -> v.getVelocidadMaximaKmh() > vehiculo.getVelocidadMaximaKmh()).count() + 1;
        int puestoAceleracion = (int) datos.stream()
                .filter(v -> v.getAceleracion0100() < vehiculo.getAceleracion0100()).count() + 1;

        tabEstadisticas.getChildren().add(
                filaRanking("VELOCIDAD MÁXIMA", vehiculo.getVelocidadMaximaKmh() + " km/h",
                        vehiculo.getVelocidadMaximaKmh() / (double) VELOCIDAD_REFERENCIA, puestoVelocidad, total));
        tabEstadisticas.getChildren().add(
                filaRanking("ACELERACIÓN 0-100", String.format(Locale.ROOT, "%.2f s", vehiculo.getAceleracion0100()),
                        1 - (vehiculo.getAceleracion0100() - 1.0) / 9.0, puestoAceleracion, total));

        Label seccion = new Label("VELOCIDAD MEDIA POR MODO");
        seccion.getStyleClass().add("mgmt-section-label");
        tabEstadisticas.getChildren().add(seccion);
        for (DrivingMode modo : DrivingMode.values()) {
            int media = vehiculo.rendimientoDe(modo).getVelocidadPromedioKmh();
            tabEstadisticas.getChildren().add(
                    filaAtributo(modo.getEtiqueta().toUpperCase(Locale.ROOT), media, VELOCIDAD_REFERENCIA, " km/h"));
        }
    }

    private VBox filaRanking(String etiqueta, String valorTexto, double fraccion, int puesto, int total) {
        Label lbl = new Label(etiqueta);
        lbl.getStyleClass().add("mgmt-attr-label");
        Region espaciador = new Region();
        HBox.setHgrow(espaciador, Priority.ALWAYS);
        Label lblValor = new Label(valorTexto);
        lblValor.getStyleClass().add("mgmt-attr-value");
        Label puestoLbl = new Label("#" + puesto + " / " + total);
        puestoLbl.getStyleClass().add("mgmt-rank-badge");
        HBox cabecera = new HBox(8, lbl, espaciador, lblValor, puestoLbl);
        cabecera.setAlignment(Pos.CENTER_LEFT);

        ProgressBar barra = new ProgressBar(Math.max(0, Math.min(1, fraccion)));
        barra.getStyleClass().add("mgmt-stat-bar");
        barra.setMaxWidth(Double.MAX_VALUE);
        return new VBox(4, cabecera, barra);
    }

    private VBox filaAtributo(String etiqueta, int valor, int referencia, String unidad) {
        Label lbl = new Label(etiqueta);
        lbl.getStyleClass().add("mgmt-attr-label");
        Label lblValor = new Label(valor + unidad);
        lblValor.getStyleClass().add("mgmt-attr-value");
        Region espaciador = new Region();
        HBox.setHgrow(espaciador, Priority.ALWAYS);
        HBox cabecera = new HBox(8, lbl, espaciador, lblValor);
        cabecera.setAlignment(Pos.CENTER_LEFT);

        ProgressBar barra = new ProgressBar(Math.max(0, Math.min(1, valor / (double) referencia)));
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

    private VBox filaInfoLarga(String etiqueta, String valor) {
        Label lbl = new Label(etiqueta);
        lbl.getStyleClass().add("mgmt-info-label");
        Label lblValor = new Label(valor);
        lblValor.getStyleClass().add("mgmt-info-value");
        lblValor.setWrapText(true);
        return new VBox(2, lbl, lblValor);
    }

    private String valorOTexto(String valor) {
        return valor == null || valor.isBlank() ? "—" : valor;
    }

    // ------------------------------------------------------------------ CRUD

    private void onNuevo() {
        Forms.vehiculo(null, equipos.listar(), pilotos.listar()).ifPresent(this::guardar);
    }

    private void onEditar() {
        Vehicle actual = tabla.getSelectionModel().getSelectedItem();
        if (actual == null) {
            return;
        }
        Forms.vehiculo(actual, equipos.listar(), pilotos.listar()).ifPresent(this::guardar);
    }

    private void onEliminar() {
        Vehicle actual = tabla.getSelectionModel().getSelectedItem();
        if (actual == null) {
            return;
        }
        if (Navigator.confirmar("¿Eliminar el " + actual.getModelo() + "?")) {
            vehiculos.eliminar(actual.getModelo());
            cargarDatos();
        }
    }

    /** Asigna pilotos al vehículo seleccionado; solo se ofrecen los de su mismo equipo. */
    private void onAsignarPilotos() {
        Vehicle seleccionado = tabla.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            Navigator.aviso("Sin selección", "Elige un vehículo de la tabla.");
            return;
        }
        Forms.asignarPilotos(seleccionado, pilotos.porEquipo(seleccionado.getEquipo()))
                .ifPresent(ids -> {
                    try {
                        Vehicle actualizado = vehiculos.asignarPilotos(seleccionado, ids);
                        cargarDatos();
                        tabla.getSelectionModel().select(actualizado);
                    } catch (ValidationException e) {
                        Navigator.error("Asignación no válida", e.getMessage());
                    }
                });
    }

    private void onComparar() {
        var seleccion = tabla.getSelectionModel().getSelectedItems();
        if (seleccion.size() < 2) {
            Navigator.aviso("Selecciona al menos dos",
                    "Marca dos o más vehículos (Ctrl + clic) para compararlos.");
            return;
        }
        var modelos = seleccion.stream().map(Vehicle::getModelo).collect(Collectors.toList());
        Navigator.ir("vehicle-compare");
        if (Navigator.ultimoControlador() instanceof VehicleCompareController comparador) {
            comparador.comparar(modelos);
        }
    }

    private void guardar(Vehicle vehiculo) {
        try {
            vehiculos.guardar(vehiculo);
            cargarDatos();
            tabla.getSelectionModel().select(vehiculo);
        } catch (ValidationException e) {
            Navigator.error("Datos no válidos", e.getMessage());
        }
    }
}
