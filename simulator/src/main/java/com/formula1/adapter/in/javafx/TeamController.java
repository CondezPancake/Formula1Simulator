package com.formula1.adapter.in.javafx;

import com.formula1.domain.model.Driver;
import com.formula1.domain.model.Team;
import com.formula1.application.usecase.TeamService;
import com.formula1.domain.service.ValidationException;
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
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
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
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

/**
 * Gestión de equipos: tabla filtrable con una ficha lateral de detalle,
 * edición y baja. El alta, la edición y la baja siguen pasando por
 * {@link Forms#equipo} y {@link TeamService}.
 */
public class TeamController {

    private static final String TODOS_PAISES = "PAÍS";
    private static final double DETALLE_ANCHO_ARTE = 330;
    private static final double DETALLE_ALTO_ARTE = 220;

    private static final Map<String, Comparator<Team>> COMPARADORES = new LinkedHashMap<>();
    static {
        COMPARADORES.put("NOMBRE", Comparator.comparing(Team::getNombre, String.CASE_INSENSITIVE_ORDER));
        COMPARADORES.put("PAÍS", Comparator.comparing(t -> t.getPais() == null ? "" : t.getPais(), String.CASE_INSENSITIVE_ORDER));
        COMPARADORES.put("CAMPEONATOS", Comparator.comparingInt(Team::getCampeonatos));
        COMPARADORES.put("VICTORIAS", Comparator.comparingInt(Team::getVictorias));
        COMPARADORES.put("PODIOS", Comparator.comparingInt(Team::getPodios));
        COMPARADORES.put("POLES", Comparator.comparingInt(Team::getPoles));
    }

    @FXML private Label lblConteo;
    @FXML private TextField buscador;
    @FXML private ComboBox<String> filtroPais;
    @FXML private ComboBox<String> ordenarPor;
    @FXML private Button btnOrdenDireccion;
    @FXML private Button btnNuevo;

    @FXML private TableView<Team> tabla;
    @FXML private TableColumn<Team, String> colNombre;
    @FXML private TableColumn<Team, String> colPais;
    @FXML private TableColumn<Team, String> colMotor;
    @FXML private TableColumn<Team, Number> colNumPilotos;
    @FXML private TableColumn<Team, Number> colCampeonatos;
    @FXML private TableColumn<Team, Number> colVictorias;

    @FXML private StackPane panelDetalle;
    @FXML private VBox detalleVacio;
    @FXML private ScrollPane detalleScroll;
    @FXML private VBox detalleContenido;
    @FXML private StackPane detalleFoto;
    @FXML private Label detalleNombre;
    @FXML private Label detalleSubtitulo;
    @FXML private VBox tabResumen;
    @FXML private VBox tabEstadisticas;
    @FXML private Button btnEditar;
    @FXML private Button btnEliminar;

    private final TeamService equipos;

    private final ObservableList<Team> datos = FXCollections.observableArrayList();
    private FilteredList<Team> filtrados;
    private SortedList<Team> ordenados;
    private boolean ordenAscendente = true;

    public TeamController() {
        this(new TeamService());
    }

