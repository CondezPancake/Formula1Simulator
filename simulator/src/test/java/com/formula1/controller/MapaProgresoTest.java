package com.formula1.controller;

import java.util.ArrayList;
import java.util.List;

import com.formula1.model.LapResult;
import com.formula1.model.LapStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La colocación de los coches se comprueba sin entorno gráfico. Lo que se
 * protege aquí es la promesa de la funcionalidad: que lo que se ve en la pista
 * no puede contradecir a la tabla de tiempos.
 */
class MapaProgresoTest {

    private static final double VUELTA = 90.0;

    /**
     * Parrilla ya ordenada como la entrega el motor en ese segmento: el tiempo
     * es el <b>acumulado</b> hasta ahí, no el de la vuelta completa, que es lo
     * que permite proyectar el ritmo del líder.
     */
    private static List<LapResult> parrilla(int segmento, double... gaps) {
        double acumuladoLider = VUELTA * segmento / MapaProgreso.TOTAL_SEGMENTOS;
        List<LapResult> parrilla = new ArrayList<>();
        for (int i = 0; i < gaps.length; i++) {
            LapResult r = new LapResult(i + 1, "Piloto " + (i + 1), "Equipo " + (i / 2),
                    "Coche " + (i / 2), acumuladoLider + gaps[i]);
            r.setPosicion(i + 1);
            r.setGap(gaps[i]);
            r.setEstadoVuelta(LapStatus.VALID);
            parrilla.add(r);
        }
        return parrilla;
    }

    private static double[] gapsRealistas() {
        // Reparto parecido al que produce el motor: la pole y luego decimas.
        double[] gaps = new double[20];
        for (int i = 1; i < 20; i++) {
            gaps[i] = gaps[i - 1] + (i % 3 == 0 ? 0.0 : 0.18);
        }
        return gaps;
    }

    /**
     * La invariante que da sentido a toda la funcionalidad: quien va delante en
     * la tabla va delante en la pista. Se prueba con empates incluidos, que es
     * donde una fórmula ingenua fallaría.
     */
    @Test
    void elOrdenEnPistaEsElDeLaTorreDeTiempos() {
        MapaProgreso mapa = new MapaProgreso();
        MapaProgreso.Estado estado = mapa.construir(10, parrilla(10, gapsRealistas()));

        List<MapaProgreso.Marcador> marcadores = estado.marcadores();
        for (int i = 1; i < marcadores.size(); i++) {
            assertTrue(marcadores.get(i).fraccion() < marcadores.get(i - 1).fraccion(),
                    "El piloto en P" + (i + 1) + " no va por detrás del de P" + i);
        }
    }

    @Test
    void elLiderVaJustoEnLaCabezaDelPeloton() {
        MapaProgreso mapa = new MapaProgreso();
        MapaProgreso.Estado estado = mapa.construir(7, parrilla(7, gapsRealistas()));

        // Gap cero y suelo cero: el líder marca la referencia del segmento.
        assertEquals(7 / 20.0, estado.marcadores().get(0).fraccion(), 1e-12);
    }

    @Test
    void conLaParrillaEmpatadaLosCochesSiguenSinSolaparse() {
        // Caso degenerado: 20 pilotos al mismo tiempo. Sin el suelo de
        // separación, los 20 marcadores caerían en el mismo píxel.
        MapaProgreso mapa = new MapaProgreso();
        MapaProgreso.Estado estado = mapa.construir(10, parrilla(10, new double[20]));

        List<MapaProgreso.Marcador> marcadores = estado.marcadores();
        for (int i = 1; i < marcadores.size(); i++) {
            double separacion = marcadores.get(i - 1).fraccion() - marcadores.get(i).fraccion();
            assertTrue(separacion >= MapaProgreso.SEPARACION_MIN - 1e-9,
                    "Separación insuficiente entre P" + i + " y P" + (i + 1));
        }
    }

