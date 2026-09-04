package com.formula1.adapter.in.javafx;

import com.formula1.adapter.out.memory.DataStore;
import com.formula1.application.port.out.PreparedConfigPort;
import com.formula1.domain.model.AerodynamicLoad;
import com.formula1.domain.model.Circuit;
import com.formula1.domain.model.DrivingMode;
import com.formula1.domain.model.FuelStrategy;
import com.formula1.domain.model.SimulationConfig;
import com.formula1.domain.model.TireCompound;
import com.formula1.domain.model.TirePressure;
import com.formula1.domain.model.Vehicle;
import com.formula1.domain.model.WeatherCondition;
import com.formula1.application.usecase.CircuitService;
import com.formula1.domain.service.LapTimeCalculator;
import com.formula1.application.usecase.VehicleService;
import com.formula1.util.FormatUtils;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * Puesta a punto del monoplaza: modo de conducción, carga aerodinámica,
 * presión y compuesto inicial de neumáticos, y estrategia de combustible.
 *
 * El panel lateral no es decorativo: consumo y desgaste salen de
 * {@link LapTimeCalculator}, que expone ambos cálculos de forma pura y
 * determinista, así que se pueden recalcular en cada clic sin simular.
 */
public class ConfigController {

    /** Colores de resalte del selector segmentado, según el diseño. */
    private static final String AZUL = "active-blue";
    private static final String AMBAR = "active-amber";

    @FXML private HBox segModo;
    @FXML private HBox segAero;
    @FXML private HBox segPresion;
    @FXML private HBox segCompuesto;
    @FXML private HBox segCombustible;
    @FXML private Label lblNotaModo;
    @FXML private Label lblNotaCombustible;

    @FXML private Label lblAeroTiempo;
    @FXML private Label lblAeroConsumo;
    @FXML private Label lblAeroDesgaste;
    @FXML private Label lblPresionTiempo;
    @FXML private Label lblPresionDesgaste;

    @FXML private Label lblVelocidad;
    @FXML private Label lblDesgaste;
    @FXML private Label lblConsumo;
    @FXML private Label lblTiempo;
    @FXML private ProgressBar barraVelocidad;
    @FXML private ProgressBar barraDesgaste;

    @FXML private Label lblResumenModo;
    @FXML private Label lblResumenAero;
    @FXML private Label lblResumenPresion;
    @FXML private Label lblResumenCompuesto;
    @FXML private Label lblResumenCombustible;
    @FXML private Label lblContexto;

    private final VehicleService vehiculos;
    private final CircuitService circuitos;
    private final PreparedConfigPort configuracionPreparada;
    private final LapTimeCalculator calculadora = new LapTimeCalculator();

    private DrivingMode modo = DrivingMode.NORMAL;
    private AerodynamicLoad aero = AerodynamicLoad.MEDIA;
    private TirePressure presion = TirePressure.ESTANDAR;
    private TireCompound compuestoInicial = TireCompound.MEDIUM;
    private FuelStrategy combustible = FuelStrategy.BALANCEADA;
    private int duracionSegundos = SimulationConfig.DURACION_PREDETERMINADA_SEGUNDOS;

    public ConfigController() {
        this(new VehicleService(), new CircuitService(), DataStore.getInstance());
    }

    public ConfigController(VehicleService vehiculos, CircuitService circuitos,
                            PreparedConfigPort configuracionPreparada) {
        this.vehiculos = vehiculos;
        this.circuitos = circuitos;
        this.configuracionPreparada = configuracionPreparada;
    }

