package com.formula1.adapter.in.javafx;

import com.formula1.domain.model.Circuit;
import com.formula1.application.usecase.CircuitService;
import com.formula1.domain.service.ValidationException;
import com.formula1.util.FormatUtils;
import com.formula1.util.ImageCrop;
import com.formula1.util.Imagenes;
import com.formula1.util.Iniciales;
import com.formula1.util.InputValidation;

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
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
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

/**
 * Gestión de circuitos: tabla filtrable con una ficha lateral de detalle,
 * edición y baja. El alta, la edición y la baja siguen pasando por
 * {@link Forms#circuito} y {@link CircuitService}; la ficha completa
 * ({@code CircuitDetailController}) se enlaza desde la pestaña Historial.
 */
public class CircuitController {

    private static final String TODOS_PAISES = "PAÍS";
    private static final double DETALLE_ANCHO_ARTE = 330;
    private static final double DETALLE_ALTO_ARTE = 220;

    private static final Map<String, Comparator<Circuit>> COMPARADORES = new LinkedHashMap<>();
    static {
        COMPARADORES.put("NOMBRE", Comparator.comparing(Circuit::getNombre, String.CASE_INSENSITIVE_ORDER));
        COMPARADORES.put("LONGITUD", Comparator.comparingDouble(Circuit::getLongitudKm));
        COMPARADORES.put("VUELTAS", Comparator.comparingInt(Circuit::getVueltas));
        COMPARADORES.put("F. TÉCNICO", Comparator.comparingDouble(Circuit::getFactorTecnico));
    }

    @FXML private Label lblConteo;
    @FXML private TextField buscador;
    @FXML private ComboBox<String> filtroPais;
    @FXML private ComboBox<String> ordenarPor;
    @FXML private Button btnOrdenDireccion;
    @FXML private Button btnNuevo;

    @FXML private TableView<Circuit> tabla;
    @FXML private TableColumn<Circuit, String> colNombre;
    @FXML private TableColumn<Circuit, String> colPais;
    @FXML private TableColumn<Circuit, Number> colLongitud;
    @FXML private TableColumn<Circuit, Number> colVueltas;
    @FXML private TableColumn<Circuit, String> colRecord;
    @FXML private TableColumn<Circuit, Number> colFactor;

    @FXML private StackPane panelDetalle;
    @FXML private VBox detalleVacio;
    @FXML private ScrollPane detalleScroll;
    @FXML private VBox detalleContenido;
    @FXML private StackPane detalleFoto;
    @FXML private Label detalleNombre;
    @FXML private Label detalleSubtitulo;
    @FXML private VBox tabResumen;
    @FXML private VBox tabHistorial;
    @FXML private Button btnEditar;
    @FXML private Button btnEliminar;

    private final CircuitService circuitos;

    private final ObservableList<Circuit> datos = FXCollections.observableArrayList();
    private FilteredList<Circuit> filtrados;
    private SortedList<Circuit> ordenados;
    private boolean ordenAscendente = true;

    public CircuitController() {
        this(new CircuitService());
    }

    public CircuitController(CircuitService circuitos) {
        this.circuitos = circuitos;
    }

    @FXML
    public void initialize() {
        InputValidation.busqueda(buscador);
        SplitPane.setResizableWithParent(panelDetalle, false);
        tabla.setPlaceholder(estadoVacio());

        configurarFiltros();
        configurarOrden();
        configurarTabla();

        filtrados = new FilteredList<>(datos, c -> true);
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
        filtroPais.valueProperty().addListener((obs, antes, ahora) -> aplicarFiltros());
        ordenarPor.valueProperty().addListener((obs, antes, ahora) -> aplicarOrden());

        btnNuevo.setOnAction(e -> onNuevo());
        btnEditar.setOnAction(e -> onEditar());
        btnEliminar.setOnAction(e -> onEliminar());

        aplicarOrden();
        cargarDatos();
        mostrarDetalleVacio();
    }

    private VBox estadoVacio() {
        Label titulo = new Label("NO SE ENCONTRARON CIRCUITOS");
        titulo.getStyleClass().add("mgmt-empty-title");
        Label subtitulo = new Label("Prueba con otro nombre, país o filtro.");
        subtitulo.getStyleClass().add("mgmt-empty-subtitle");
        VBox caja = new VBox(6, titulo, subtitulo);
        caja.setAlignment(Pos.CENTER);
        return caja;
    }