    public TeamController(TeamService equipos) {
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

        filtrados = new FilteredList<>(datos, t -> true);
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
        Label titulo = new Label("NO SE ENCONTRARON EQUIPOS");
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
                .map(Team::getPais)
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
        filtrados.setPredicate(equipo -> {
            boolean coincideTexto = texto.isEmpty()
                    || contiene(equipo.getNombre(), texto)
                    || contiene(equipo.getPais(), texto)
                    || contiene(equipo.getMotor(), texto);
            boolean coincidePais = pais == null || TODOS_PAISES.equals(pais) || pais.equalsIgnoreCase(equipo.getPais());
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
        Comparator<Team> base = COMPARADORES.getOrDefault(ordenarPor.getValue(), COMPARADORES.get("NOMBRE"));
        ordenados.setComparator(ordenAscendente ? base : base.reversed());
    }

    // --------------------------------------------------------------- tabla

    private void configurarTabla() {
        colNombre.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().getNombre()));
        colPais.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().getPais()));
        colMotor.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().getMotor()));
        colNumPilotos.setCellValueFactory(f -> new SimpleIntegerProperty(f.getValue().getPilotos().size()));
        colCampeonatos.setCellValueFactory(f -> new SimpleIntegerProperty(f.getValue().getCampeonatos()));
        colVictorias.setCellValueFactory(f -> new SimpleIntegerProperty(f.getValue().getVictorias()));
    }

    // ---------------------------------------------------------------- datos

    private void cargarDatos() {
        datos.setAll(equipos.listar());
        actualizarPaises();
        aplicarFiltros();
    }

    /** Vuelve a consultar el catálogo, preservando la selección actual si sigue existiendo. */
    void refrescarVista() {
        Team seleccionado = tabla.getSelectionModel().getSelectedItem();
        cargarDatos();
        if (seleccionado != null) {
            tabla.getSelectionModel().select(seleccionado);
            mostrarDetalle(seleccionado);
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

    private void mostrarDetalle(Team equipo) {
        detalleVacio.setVisible(false);
        detalleVacio.setManaged(false);
        detalleScroll.setVisible(true);
        detalleScroll.setManaged(true);
        detalleContenido.setVisible(true);
        detalleContenido.setManaged(true);

        pintarLogo(equipo);
        detalleNombre.setText(equipo.getNombre() == null ? "" : equipo.getNombre().toUpperCase(Locale.ROOT));
        detalleSubtitulo.setText(valorOTexto(equipo.getPais()) + "  ·  Motor " + valorOTexto(equipo.getMotor()));

        pintarResumen(equipo);
        pintarEstadisticas(equipo);
    }

    /** Livery del equipo con su logo centrado; si no hay logo, sus iniciales. */
    private void pintarLogo(Team equipo) {
        detalleFoto.getChildren().clear();
        detalleFoto.setMinSize(DETALLE_ANCHO_ARTE, DETALLE_ALTO_ARTE);
        detalleFoto.setPrefSize(DETALLE_ANCHO_ARTE, DETALLE_ALTO_ARTE);
        detalleFoto.setMaxSize(DETALLE_ANCHO_ARTE, DETALLE_ALTO_ARTE);

        Region base = new Region();
        base.setStyle("-fx-background-color: " + TeamColors.accesible(equipo.getNombre()) + ";");
        detalleFoto.getChildren().add(base);

        Image logo = Imagenes.cargar(F1Assets.logo(equipo.getNombre()), 0, 140);
        if (logo != null) {
            ImageView vista = new ImageView(logo);
            vista.setFitHeight(120);
            vista.setPreserveRatio(true);
            vista.setSmooth(true);
            detalleFoto.getChildren().add(vista);
        } else {
            Label iniciales = new Label(Iniciales.de(equipo.getNombre()));
            iniciales.getStyleClass().add("mgmt-watermark");
            detalleFoto.getChildren().add(iniciales);
        }
    }

    private void pintarResumen(Team equipo) {
        List<Driver> pilotos = equipos.pilotosDe(equipo);
        String nombresPilotos = pilotos.isEmpty() ? "—"
                : pilotos.stream().map(Driver::getNombre).collect(Collectors.joining(", "));

        tabResumen.getChildren().setAll(
                filaInfo("NOMBRE COMPLETO", valorOTexto(equipo.getNombreCompleto())),
                filaInfo("BASE", valorOTexto(equipo.getBase())),
                filaInfo("JEFE DE EQUIPO", valorOTexto(equipo.getJefeEquipo())),
                filaInfo("JEFE TÉCNICO", valorOTexto(equipo.getJefeTecnico())),
                filaInfo("PILOTO RESERVA", valorOTexto(equipo.getPilotoReserva())),
                filaInfo("PRIMERA PARTICIPACIÓN",
                        equipo.getPrimeraParticipacion() > 0 ? String.valueOf(equipo.getPrimeraParticipacion()) : "—"),
                filaInfoLarga("PILOTOS", nombresPilotos));
    }

    /**
     * A diferencia de Resumen (los datos de ficha), aquí se compara con el
     * resto de escuderías cargadas: la posición real en cada palmarés, con
     * una barra proporcional al mejor valor de la parrilla.
     */
    private void pintarEstadisticas(Team equipo) {
        int total = datos.size();
        tabEstadisticas.getChildren().setAll(
                filaRanking("CAMPEONATOS", equipo.getCampeonatos(), Team::getCampeonatos, equipo, total),
                filaRanking("VICTORIAS", equipo.getVictorias(), Team::getVictorias, equipo, total),
                filaRanking("PODIOS", equipo.getPodios(), Team::getPodios, equipo, total),
                filaRanking("POLES", equipo.getPoles(), Team::getPoles, equipo, total),
                filaRanking("GRANDES PREMIOS", equipo.getGranPremios(), Team::getGranPremios, equipo, total));
    }

    private VBox filaRanking(String etiqueta, int valor, ToIntFunction<Team> metrica, Team equipo, int total) {
        int mejor = datos.stream().mapToInt(metrica).max().orElse(0);
        int puesto = (int) datos.stream().filter(t -> metrica.applyAsInt(t) > valor).count() + 1;

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

        ProgressBar barra = new ProgressBar(mejor <= 0 ? 0 : (double) valor / mejor);
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

    /** Como {@link #filaInfo}, pero el valor va debajo y con ajuste de línea. */
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
        Forms.equipo(null).ifPresent(this::guardar);
    }

    private void onEditar() {
        Team actual = tabla.getSelectionModel().getSelectedItem();
        if (actual == null) {
            return;
        }
        Forms.equipo(actual).ifPresent(this::guardar);
    }

    private void onEliminar() {
        Team actual = tabla.getSelectionModel().getSelectedItem();
        if (actual == null) {
            return;
        }
        if (!Navigator.confirmar("¿Eliminar " + actual.getNombre() + "?")) {
            return;
        }
        try {
            equipos.eliminar(actual.getNombre());
            cargarDatos();
        } catch (ValidationException e) {
            Navigator.error("No se puede eliminar", e.getMessage());
        }
    }

    private void guardar(Team equipo) {
        try {
            equipos.guardar(equipo);
            cargarDatos();
            tabla.getSelectionModel().select(equipo);
        } catch (ValidationException e) {
            Navigator.error("Datos no válidos", e.getMessage());
        }
    }
}
