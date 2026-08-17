package com.formula1.service;

import com.formula1.data.DataStore;
import com.formula1.event.EventManager;
import com.formula1.event.EventCatalog;
import com.formula1.event.SimulationEvent;
import com.formula1.model.AerodynamicLoad;
import com.formula1.model.Circuit;
import com.formula1.model.Driver;
import com.formula1.model.DrivingMode;
import com.formula1.model.FuelStrategy;
import com.formula1.model.EventProbabilityConfig;
import com.formula1.model.EventType;
import com.formula1.model.LapResult;
import com.formula1.model.LapStatus;
import com.formula1.model.QualifyingSession;
import com.formula1.model.SimulationConfig;
import com.formula1.model.SimulationSnapshot;
import com.formula1.model.SessionStatistics;
import com.formula1.model.TirePressure;
import com.formula1.model.TelemetrySnapshot;
import com.formula1.model.Vehicle;
import com.formula1.model.WeatherCondition;
import com.formula1.model.WeatherSnapshot;
import com.formula1.util.FormatUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QualifyingServiceTest {

    private DataStore datos;
    private QualifyingService sesiones;
    private LapTimeCalculator calculadora;

    /** Configuración neutra sobre Monza con el Red Bull. */
    private SimulationConfig config(DrivingMode modo) {
        return new SimulationConfig("Circuito de Monza", 1, "RB20", modo,
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
        sesiones = new QualifyingService(
                datos, calculadora, new DynamicWeatherService(new Random(11)),
                new EventManager(EventProbabilityConfig.disabled(), new Random(13)));
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
        SimulationConfig enMonaco = new SimulationConfig("Circuito de Mónaco", 1, "RB20", DrivingMode.NORMAL,
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
    void cadaVueltaValidaConservaTresSectoresQueSumanElTiempoTotal() {
        QualifyingSession sesion = sesiones.simular(
                config(DrivingMode.NORMAL), WeatherCondition.SECO, null);

        for (LapResult resultado : sesion.getResultados()) {
            assertTrue(resultado.hasSectorTimes());
            assertTrue(resultado.getSectorTimes().sector1Seconds() > 0);
            assertTrue(resultado.getSectorTimes().sector2Seconds() > 0);
            assertTrue(resultado.getSectorTimes().sector3Seconds() > 0);
            assertEquals(resultado.getTiempoSegundos(),
                    resultado.getSectorTimes().tiempoTotal(), 1e-9);
        }
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
    void laConfiguracionSoloAfectaAlPilotoElegido() {
        SimulationConfig normal = config(DrivingMode.NORMAL);
        QualifyingSession sesion = sesiones.simular(normal, WeatherCondition.SECO, null);

        LapResult verstappen = resultadoDe(sesion, 1);
        LapResult perez = resultadoDe(sesion, 2);

        double consumoSeleccionado = calculadora.consumoPorVuelta(
                rb20(), monza(), sesion.getEvolucionClimatica(), normal);
        double consumoCompanero = calculadora.consumoPorVuelta(
                rb20(), monza(), sesion.getEvolucionClimatica(), SimulationConfig.paraClasificacion());

        assertEquals(consumoSeleccionado, verstappen.getConsumoEstimado(), 1e-9);
        assertEquals(consumoCompanero, perez.getConsumoEstimado(), 1e-9);
        assertEquals(1, sesion.getConfig().getPilotoId());
    }

    @Test
    void rechazaUnPilotoQueNoConduceElVehiculoElegido() {
        SimulationConfig seleccionInvalida = new SimulationConfig(
                "Circuito de Monza", 3, "RB20", DrivingMode.NORMAL,
                AerodynamicLoad.MEDIA, TirePressure.ESTANDAR, FuelStrategy.BALANCEADA);

        ValidationException error = assertThrows(ValidationException.class,
                () -> sesiones.simular(seleccionInvalida, WeatherCondition.SECO, null));

        assertTrue(error.getMessage().contains("no conduce"));
    }

    @Test
    void laEvolucionConvergeAlResultadoCalculado() {
        List<SimulationSnapshot> muestras = new ArrayList<>();

        QualifyingSession sesion = sesiones.simular(
                config(DrivingMode.NORMAL), WeatherCondition.SECO, null, muestras::add);

        assertEquals(QualifyingService.SEGMENTOS_EVOLUCION, muestras.size());
        SimulationSnapshot ultima = muestras.get(muestras.size() - 1);
        LapResult seleccionado = resultadoDe(sesion, 1);

        assertEquals("Max Verstappen", ultima.piloto());
        assertEquals("RB20", ultima.vehiculo());
        assertEquals(1.0, ultima.progreso(), 1e-9);
        assertEquals(seleccionado.getConsumoEstimado(), ultima.consumoAcumulado(), 1e-9);
        assertEquals(seleccionado.getDesgasteEstimado(), ultima.desgasteAcumulado(), 1e-9);
        assertTrue(ultima.velocidadKmh() > 0);
        assertTrue(ultima.velocidadKmh() <= ultima.velocidadMaximaKmh());

        for (int i = 1; i < muestras.size(); i++) {
            assertTrue(muestras.get(i).consumoAcumulado() >= muestras.get(i - 1).consumoAcumulado());
            assertTrue(muestras.get(i).desgasteAcumulado() >= muestras.get(i - 1).desgasteAcumulado());
        }
    }

    @Test
    void laTelemetriaCubreLaVueltaYRespetaLimitesFisicos() {
        List<SimulationSnapshot> evolucion = new ArrayList<>();
        List<TelemetrySnapshot> telemetria = new ArrayList<>();

        QualifyingSession sesion = sesiones.simular(
                config(DrivingMode.NORMAL), WeatherCondition.SECO, null,
                evolucion::add, telemetria::add);

        assertEquals(QualifyingService.SEGMENTOS_EVOLUCION, telemetria.size());
        assertEquals(evolucion.size(), telemetria.size(), "cada segmento comparte una lectura");
        assertEquals(telemetria, sesion.getEvolucionVuelta(),
                "la evolución mostrada debe ser la misma que se persiste");

        LapResult seleccionado = resultadoDe(sesion, 1);
        TelemetrySnapshot ultima = telemetria.get(telemetria.size() - 1);
        assertEquals("Max Verstappen", ultima.piloto());
        assertEquals("RB20", ultima.vehiculo());
        assertEquals(3, ultima.sectorActual());
        assertEquals(sesion.getEvolucionClimatica().get(19), ultima.clima());
        assertEquals(seleccionado.getTiempoSegundos(), ultima.tiempoVueltaSegundos(), 1e-9);
        assertEquals(seleccionado.getTiempoSegundos() - monza().getRecordVuelta().getTiempoSegundos(),
                ultima.deltaSegundos(), 1e-9);
        assertEquals(0, ultima.combustibleRestantePorcentaje(), 1e-9);

        for (int i = 0; i < telemetria.size(); i++) {
            TelemetrySnapshot muestra = telemetria.get(i);
            assertEquals(sesion.getEvolucionClimatica().get(i), muestra.clima());
            assertEquals(evolucion.get(i).velocidadKmh(), muestra.velocidadKmh(), 1e-9,
                    "evolucion y telemetria deben provenir de la misma muestra");
            assertTrue(muestra.velocidadKmh() >= 0
                    && muestra.velocidadKmh() <= muestra.velocidadMaximaKmh());
            assertTrue(muestra.rpm() >= 4_000 && muestra.rpm() <= 15_000);
            assertTrue(muestra.temperaturaNeumaticosC() >= 35
                    && muestra.temperaturaNeumaticosC() <= 125);
            assertTrue(muestra.temperaturaMotorC() >= 75
                    && muestra.temperaturaMotorC() <= 125);
            assertTrue(muestra.sectorActual() >= 1 && muestra.sectorActual() <= 3);
            if (i > 0) {
                TelemetrySnapshot anterior = telemetria.get(i - 1);
                assertTrue(muestra.tiempoVueltaSegundos() >= anterior.tiempoVueltaSegundos());
                assertTrue(muestra.combustibleRestantePorcentaje()
                        <= anterior.combustibleRestantePorcentaje());
                assertTrue(muestra.desgasteNeumaticosPorcentaje()
                        >= anterior.desgasteNeumaticosPorcentaje());
            }
        }
    }

    @Test
    void guardaLaEvolucionAunqueLaSimulacionNoTengaCallbacksVisuales() {
        QualifyingSession sesion = sesiones.simular(
                config(DrivingMode.NORMAL), WeatherCondition.SECO, null);

        List<TelemetrySnapshot> muestras = sesion.getEvolucionVuelta();
        assertEquals(QualifyingService.SEGMENTOS_EVOLUCION, muestras.size());
        assertTrue(muestras.stream().allMatch(muestra -> muestra.piloto().equals("Max Verstappen")));
        assertEquals(1, muestras.get(0).segmento());
        assertEquals(QualifyingService.SEGMENTOS_EVOLUCION,
                muestras.get(muestras.size() - 1).segmento());
        assertThrows(UnsupportedOperationException.class,
                () -> muestras.add(muestras.get(0)), "la sesión no expone su lista mutable");
    }

    @Test
    void variasCarrerasConsecutivasNuncaSuperanLosTotalesAcumulados() {
        for (int carrera = 0; carrera < 250; carrera++) {
            List<SimulationSnapshot> evolucion = new ArrayList<>();

            sesiones.simular(config(DrivingMode.NORMAL), WeatherCondition.SECO,
                    null, evolucion::add);

            assertEquals(QualifyingService.SEGMENTOS_EVOLUCION, evolucion.size());
            for (SimulationSnapshot muestra : evolucion) {
                assertTrue(muestra.consumoAcumulado() <= muestra.consumoTotal());
                assertTrue(muestra.desgasteAcumulado() <= muestra.desgasteTotal());
            }
            SimulationSnapshot ultima = evolucion.get(evolucion.size() - 1);
            assertEquals(ultima.consumoTotal(), ultima.consumoAcumulado(), 0);
            assertEquals(ultima.desgasteTotal(), ultima.desgasteAcumulado(), 0);
        }
    }

    @Test
    void elClimaDinamicoFormaParteDelResultadoDeLaSesion() {
        QualifyingSession sesion = sesiones.simular(
                config(DrivingMode.NORMAL), WeatherCondition.LLUVIOSO, null);

        assertEquals(QualifyingService.SEGMENTOS_EVOLUCION,
                sesion.getEvolucionClimatica().size());
        WeatherSnapshot inicial = sesion.getEvolucionClimatica().get(0);
        WeatherSnapshot finalSesion = sesion.getEvolucionClimatica().get(19);
        assertTrue(inicial.temperaturaC() != finalSesion.temperaturaC());
        assertTrue(inicial.gripPorcentaje() != finalSesion.gripPorcentaje());
    }

    @Test
    void laLluviaIntensaProduceUnaVueltaMasLentaQueUnaPistaSeca() {
        WeatherSnapshot seco = new WeatherSnapshot(1, 1,
                com.formula1.model.DynamicWeatherState.SECO,
                25, 45, 10, 0, 37, 95, 95, 94);
        WeatherSnapshot lluvia = new WeatherSnapshot(1, 1,
                com.formula1.model.DynamicWeatherState.LLUVIA_INTENSA,
                17, 95, 100, 90, 16, 50, 45, 42);

        double tiempoSeco = new LapTimeCalculator(new Random(4)).calcularTiempo(
                verstappen(), rb20(), monza(), List.of(seco), config(DrivingMode.NORMAL));
        double tiempoLluvia = new LapTimeCalculator(new Random(4)).calcularTiempo(
                verstappen(), rb20(), monza(), List.of(lluvia), config(DrivingMode.NORMAL));

        assertTrue(tiempoLluvia > tiempoSeco * 1.15);
    }

    @Test
    void calculaEstadisticasDeTodosLosParticipantes() {
        QualifyingSession sesion = sesiones.simular(
                config(DrivingMode.NORMAL), WeatherCondition.SECO, null);

        SessionStatistics estadisticas = sesiones.calcularEstadisticas(sesion);

        assertTrue(estadisticas.tieneResultados());
        assertEquals(20, estadisticas.participantes());
        assertEquals(sesion.getPole().getTiempoSegundos(), estadisticas.tiempoPole(), 1e-9);
        assertEquals(sesion.getResultados().get(19).getGap(), estadisticas.diferenciaMaxima(), 1e-9);
        assertTrue(estadisticas.tiempoPromedio() >= estadisticas.tiempoPole());
        assertTrue(estadisticas.consumoPromedio() > 0);
        assertTrue(estadisticas.desgastePromedio() > 0);
    }

    @Test
    void crashIsReflectedInResultsEventsAndTelemetry() {
        SimulationEvent crash = EventCatalog.defaultEvents().stream()
                .filter(event -> event.type() == EventType.CRASH)
                .findFirst()
                .orElseThrow();
        EventProbabilityConfig onlyExceptional = new EventProbabilityConfig(
                0, 0, 0, 0, 0, 1, 0);
        QualifyingService crashSession = new QualifyingService(
                datos,
                new LapTimeCalculator(new Random(21)),
                new DynamicWeatherService(new Random(22)),
                new EventManager(onlyExceptional, new Random(23), List.of(crash)));
        List<TelemetrySnapshot> telemetry = new ArrayList<>();

        QualifyingSession session = crashSession.simular(
                config(DrivingMode.AGRESIVA), WeatherCondition.LLUVIOSO,
                null, null, telemetry::add);

        assertEquals(20, session.getEventos().size());
        assertTrue(session.getEventos().stream().allMatch(event -> event.tipo() == EventType.CRASH));
        assertTrue(session.getResultados().stream().noneMatch(LapResult::isVueltaValida));
        assertTrue(session.getResultados().stream().allMatch(result -> result.getTiempoSegundos() == 0));
        assertTrue(session.getResultados().stream().noneMatch(LapResult::hasSectorTimes));
        assertTrue(session.getResultados().stream().allMatch(result -> result.getSectorIncidente()
                != com.formula1.model.TrackSector.NONE));
        assertNull(session.getPole());

        assertEquals(QualifyingService.SEGMENTOS_EVOLUCION, telemetry.size());
        assertEquals(telemetry, session.getEvolucionVuelta());
        assertTrue(telemetry.stream().anyMatch(sample -> sample.evento().tipo() == EventType.CRASH));
        assertTrue(telemetry.stream().anyMatch(sample -> sample.estadoVuelta() != LapStatus.VALID));
        assertTrue(telemetry.stream()
                .filter(sample -> sample.estadoVuelta() != LapStatus.VALID)
                .allMatch(sample -> sample.velocidadKmh() == 0));
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
        SimulationConfig ahorro = new SimulationConfig("Circuito de Monza", 1, "RB20", DrivingMode.NORMAL,
                AerodynamicLoad.MEDIA, TirePressure.ESTANDAR, FuelStrategy.AHORRO);
        SimulationConfig empuje = new SimulationConfig("Circuito de Monza", 1, "RB20", DrivingMode.NORMAL,
                AerodynamicLoad.MEDIA, TirePressure.ESTANDAR, FuelStrategy.AGRESIVA);

        assertTrue(calculadora.consumoPorVuelta(rb20(), monza(), WeatherCondition.SECO, ahorro)
                < calculadora.consumoPorVuelta(rb20(), monza(), WeatherCondition.SECO, empuje));

        SimulationConfig presionBaja = new SimulationConfig("Circuito de Monza", 1, "RB20", DrivingMode.NORMAL,
                AerodynamicLoad.MEDIA, TirePressure.BAJA, FuelStrategy.BALANCEADA);
        SimulationConfig presionAlta = new SimulationConfig("Circuito de Monza", 1, "RB20", DrivingMode.NORMAL,
                AerodynamicLoad.MEDIA, TirePressure.ALTA, FuelStrategy.BALANCEADA);

        assertTrue(calculadora.desgastePorVuelta(rb20(), monza(), WeatherCondition.SECO, presionBaja)
                > calculadora.desgastePorVuelta(rb20(), monza(), WeatherCondition.SECO, presionAlta),
                "menos presión debe desgastar más");
    }

    private LapResult resultadoDe(QualifyingSession sesion, int pilotoId) {
        return sesion.getResultados().stream()
                .filter(resultado -> resultado.getPilotoId() == pilotoId)
                .findFirst()
                .orElseThrow();
    }
}
