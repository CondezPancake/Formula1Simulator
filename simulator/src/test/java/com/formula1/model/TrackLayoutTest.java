package com.formula1.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La geometría del trazado se comprueba sin entorno gráfico: es matemática
 * pura y ahí es donde puede romperse el movimiento de los coches.
 */
class TrackLayoutTest {

    private static final double PRECISION = 1e-9;

    /** Un óvalo sencillo: se conoce su forma, así que los fallos se ven. */
    private static TrackLayout ovalo() {
        return TrackLayout.desdePuntosDeControl(new double[][] {
            {0.5, 0.0}, {1.0, 0.25}, {1.0, 0.75}, {0.5, 1.0}, {0.0, 0.75}, {0.0, 0.25},
        });
    }

    @Test
    void hacenFaltaAlMenosCuatroPuntosDeControl() {
        assertThrows(IllegalArgumentException.class,
                () -> TrackLayout.desdePuntosDeControl(new double[][] {{0, 0}, {1, 0}, {1, 1}}));
        assertThrows(IllegalArgumentException.class,
                () -> TrackLayout.desdePuntosDeControl(null));
    }

    @Test
    void laFraccionSeEnvuelveYElLazoCierra() {
        TrackLayout trazado = ovalo();

        // 1.0 es la misma vuelta que 0.0: si no, el coche daria un salto al
        // cruzar la meta.
        assertEquals(trazado.puntoEn(0).x(), trazado.puntoEn(1.0).x(), PRECISION);
        assertEquals(trazado.puntoEn(0).y(), trazado.puntoEn(1.0).y(), PRECISION);

        // Y las vueltas de mas o de menos caen donde deben.
        assertEquals(trazado.puntoEn(0.75).x(), trazado.puntoEn(-0.25).x(), PRECISION);
        assertEquals(trazado.puntoEn(0.75).y(), trazado.puntoEn(-0.25).y(), PRECISION);
        assertEquals(trazado.puntoEn(0.5).x(), trazado.puntoEn(2.5).x(), PRECISION);
        assertEquals(trazado.puntoEn(0.5).y(), trazado.puntoEn(2.5).y(), PRECISION);
    }

    @Test
    void elCierreDelLazoNoDejaCostura() {
        TrackLayout trazado = ovalo();
        TrackLayout.Punto antes = trazado.puntoEn(0.9999);
        TrackLayout.Punto despues = trazado.puntoEn(0.0001);

        // El salto en la costura no puede pasar de lo que mide un tramo de
        // muestreo, o se veria un tiron justo en la linea de meta.
        double salto = Math.hypot(despues.x() - antes.x(), despues.y() - antes.y());
        assertTrue(salto < trazado.longitud() / trazado.muestras(),
                "Hay costura en el cierre del lazo: salto de " + salto);
    }

    /**
     * La propiedad de la que depende todo el movimiento: avances iguales del
     * parámetro tienen que recorrer distancias iguales. Sin reparametrizar,
     * los coches acelerarían en las curvas y frenarían en las rectas.
     */
    @Test
    void avancesIgualesDeFraccionRecorrenDistanciasIguales() {
        TrackLayout trazado = ovalo();
        int pasos = 500;
        double esperada = trazado.longitud() / pasos;

        for (int i = 0; i < pasos; i++) {
            TrackLayout.Punto a = trazado.puntoEn(i / (double) pasos);
            TrackLayout.Punto b = trazado.puntoEn((i + 1) / (double) pasos);
            double recorrido = Math.hypot(b.x() - a.x(), b.y() - a.y());
            // La tolerancia cubre que se mide la cuerda y no el arco.
            assertEquals(esperada, recorrido, esperada * 0.06,
                    "Velocidad desigual en la fraccion " + i / (double) pasos);
        }
    }

    @Test
    void laTangenteApuntaEnElSentidoDeLaMarcha() {
        TrackLayout trazado = ovalo();
        for (int i = 0; i < 40; i++) {
            double t = i / 40.0;
            TrackLayout.Punto actual = trazado.puntoEn(t);
            TrackLayout.Punto siguiente = trazado.puntoEn(t + 0.004);
            double haciaSiguiente = Math.atan2(siguiente.y() - actual.y(),
                    siguiente.x() - actual.x());
            double diferencia = Math.abs(Math.atan2(
                    Math.sin(haciaSiguiente - actual.angulo()),
                    Math.cos(haciaSiguiente - actual.angulo())));
            assertTrue(diferencia < 0.25,
                    "La tangente no mira adonde avanza el coche en t=" + t);
        }
    }

    @Test
    void ningunaFraccionProduceNaN() {
        TrackLayout trazado = ovalo();
        for (int i = 0; i < 2000; i++) {
            TrackLayout.Punto p = trazado.puntoEn(i / 1000.0 - 0.5);
            assertTrue(Double.isFinite(p.x()) && Double.isFinite(p.y())
                    && Double.isFinite(p.angulo()), "NaN en la fraccion " + i);
        }
    }

    @Test
    void elEncajeCabeEnElLienzoYConservaLaProporcion() {
        TrackLayout trazado = ovalo();
        double ancho = 640;
        double alto = 260;
        double margen = 18;
        TrackLayout.Encaje encaje = trazado.encajarEn(ancho, alto, margen);

        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        for (int i = 0; i < 1000; i++) {
            TrackLayout.Punto p = trazado.puntoEn(i / 1000.0);
            double x = encaje.x(p.x());
            double y = encaje.y(p.y());
            assertTrue(x >= margen - 0.5 && x <= ancho - margen + 0.5, "Se sale en X: " + x);
            assertTrue(y >= margen - 0.5 && y <= alto - margen + 0.5, "Se sale en Y: " + y);
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }

        // Una sola escala para los dos ejes: el circuito no se deforma.
        double[] caja = trazado.limites();
        assertEquals((caja[2] - caja[0]) / (caja[3] - caja[1]),
                (maxX - minX) / (maxY - minY), 1e-6);
    }

    @Test
    void laCurvaPasaPorSusPuntosDeControl() {
        // Es lo que permite calcar un trazado colocando puntos sobre el: si la
        // spline no pasara por ellos, el calco no valdria.
        double[][] control = {{0.5, 0.0}, {1.0, 0.25}, {1.0, 0.75}, {0.5, 1.0},
                              {0.0, 0.75}, {0.0, 0.25}};
        TrackLayout trazado = TrackLayout.desdePuntosDeControl(control);

        for (double[] punto : control) {
            double mejor = Double.MAX_VALUE;
            for (int i = 0; i < trazado.muestras(); i++) {
                mejor = Math.min(mejor, Math.hypot(trazado.x(i) - punto[0],
                        trazado.y(i) - punto[1]));
            }
            assertEquals(0, mejor, 1e-6,
                    "La curva no pasa por (" + punto[0] + ", " + punto[1] + ")");
        }
    }

    @Test
    void losPuntosDeControlRepetidosNoRompenLaCurva() {
        // La formula centripeta divide por la distancia entre puntos, asi que
        // un duplicado la haria estallar. Debe caer a las tangentes uniformes.
        TrackLayout trazado = TrackLayout.desdePuntosDeControl(new double[][] {
            {0.0, 0.0}, {1.0, 0.0}, {1.0, 0.0}, {1.0, 1.0}, {0.0, 1.0},
        });
        for (int i = 0; i < 500; i++) {
            TrackLayout.Punto p = trazado.puntoEn(i / 500.0);
            assertTrue(Double.isFinite(p.x()) && Double.isFinite(p.y()));
        }
        assertTrue(trazado.longitud() > 0);
        assertNotEquals(0.0, trazado.longitud());
    }
}
