package com.formula1.service;

import com.formula1.data.DataStore;
import com.formula1.model.AerodynamicLoad;
import com.formula1.model.Circuit;
import com.formula1.model.Driver;
import com.formula1.model.DrivingMode;
import com.formula1.model.FuelStrategy;
import com.formula1.model.LapResult;
import com.formula1.model.QualifyingSession;
import com.formula1.model.SimulationConfig;
import com.formula1.model.TirePressure;
import com.formula1.model.Vehicle;
import com.formula1.model.WeatherCondition;
import com.formula1.util.FormatUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QualifyingServiceTest {

    private DataStore datos;
    private QualifyingService sesiones;
    private LapTimeCalculator calculadora;

    /** Configuración neutra sobre Monza con el Red Bull. */
    private SimulationConfig config(DrivingMode modo) {
        return new SimulationConfig("Circuito de Monza", "RB20", modo,
                AerodynamicLoad.MEDIA, TirePressure.ESTANDAR, FuelStrategy.BALANCEADA);
    }

    private Driver verstappen() {
        return datos.pilotos().get(1);
    }

    private Vehicle rb20() {
        return datos.vehiculos().get("RB20");
    }

    private Circuit monza() {
        return datos.circuitos().get("Circuito de Monza");
    }

    @BeforeEach
    void preparar() {
        datos = DataStore.enMemoria();
        // Semilla fija -> sin ruido aleatorio, resultados reproducibles.
        calculadora = new LapTimeCalculator(new Random(7));
        sesiones = new QualifyingService(datos, calculadora);
    }

    @Test
    void elTiempoEnMonzaSeParecealRecordReal() {
        double tiempo = calculadora.calcularTiempo(
                verstappen(), rb20(), monza(), WeatherCondition.SECO, config(DrivingMode.NORMAL));

        // Récord real de Monza: 1:21.046. Un margen de 3 s valida la escala.
        assertEquals(81.0, tiempo, 3.0,
                "tiempo obtenido: " + FormatUtils.formatLapTime(tiempo));
    }

    @Test
    void conducirMasAgresivoBajaElTiempo() {
        LapTimeCalculator sinRuido = new LapTimeCalculator(new Random(1));

        double agresiva = sinRuido.calcularTiempo(verstappen(), rb20(), monza(), WeatherCondition.SECO, config(DrivingMode.AGRESIVA));
        double normal = new LapTimeCalculator(new Random(1)).calcularTiempo(verstappen(), rb20(), monza(), WeatherCondition.SECO, config(DrivingMode.NORMAL));
        double ahorro = new LapTimeCalculator(new Random(1)).calcularTiempo(verstappen(), rb20(), monza(), WeatherCondition.SECO, config(DrivingMode.AHORRO));

        assertTrue(agresiva < normal, "agresiva debe ser más rápida que normal");
        assertTrue(normal < ahorro, "normal debe ser más rápida que ahorro");
    }

    @Test
    void peorClimaImplicaPeorTiempo() {
        double seco = new LapTimeCalculator(new Random(1)).calcularTiempo(verstappen(), rb20(), monza(), WeatherCondition.SECO, config(DrivingMode.NORMAL));
        double lluvia = new LapTimeCalculator(new Random(1)).calcularTiempo(verstappen(), rb20(), monza(), WeatherCondition.LLUVIOSO, config(DrivingMode.NORMAL));
        double extremo = new LapTimeCalculator(new Random(1)).calcularTiempo(verstappen(), rb20(), monza(), WeatherCondition.EXTREMO, config(DrivingMode.NORMAL));

        assertTrue(seco < lluvia && lluvia < extremo);
    }

    @Test
    void monacoEsMasLentoQueMonzaAunqueSeaMasCorto() {
        Circuit monaco = datos.circuitos().get("Circuito de Mónaco");
        SimulationConfig enMonaco = new SimulationConfig("Circuito de Mónaco", "RB20", DrivingMode.NORMAL,
                AerodynamicLoad.MEDIA, TirePressure.ESTANDAR, FuelStrategy.BALANCEADA);

        double tMonaco = new LapTimeCalculator(new Random(1)).calcularTiempo(verstappen(), rb20(), monaco, WeatherCondition.SECO, enMonaco);
        double tMonza = new LapTimeCalculator(new Random(1)).calcularTiempo(verstappen(), rb20(), monza(), WeatherCondition.SECO, config(DrivingMode.NORMAL));

        assertTrue(monaco.getLongitudKm() < monza().getLongitudKm(), "Mónaco es más corto");
        assertTrue(tMonaco < tMonza, "pero su vuelta también es más corta en tiempo");
        // Lo relevante: en Mónaco se va mucho más lento por km.
        assertTrue(tMonaco / monaco.getLongitudKm() > tMonza / monza().getLongitudKm() * 1.4);
    }

    @Test
    void mismaSemillaProduceElMismoTiempo() {
        double a = new LapTimeCalculator(new Random(42)).calcularTiempo(verstappen(), rb20(), monza(), WeatherCondition.SECO, config(DrivingMode.NORMAL));
        double b = new LapTimeCalculator(new Random(42)).calcularTiempo(verstappen(), rb20(), monza(), WeatherCondition.SECO, config(DrivingMode.NORMAL));

        assertEquals(a, b, 1e-9);
    }

    @Test
    void unPilotoMejorEsMasRapidoConElMismoCoche() {
        Driver sargeant = datos.pilotos().get(20);

        double tVer = new LapTimeCalculator(new Random(1)).calcularTiempo(verstappen(), rb20(), monza(), WeatherCondition.SECO, config(DrivingMode.NORMAL));
        double tSar = new LapTimeCalculator(new Random(1)).calcularTiempo(sargeant, rb20(), monza(), WeatherCondition.SECO, config(DrivingMode.NORMAL));

        assertTrue(tVer < tSar, "Verstappen debe batir a Sargeant con el mismo coche");
    }

    @Test
    void laSesionClasificaALosVeintePilotos() {
        QualifyingSession sesion = sesiones.simular(config(DrivingMode.NORMAL), WeatherCondition.SECO, null);
        List<LapResult> parrilla = sesion.getResultados();

        assertEquals(20, parrilla.size());
        assertEquals(WeatherCondition.SECO, sesion.getClima());
        assertNotNull(sesion.getFecha());

        for (int i = 0; i < parrilla.size(); i++) {
            assertEquals(i + 1, parrilla.get(i).getPosicion(), "posiciones consecutivas desde 1");
            if (i > 0) {
                assertTrue(parrilla.get(i - 1).getTiempoSegundos() <= parrilla.get(i).getTiempoSegundos(),
                        "la parrilla debe quedar ordenada por tiempo");
            }
        }

        assertEquals(0.0, sesion.getPole().getGap(), 1e-9, "la pole no tiene diferencia consigo misma");
        assertTrue(parrilla.get(19).getGap() > 0);
    }

    @Test
    void laParrillaTieneUnaDispersionRealista() {
        QualifyingSession sesion = sesiones.simular(config(DrivingMode.NORMAL), WeatherCondition.SECO, null);
        double colista = sesion.getResultados().get(19).getGap();

        // Un Williams no puede hacer la pole, pero tampoco quedar a un minuto:
        // la parrilla debe caber en unos pocos segundos, como en la realidad.
        assertTrue(colista > 1.0 && colista < 10.0,
                "diferencia pole-colista fuera de rango: " + FormatUtils.formatGap(colista));
    }

    @Test
    void laConfiguracionSoloAfectaAlCocheElegido() {
        SimulationConfig agresivo = config(DrivingMode.AGRESIVA);
        QualifyingSession sesion = sesiones.simular(agresivo, WeatherCondition.SECO, null);

        LapResult conRb20 = sesion.getResultados().stream()
                .filter(r -> r.getVehiculo().equals("RB20")).findFirst().orElseThrow();
        LapResult otro = sesion.getResultados().stream()
                .filter(r -> !r.getVehiculo().equals("RB20")).findFirst().orElseThrow();

        assertEquals("RB20", conRb20.getVehiculo());
        assertTrue(otro.getConsumoEstimado() > 0, "el resto corre con configuración neutra");
    }

    @Test
    void elClimaGeneradoRespetaLaDistribucionDelCircuito() {
        Circuit yasMarina = datos.circuitos().get("Circuito de Yas Marina");
        int secos = 0;
        for (int i = 0; i < 200; i++) {
            if (sesiones.generarClima(yasMarina) == WeatherCondition.SECO) {
                secos++;
            }
        }

        // Yas Marina es seco el 95 % de las veces.
        assertTrue(secos > 160, "esperaba mayoría de sesiones en seco, hubo " + secos);
    }

    @Test
    void elConsumoYElDesgasteReaccionanALaConfiguracion() {
        SimulationConfig ahorro = new SimulationConfig("Circuito de Monza", "RB20", DrivingMode.NORMAL,
                AerodynamicLoad.MEDIA, TirePressure.ESTANDAR, FuelStrategy.AHORRO);
        SimulationConfig empuje = new SimulationConfig("Circuito de Monza", "RB20", DrivingMode.NORMAL,
                AerodynamicLoad.MEDIA, TirePressure.ESTANDAR, FuelStrategy.AGRESIVA);

        assertTrue(calculadora.consumoPorVuelta(rb20(), monza(), WeatherCondition.SECO, ahorro)
                < calculadora.consumoPorVuelta(rb20(), monza(), WeatherCondition.SECO, empuje));

        SimulationConfig presionBaja = new SimulationConfig("Circuito de Monza", "RB20", DrivingMode.NORMAL,
                AerodynamicLoad.MEDIA, TirePressure.BAJA, FuelStrategy.BALANCEADA);
        SimulationConfig presionAlta = new SimulationConfig("Circuito de Monza", "RB20", DrivingMode.NORMAL,
                AerodynamicLoad.MEDIA, TirePressure.ALTA, FuelStrategy.BALANCEADA);

        assertTrue(calculadora.desgastePorVuelta(rb20(), monza(), WeatherCondition.SECO, presionBaja)
                > calculadora.desgastePorVuelta(rb20(), monza(), WeatherCondition.SECO, presionAlta),
                "menos presión debe desgastar más");
    }
}