    @FXML
    public void initialize() {
        recuperarConfiguracion();

        construir(segModo, DrivingMode.values(), AZUL, v -> {
            modo = (DrivingMode) v;
            refrescar();
        }, () -> modo);
        construir(segAero, AerodynamicLoad.values(), AZUL, v -> {
            aero = (AerodynamicLoad) v;
            refrescar();
        }, () -> aero);
        construir(segPresion, TirePressure.values(), AMBAR, v -> {
            presion = (TirePressure) v;
            refrescar();
        }, () -> presion);
        construir(segCompuesto, TireCompound.values(), AMBAR, v -> {
            compuestoInicial = (TireCompound) v;
            refrescar();
        }, () -> compuestoInicial);
        construir(segCombustible, FuelStrategy.values(), AMBAR, v -> {
            combustible = (FuelStrategy) v;
            refrescar();
        }, () -> combustible);

        refrescar();
    }

    /** Arranca desde lo último que el usuario dejó preparado o simuló. */
    private void recuperarConfiguracion() {
        SimulationConfig previa = configuracionPreparada.configuracionActual();
        if (previa == null) {
            previa = new com.formula1.application.usecase.QualifyingService().historial().stream()
                    .map(s -> s.getConfig())
                    .filter(c -> c != null)
                    .findFirst()
                    .orElse(null);
        }
        if (previa == null) {
            return;
        }
        modo = previa.getModo() == null ? modo : previa.getModo();
        aero = previa.getAerodinamica() == null ? aero : previa.getAerodinamica();
        presion = previa.getPresion() == null ? presion : previa.getPresion();
        compuestoInicial = previa.getCompuestoInicial();
        combustible = previa.getCombustible() == null ? combustible : previa.getCombustible();
        duracionSegundos = previa.getDuracionSegundos();
    }

    /**
     * Construye un selector segmentado: un botón por constante del enum, con
     * el activo resaltado en el color que le corresponde en el diseño.
     */
    private void construir(HBox contenedor, Object[] opciones, String estiloActivo,
                           Consumer<Object> alElegir, java.util.function.Supplier<Object> actual) {
        contenedor.getChildren().clear();
        for (Object opcion : opciones) {
            Button boton = new Button(etiquetaDe(opcion).toUpperCase(Locale.ROOT));
            boton.getStyleClass().add("segment");
            boton.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(boton, Priority.ALWAYS);
            boton.setOnAction(e -> {
                alElegir.accept(opcion);
                for (var nodo : contenedor.getChildren()) {
                    nodo.getStyleClass().removeAll(AZUL, AMBAR);
                }
                boton.getStyleClass().add(estiloActivo);
            });
            if (opcion.equals(actual.get())) {
                boton.getStyleClass().add(estiloActivo);
            }
            contenedor.getChildren().add(boton);
        }
    }

    private String etiquetaDe(Object opcion) {
        if (opcion instanceof DrivingMode m) return m.getEtiqueta();
        if (opcion instanceof AerodynamicLoad a) return a.getEtiqueta();
        if (opcion instanceof TirePressure p) return p.getEtiqueta();
        if (opcion instanceof TireCompound c) return c.getEtiqueta();
        if (opcion instanceof FuelStrategy f) return f.getEtiqueta();
        return String.valueOf(opcion);
    }

    /** Recalcula el panel de impacto con los ajustes elegidos. */
    private void refrescar() {
        lblNotaModo.setText(switch (modo) {
            case NORMAL -> "Equilibrio entre velocidad y desgaste. El ajuste de referencia.";
            case AGRESIVA -> "Máximo ritmo a costa de un consumo y un desgaste notablemente mayores.";
            case AHORRO -> "Preserva neumáticos y combustible sacrificando velocidad punta.";
        });
        lblNotaCombustible.setText(switch (combustible) {
            case AGRESIVA -> "Mezcla rica: mejora el tiempo un 1 % y dispara el consumo un 15 %.";
            case BALANCEADA -> "Distribución óptima. Mantiene el rendimiento sin penalizar el consumo.";
            case AHORRO -> "Ahorra un 15 % de combustible a cambio de un 1 % de tiempo.";
        });

        lblAeroTiempo.setText(porcentaje(aero.getFactorTiempo()));
        lblAeroConsumo.setText(porcentaje(aero.getFactorConsumo()));
        lblAeroDesgaste.setText(porcentaje(aero.getFactorDesgaste()));
        lblPresionTiempo.setText(porcentaje(presion.getFactorTiempo()));
        lblPresionDesgaste.setText(porcentaje(presion.getFactorDesgaste()));

        lblResumenModo.setText(modo.getEtiqueta());
        lblResumenAero.setText(aero.getEtiqueta());
        lblResumenPresion.setText(presion.getEtiqueta());
        lblResumenCompuesto.setText(compuestoInicial.toString());
        lblResumenCombustible.setText(combustible.getEtiqueta());

        estimarImpacto();
    }