    @Test
    void unHuecoGrandeSeDibujaAEscalaRealYNoAlMinimo() {
        // Tres segundos sobre una vuelta de 90 son un 3,3 % del trazado, muy por
        // encima del suelo del segundo clasificado. Debe mandar el dato real.
        MapaProgreso mapa = new MapaProgreso();
        List<LapResult> parrilla = parrilla(20, 0.0, 3.0);
        MapaProgreso.Estado estado = mapa.construir(20, parrilla);

        double separacion = estado.marcadores().get(0).fraccion()
                - estado.marcadores().get(1).fraccion();
        assertEquals(3.0 / VUELTA, separacion, 1e-6);
        assertTrue(separacion > MapaProgreso.SEPARACION_MIN);
    }

    @Test
    void laVueltaInvalidadaCongelaElCocheDondeEstaba() {
        MapaProgreso mapa = new MapaProgreso();
        List<LapResult> parrilla = parrilla(9, 0.0, 0.3, 0.6);

        double antes = mapa.construir(9, parrilla).marcadores().get(2).fraccion();

        // El motor invalida la vuelta al llegar al sector del incidente y la
        // manda al final de la parrilla con gap cero.
        LapResult afectado = parrilla.remove(2);
        afectado.setEstadoVuelta(LapStatus.INVALID);
        afectado.setGap(0);
        afectado.setPosicion(3);
        parrilla.add(afectado);

        double despues = mapa.construir(10, parrilla).marcadores().get(2).fraccion();

        assertEquals(antes, despues, 1e-12, "El coche invalidado siguió avanzando");
        assertTrue(!mapa.construir(11, parrilla).marcadores().get(2).valida());
    }

    @Test
    void elPilotoFueraDeSesionSeQuedaParado() {
        MapaProgreso mapa = new MapaProgreso();
        List<LapResult> parrilla = parrilla(6, 0.0, 0.4);
        mapa.construir(6, parrilla);

        LapResult fuera = parrilla.get(1);
        fuera.setEstadoVuelta(LapStatus.OUT);
        fuera.setGap(0);

        double enSeis = mapa.construir(6, parrilla).marcadores().get(1).fraccion();
        double enVeinte = mapa.construir(20, parrilla).marcadores().get(1).fraccion();

        assertEquals(enSeis, enVeinte, 1e-12);
        assertTrue(mapa.construir(20, parrilla).marcadores().get(1).fuera());
    }

    @Test
    void laInterpolacionCruzaLaMetaPorElArcoCorto() {
        // De 0,98 a 0,02 hay cuatro centésimas hacia delante, no noventa y seis
        // hacia atrás. A mitad de camino el coche está justo en la meta.
        assertEquals(0.0, MapaProgreso.interpolar(0.98, 0.02, 0.5), 1e-9);

        // Y un tramo normal se interpola sin sorpresas.
        assertEquals(0.30, MapaProgreso.interpolar(0.20, 0.40, 0.5), 1e-9);

        // El resultado siempre queda dentro de la vuelta.
        for (int i = 0; i <= 100; i++) {
            double f = MapaProgreso.interpolar(0.95, 0.10, i / 100.0);
            assertTrue(f >= 0 && f < 1, "Fracción fuera de rango: " + f);
        }
    }

    @Test
    void elSuavizadoRespetaLosExtremos() {
        assertEquals(0.0, MapaProgreso.suavizar(0), 1e-12);
        assertEquals(1.0, MapaProgreso.suavizar(1), 1e-12);
        assertEquals(0.5, MapaProgreso.suavizar(0.5), 1e-12);
        // Fuera de [0,1] se recorta, para que un fotograma tardío no dispare.
        assertEquals(1.0, MapaProgreso.suavizar(1.4), 1e-12);
        assertEquals(0.0, MapaProgreso.suavizar(-0.2), 1e-12);
    }

    @Test
    void elPelotonAvanzaSegmentoTrasSegmento() {
        MapaProgreso mapa = new MapaProgreso();
        double[] gaps = gapsRealistas();
        double anterior = -1;
        for (int segmento = 1; segmento <= MapaProgreso.TOTAL_SEGMENTOS; segmento++) {
            double lider = mapa.construir(segmento, parrilla(segmento, gaps)).marcadores().get(0).fraccion();
            assertTrue(lider > anterior, "El líder retrocedió en el segmento " + segmento);
            anterior = lider;
        }
    }
}