    // ------------------------------------------------------------- filtros

    private void configurarFiltros() {
        filtroPais.getItems().add(TODOS_PAISES);
        filtroPais.setValue(TODOS_PAISES);
    }

    private void actualizarPaises() {
        String actual = filtroPais.getValue();
        List<String> paises = datos.stream()
                .map(Circuit::getPais)
                .filter(p -> p != null && !p.isBlank())
                .distinct()
                .sorted()
                .toList();
        filtroPais.getItems().setAll(TODOS_PAISES);
        filtroPais.getItems().addAll(paises);
        filtroPais.setValue(paises.contains(actual) || TODOS_PAISES.equals(actual) ? actual : TODOS_PAISES);
    }

    private void aplicarFiltros() {
        String texto = buscador.getText() == null ? "" : buscador.getText().trim().toLowerCase(Locale.ROOT);
        String pais = filtroPais.getValue();
        filtrados.setPredicate(circuito -> {
            boolean coincideTexto = texto.isEmpty()
                    || contiene(circuito.getNombre(), texto)
                    || contiene(circuito.getPais(), texto);
            boolean coincidePais = pais == null || TODOS_PAISES.equals(pais) || pais.equalsIgnoreCase(circuito.getPais());
            return coincideTexto && coincidePais;
        });
        actualizarConteo();
    }

    private boolean contiene(String valor, String q) {
        return valor != null && valor.toLowerCase(Locale.ROOT).contains(q);
    }

    // --------------------------------------------------------------- orden

    private void configurarOrden() {
        ordenarPor.getItems().addAll(COMPARADORES.keySet());
        ordenarPor.setValue("NOMBRE");
        btnOrdenDireccion.setText("↑");
        btnOrdenDireccion.setOnAction(e -> {
            ordenAscendente = !ordenAscendente;
            btnOrdenDireccion.setText(ordenAscendente ? "↑" : "↓");
            aplicarOrden();
        });
    }

    private void aplicarOrden() {
        Comparator<Circuit> base = COMPARADORES.getOrDefault(ordenarPor.getValue(), COMPARADORES.get("NOMBRE"));
        ordenados.setComparator(ordenAscendente ? base : base.reversed());
    }

    // --------------------------------------------------------------- tabla

