package com.formula1.controller;

import com.formula1.model.Driver;
import com.formula1.model.LapResult;
import com.formula1.model.QualifyingSession;
import com.formula1.model.Vehicle;
import com.formula1.service.DriverService;
import com.formula1.service.QualifyingService;
import com.formula1.service.VehicleService;
import com.formula1.util.FormatUtils;
import com.formula1.util.ImageCrop;
import com.formula1.util.TeamColors;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.StackPane;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Ficha completa de un piloto.
 *
 * Sigue el mismo patrón visual que la ficha de circuito —título, fila de
 * cifras y paneles con antetítulo— para que las dos se lean igual. Aparte de
 * los datos de catálogo, resume cómo le ha ido al piloto en las sesiones ya
 * disputadas, que es información que hasta ahora no se veía en ningún sitio.
 */
public class DriverDetailController {

    private static final double ANCHO_FOTO = 210;
    private static final double ALTO_FOTO = 240;

    @FXML private Label lblNombre;
    @FXML private Label lblDorsal;
    @FXML private Label lblRol;
    @FXML private Label lblEquipo;
    @FXML private Label lblNacionalidad;
    @FXML private StackPane marcoFoto;

    @FXML private Label lblVictorias;
    @FXML private Label lblCampeonatos;
    @FXML private Label lblExperiencia;
    @FXML private Label lblVehiculo;

    @FXML private Label lblVelocidad;
    @FXML private Label lblConsistencia;
    @FXML private Label lblLluvia;
    @FXML private ProgressBar barraVelocidad;
    @FXML private ProgressBar barraConsistencia;
    @FXML private ProgressBar barraLluvia;

    @FXML private Label lblResumenSesiones;
    @FXML private Label lblSesiones;
    @FXML private Label lblMejorPosicion;
    @FXML private Label lblPosicionMedia;
    @FXML private Label lblMejorVuelta;

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
        String color = TeamColors.hex(piloto.getEquipo());

        lblNombre.setText(piloto.getNombre());
        lblDorsal.setText("#" + piloto.getNumero());
        lblDorsal.setStyle("-fx-background-color: " + color + ";");
        lblRol.setText(piloto.getRol() == null ? "" : piloto.getRol().getEtiqueta().toUpperCase(Locale.ROOT));
        lblRol.getStyleClass().removeAll("lider", "escudero");
        lblRol.getStyleClass().add(piloto.getRol() == null
                ? "escudero" : piloto.getRol().name().toLowerCase(Locale.ROOT));

        lblEquipo.setText(piloto.getEquipo() == null ? "" : piloto.getEquipo().toUpperCase(Locale.ROOT));
        lblEquipo.setStyle("-fx-text-fill: " + color + ";");
        lblNacionalidad.setText(piloto.getNacionalidad() == null ? "" : piloto.getNacionalidad());

        mostrarFoto(piloto, color);

        lblVictorias.setText(String.valueOf(piloto.getVictorias()));
        lblCampeonatos.setText(String.valueOf(piloto.getCampeonatos()));
        lblExperiencia.setText(String.valueOf(piloto.getExperiencia()));
        lblVehiculo.setText(vehiculos.listar().stream()
                .filter(v -> v.conduce(piloto.getId()))
                .map(Vehicle::getModelo)
                .findFirst()
                .orElse("—"));

        habilidad(piloto, Driver.HABILIDAD_VELOCIDAD, lblVelocidad, barraVelocidad);
        habilidad(piloto, Driver.HABILIDAD_CONSISTENCIA, lblConsistencia, barraConsistencia);
        habilidad(piloto, Driver.HABILIDAD_LLUVIA, lblLluvia, barraLluvia);

        mostrarHistorial(piloto);
    }

    private void mostrarFoto(Driver piloto, String color) {
        marcoFoto.getChildren().clear();
        marcoFoto.setPrefSize(ANCHO_FOTO, ALTO_FOTO);
        marcoFoto.setMinSize(ANCHO_FOTO, ALTO_FOTO);
        marcoFoto.setMaxSize(ANCHO_FOTO, ALTO_FOTO);
        marcoFoto.setStyle("-fx-border-color: " + color + ";");

        ImageCrop.desdeClasspath(piloto.getImagen(), ANCHO_FOTO, ALTO_FOTO, ImageCrop.SESGO_RETRATO)
                .ifPresentOrElse(marcoFoto.getChildren()::add, () -> {
                    Label inicial = new Label(piloto.getNombre().substring(0, 1));
                    inicial.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 60px; -fx-font-weight: bold;");
                    marcoFoto.getChildren().add(inicial);
                });
    }

    private void habilidad(Driver piloto, String clave, Label etiqueta, ProgressBar barra) {
        int valor = piloto.getHabilidad(clave);
        etiqueta.setText(valor + " / 100");
        barra.setProgress(valor / 100.0);
    }

    /**
     * Recorre las sesiones guardadas buscando la vuelta de este piloto. No hay
     * ningún resumen almacenado, así que se calcula al vuelo.
     */
    private void mostrarHistorial(Driver piloto) {
        List<Fila> filas = sesiones.historial().stream()
                .flatMap(s -> vueltaDe(s, piloto).stream())
                .toList();

        tablaSesiones.setItems(FXCollections.observableArrayList(filas));
        lblSesiones.setText(String.valueOf(filas.size()));

        if (filas.isEmpty()) {
            lblResumenSesiones.setText("Todavía no ha salido a pista en ninguna clasificación guardada.");
            lblMejorPosicion.setText("—");
            lblPosicionMedia.setText("—");
            lblMejorVuelta.setText("—");
            return;
        }

        var validas = filas.stream().filter(f -> f.segundos() > 0).toList();
        lblResumenSesiones.setText("Resultados de " + piloto.getNombre()
                + " en las clasificaciones ya disputadas.");
        lblMejorPosicion.setText("P" + filas.stream().mapToInt(Fila::orden).min().orElse(0));
        lblPosicionMedia.setText(String.format(Locale.ROOT, "%.1f",
                filas.stream().mapToInt(Fila::orden).average().orElse(0)));
        lblMejorVuelta.setText(validas.isEmpty() ? "—"
                : FormatUtils.formatLapTime(validas.stream()
                        .min(Comparator.comparingDouble(Fila::segundos))
                        .orElseThrow().segundos()));
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

    /** Fila de la tabla; se expone como record para las cell factories. */
    public record Fila(String fecha, String circuito, String posicion, String tiempo,
                       String gap, String estado, int orden, double segundos) {
    }
}