    /**
     * Usa un vehículo y un circuito de referencia para que las cifras del
     * panel sean reales; si no hay datos cargados, se deja en blanco en vez
     * de inventar números.
     */
    private void estimarImpacto() {
        Vehicle vehiculo = referencia(vehiculos.listar());
        Circuit circuito = referencia(circuitos.listar());
        if (vehiculo == null || circuito == null) {
            lblContexto.setText("Sin datos cargados todavía.");
            return;
        }

        SimulationConfig config = configuracion();
        double consumo = calculadora.consumoPorVuelta(vehiculo, circuito, WeatherCondition.SECO, config);
        double desgaste = calculadora.desgastePorVuelta(
                vehiculo, circuito, WeatherCondition.SECO, config)
                * compuestoInicial.getFactorDesgaste();
        int velocidad = vehiculo.rendimientoDe(modo).getVelocidadPromedioKmh();

        // Tiempo base del circuito ajustado por los factores elegidos: la
        // misma fórmula del motor, sin el ruido aleatorio del simulador.
        double base = 3600.0 * circuito.getLongitudKm() / velocidad;
        double tiempo = base * circuito.getFactorTecnico()
                * aero.getFactorTiempo() * presion.getFactorTiempo()
                * combustible.getFactorTiempo() * compuestoInicial.getFactorTiempo();

        lblVelocidad.setText(String.valueOf(velocidad));
        lblConsumo.setText(String.format(Locale.ROOT, "%.2f", consumo));
        lblDesgaste.setText(String.format(Locale.ROOT, "%.2f", desgaste));
        lblTiempo.setText(FormatUtils.formatLapTime(tiempo));

        barraVelocidad.setProgress(Math.min(1, velocidad / (double) vehiculo.getVelocidadMaximaKmh()));
        barraDesgaste.setProgress(Math.min(1, desgaste / 4.0));

        lblContexto.setText("Estimación sobre " + vehiculo.getModelo() + " en "
                + circuito.getNombre() + ", en seco.");
    }

    /** Un factor de 1.08 se lee mejor como «+8 %» que como el número crudo. */
    private String porcentaje(double factor) {
        double delta = (factor - 1.0) * 100.0;
        if (Math.abs(delta) < 0.05) {
            return "—";
        }
        return String.format(Locale.ROOT, "%+.1f%%", delta);
    }

    private <T> T referencia(java.util.List<T> lista) {
        return lista.isEmpty() ? null : lista.get(0);
    }

    private SimulationConfig configuracion() {
        SimulationConfig previa = configuracionPreparada.configuracionActual();
        SimulationConfig config = new SimulationConfig();
        if (previa != null) {
            config.setCircuito(previa.getCircuito());
            config.setVehiculo(previa.getVehiculo());
            config.setPilotoId(previa.getPilotoId());
        }
        config.setModo(modo);
        config.setAerodinamica(aero);
        config.setPresion(presion);
        config.setCompuestoInicial(compuestoInicial);
        config.setCombustible(combustible);
        config.setDuracionSegundos(duracionSegundos);
        return config;
    }

    @FXML
    private void onGuardar() {
        configuracionPreparada.guardarConfiguracion(configuracion());
        lblContexto.setText("Configuración guardada. Carrera la aplicará al volver, sin iniciar la sesión.");
    }
}
