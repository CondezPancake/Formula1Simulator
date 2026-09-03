package com.formula1.service;

import com.formula1.model.EventImpact;
import com.formula1.model.EventOccurrence;
import com.formula1.model.EventType;
import com.formula1.model.LapResult;
import com.formula1.model.LapStatus;
import com.formula1.model.PitStopPhase;
import com.formula1.model.PitStopReason;
import com.formula1.model.PitStopRecord;
import com.formula1.model.RadioMessage;
import com.formula1.model.RadioMessage.Emisor;
import com.formula1.model.RadioMessage.Prioridad;
import com.formula1.model.TireChangeRecord;
import com.formula1.model.TireCompound;
import com.formula1.model.TrackSector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** La radio es lógica pura: se comprueba sin levantar JavaFX. */
class RaceRadioServiceTest {

    private static final int PILOTO = 7;

    private RaceRadioService radio;

    @BeforeEach
    void preparar() {
        radio = new RaceRadioService();
        radio.reiniciar();
    }

    @Test
    void elSaludoSoloSuenaUnaVez() {
        List<RadioMessage> primero = radio.saludoInicial("VER");
        assertEquals(2, primero.size(), "El check de radio lleva pregunta y respuesta");
        assertEquals(Emisor.INGENIERO, primero.get(0).emisor());
        assertEquals(Emisor.PILOTO, primero.get(1).emisor());
        assertTrue(radio.saludoInicial("VER").isEmpty(),
                "Repetir el saludo cada segmento sería ruido");
    }

    @Test
    void laBanderaRojaEsCritica() {
        EventOccurrence roja = new EventOccurrence(EventType.RED_FLAG, PILOTO, "Piloto",
                1, TrackSector.SECTOR_2, EventImpact.none());
        Optional<RadioMessage> mensaje = radio.desdeEvento(roja, PILOTO);
        assertTrue(mensaje.isPresent());
        assertEquals(Prioridad.CRITICA, mensaje.get().prioridad());
        assertTrue(mensaje.get().interrumpe(), "Una roja tiene que interrumpir");
    }

    @Test
    void unIncidenteLeveNoInterrumpe() {
        EventOccurrence bloqueo = new EventOccurrence(EventType.LOCK_UP, PILOTO, "Piloto",
                1, TrackSector.SECTOR_1, EventImpact.none());
        RadioMessage mensaje = radio.desdeEvento(bloqueo, PILOTO).orElseThrow();
        assertEquals(Prioridad.RUTINA, mensaje.prioridad());
        assertFalse(mensaje.interrumpe());
    }

    @Test
    void elEventoDeOtroPilotoSoloSeNarraSiEsGlobal() {
        EventOccurrence ajenoIndividual = new EventOccurrence(EventType.LOCK_UP, 99, "Otro",
                1, TrackSector.SECTOR_1, EventImpact.none());
        assertTrue(radio.desdeEvento(ajenoIndividual, PILOTO).isEmpty(),
                "El error de otro coche no es asunto de tu ingeniero");

        EventOccurrence ajenoGlobal = new EventOccurrence(EventType.YELLOW_FLAG, 99, "Otro",
                1, TrackSector.SECTOR_2, EventImpact.none());
        assertTrue(radio.desdeEvento(ajenoGlobal, PILOTO).isPresent(),
                "Una amarilla afecta a todos");
    }

    @Test
    void elSinEventoNoGeneraRadio() {
        EventOccurrence nada = new EventOccurrence(EventType.NO_EVENT, PILOTO, "Piloto",
                1, TrackSector.NONE, EventImpact.none());
        assertTrue(radio.desdeEvento(nada, PILOTO).isEmpty());
    }

    @Test
    void laPosicionSoloSeCantaCuandoCambia() {
        List<LapResult> parrilla = List.of(resultado(PILOTO, 3, 0.5));
        assertFalse(radio.desdeClasificacion(parrilla, PILOTO, 5).isEmpty(),
                "La primera lectura sí se canta");
        assertTrue(radio.desdeClasificacion(parrilla, PILOTO, 6).isEmpty(),
                "Repetir la misma posición cada segmento sería ruido");
    }