    private void configurarTabla() {
        colNombre.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().getNombre()));
        colPais.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().getPais()));
        colLongitud.setCellValueFactory(f -> new SimpleDoubleProperty(f.getValue().getLongitudKm()));
        colVueltas.setCellValueFactory(f -> new SimpleIntegerProperty(f.getValue().getVueltas()));
        colRecord.setCellValueFactory(f -> new SimpleStringProperty(
                f.getValue().getRecordVuelta() == null ? "—"
                        : FormatUtils.formatLapTime(f.getValue().getRecordVuelta().getTiempoSegundos())));
        colFactor.setCellValueFactory(f -> new SimpleDoubleProperty(f.getValue().getFactorTecnico()));
    }

    // ---------------------------------------------------------------- datos

    private void cargarDatos() {
        datos.setAll(circuitos.listar());
        actualizarPaises();
        aplicarFiltros();
    }

    /** Vuelve a consultar el catálogo, preservando la selección actual si sigue existiendo. */
    void refrescarVista() {
        Circuit seleccionado = tabla.getSelectionModel().getSelectedItem();
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

    private void mostrarDetalle(Circuit circuito) {
        detalleVacio.setVisible(false);
        detalleVacio.setManaged(false);
        detalleScroll.setVisible(true);
        detalleScroll.setManaged(true);
        detalleContenido.setVisible(true);
        detalleContenido.setManaged(true);

        pintarFoto(circuito);
        detalleNombre.setText(circuito.getNombre() == null ? "" : circuito.getNombre().toUpperCase(Locale.ROOT));
        detalleSubtitulo.setText(valorOTexto(circuito.getPais()) + "  ·  " + circuito.getVueltas() + " vueltas");

        pintarResumen(circuito);
        pintarHistorial(circuito);
    }

    private void pintarFoto(Circuit circuito) {
        detalleFoto.getChildren().clear();
        detalleFoto.setMinSize(DETALLE_ANCHO_ARTE, DETALLE_ALTO_ARTE);
        detalleFoto.setPrefSize(DETALLE_ANCHO_ARTE, DETALLE_ALTO_ARTE);
        detalleFoto.setMaxSize(DETALLE_ANCHO_ARTE, DETALLE_ALTO_ARTE);

        Image imagen = Imagenes.cargar(circuito.getImagen(), DETALLE_ANCHO_ARTE * 2, 0);
        if (imagen != null) {
            ImageView vista = ImageCrop.encajar(imagen, DETALLE_ANCHO_ARTE, DETALLE_ALTO_ARTE, ImageCrop.CENTRADO);
            detalleFoto.getChildren().add(vista);
        } else {
            Label iniciales = new Label(Iniciales.de(circuito.getNombre()));
            iniciales.getStyleClass().add("mgmt-watermark");
            detalleFoto.getChildren().add(iniciales);
        }
    }

    private void pintarResumen(Circuit circuito) {
        String record = circuito.getRecordVuelta() == null ? "Sin récord registrado"
                : FormatUtils.formatLapTime(circuito.getRecordVuelta().getTiempoSegundos())
                        + " — " + circuito.getRecordVuelta().getPiloto()
                        + " (" + circuito.getRecordVuelta().getAnio() + ")";

        tabResumen.getChildren().setAll(
                filaInfo("LONGITUD", String.format(Locale.ROOT, "%.3f km", circuito.getLongitudKm())),
                filaInfo("VUELTAS", String.valueOf(circuito.getVueltas())),
                filaInfo("FACTOR TÉCNICO", String.format(Locale.ROOT, "%.2f", circuito.getFactorTecnico())),
                filaInfo("FACTOR CONSUMO", String.format(Locale.ROOT, "%.2f", circuito.getFactorConsumo())),
                filaInfo("FACTOR DESGASTE", String.format(Locale.ROOT, "%.2f", circuito.getFactorDesgaste())),
                filaInfoLarga("RÉCORD DE VUELTA", record),
                filaInfoLarga("DESCRIPCIÓN", valorOTexto(circuito.getDescripcion())));
    }

    private void pintarHistorial(Circuit circuito) {
        List<String> ganadores = circuitos.ganadoresDe(circuito);
        VBox lista = new VBox(4);
        if (ganadores.isEmpty()) {
            Label vacio = new Label("Sin ganadores registrados.");
            vacio.getStyleClass().add("hint");
            lista.getChildren().add(vacio);
        } else {
            for (String ganador : ganadores) {
                Label fila = new Label(ganador);
                fila.getStyleClass().add("mgmt-info-value");
                lista.getChildren().add(fila);
            }
        }

        Button verFicha = new Button("VER FICHA COMPLETA →");
        verFicha.getStyleClass().add("icon-button");
        verFicha.setOnAction(e -> {
            Navigator.irConRetorno("circuit-detail");
            if (Navigator.ultimoControlador() instanceof CircuitDetailController detalle) {
                detalle.mostrar(circuito.getNombre());
            }
        });

        tabHistorial.getChildren().setAll(lista, verFicha);
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
        Forms.circuito(null).ifPresent(this::guardar);
    }

    private void onEditar() {
        Circuit actual = tabla.getSelectionModel().getSelectedItem();
        if (actual == null) {
            return;
        }
        Forms.circuito(actual).ifPresent(this::guardar);
    }

    private void onEliminar() {
        Circuit actual = tabla.getSelectionModel().getSelectedItem();
        if (actual == null) {
            return;
        }
        if (Navigator.confirmar("¿Eliminar " + actual.getNombre() + "?")) {
            circuitos.eliminar(actual.getNombre());
            cargarDatos();
        }
    }

    private void guardar(Circuit circuito) {
        try {
            circuitos.guardar(circuito);
            cargarDatos();
            tabla.getSelectionModel().select(circuito);
        } catch (ValidationException e) {
            Navigator.error("Datos no válidos", e.getMessage());
        }
    }
}