    @Test
    void perderPosicionesInterrumpe() {
        radio.desdeClasificacion(List.of(resultado(PILOTO, 2, 0.2)), PILOTO, 4);
        List<RadioMessage> caida = radio.desdeClasificacion(
                List.of(resultado(PILOTO, 6, 1.4)), PILOTO, 8);
        assertEquals(1, caida.size());
        assertEquals(Prioridad.IMPORTANTE, caida.get(0).prioridad());
        assertTrue(caida.get(0).texto().contains("P6"));
    }

    @Test
    void elBoxLlevaOrdenYAcuse() {
        PitStopRecord entrada = new PitStopRecord("stop-1", PILOTO, "Piloto", 1,
                8, 8, PitStopPhase.ENTERING, PitStopReason.TYRE_CONDITION,
                0, 0.45, 4, 4);
        List<RadioMessage> mensajes = radio.desdeParada(entrada, PILOTO);
        assertEquals(2, mensajes.size(), "A un box se le contesta siempre");
        assertEquals(Prioridad.CRITICA, mensajes.get(0).prioridad());
        assertTrue(mensajes.get(0).texto().toLowerCase().contains("box"));
        assertEquals(Emisor.PILOTO, mensajes.get(1).emisor());
    }

    @Test
    void elCambioDeNeumaticoExplicaElCompuesto() {
        TireChangeRecord cambio = new TireChangeRecord("stop-1", PILOTO, "Piloto", 1, 9,
                TireCompound.MEDIUM, TireCompound.SOFT, PitStopReason.TYRE_CONDITION);
        RadioMessage mensaje = radio.desdeCambioNeumaticos(cambio, PILOTO).orElseThrow();
        assertTrue(mensaje.texto().toLowerCase().contains("soft"));
        assertEquals(Prioridad.IMPORTANTE, mensaje.prioridad());
    }

    @Test
    void elDesgasteSoloSeAvisaUnaVez() {
        assertFalse(radio.desdeTelemetria(telemetria(85, 100, 90)).isEmpty(),
                "Con los neumáticos al límite hay que avisar");
        assertTrue(radio.desdeTelemetria(telemetria(90, 100, 90)).isEmpty(),
                "El mismo aviso repetido veinte veces sería ruido");
    }

    @Test
    void elCierreDistingueLaPole() {
        List<RadioMessage> pole = radio.cierreDeSesion(resultado(PILOTO, 1, 0), 20);
        assertTrue(pole.get(0).texto().toLowerCase().contains("pole"));

        List<RadioMessage> resto = new RaceRadioService()
                .cierreDeSesion(resultado(PILOTO, 9, 2.1), 20);
        assertTrue(resto.get(0).texto().contains("P9"));
    }

    @Test
    void sinResultadoElCierreNoRevienta() {
        List<RadioMessage> cierre = radio.cierreDeSesion(null, 20);
        assertEquals(1, cierre.size());
        assertFalse(cierre.get(0).texto().isBlank());
    }

    private LapResult resultado(int pilotoId, int posicion, double gap) {
        LapResult resultado = new LapResult(pilotoId, "Piloto", "Equipo", "Coche", 78.5);
        resultado.setPosicion(posicion);
        resultado.setGap(gap);
        resultado.setEstadoVuelta(LapStatus.VALID);
        return resultado;
    }

    private com.formula1.model.TelemetrySnapshot telemetria(double desgaste,
                                                            double combustible,
                                                            double motor) {
        com.formula1.model.WeatherSnapshot clima = new com.formula1.model.WeatherSnapshot(
                5, 20, com.formula1.model.DynamicWeatherState.SECO,
                26, 45, 5, 0, 38, 92, 90, 91);
        return new com.formula1.model.TelemetrySnapshot("Piloto", "Coche", 5, 20,
                280, 340, 11000, combustible, desgaste, 95, motor, 2, 78.5, 0.1,
                clima, LapStatus.VALID,
                new EventOccurrence(EventType.NO_EVENT, PILOTO, "Piloto", 1,
                        TrackSector.NONE, EventImpact.none()));
    }
}
