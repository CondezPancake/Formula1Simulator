package com.formula1.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.formula1.model.LapResult;

/**
 * Convierte la clasificación en vivo en la posición de cada coche sobre el
 * trazado.
 *
 * <p>Trabaja sobre el <b>mismo {@code List<LapResult>} que puebla la torre de
 * tiempos</b>. Ese objeto compartido es la garantía de coherencia: el dot y la
 * fila no son dos cálculos que puedan divergir, son el mismo dato leído dos
 * veces.
 *
 * <p>No importa nada de JavaFX a propósito, para poder comprobar la invariante
 * principal —que el orden en pista es el de la tabla— sin levantar el toolkit,
 * igual que {@code util.ImageCrop.recorteDe}.
 */
public final class MapaProgreso {

    /** Segmentos en que el motor reparte la vuelta ({@code SEGMENTOS_EVOLUCION}). */
    public static final int TOTAL_SEGMENTOS = 20;

    /**
     * Separación mínima entre dos posiciones consecutivas, en vueltas.
     *
     * <p>Existe porque el pelotón real cabe en un 2-3 % de la vuelta —dos o tres
     * segundos sobre noventa— y ahí veinte marcadores se solapan hasta ser
     * ilegibles. Con este suelo el campo ocupa al menos un 21 % del trazado.
     *
     * <p>Es una decisión de presentación, y solo <b>añade</b> separación donde la
     * geometría no la daba: nunca comprime un hueco real, y por ser una función
     * estrictamente creciente de la posición no puede alterar el orden.
     */
    static final double SEPARACION_MIN = 0.011;

    /** Última fracción válida de cada piloto, para congelar al que se va fuera. */
    private final Map<Integer, Double> ultimaValida = new HashMap<>();

    /** Un coche sobre el trazado, listo para pintar. */
    public record Marcador(int pilotoId, String piloto, String equipo, int posicion,
                           double fraccion, double gapSegundos, boolean valida,
                           boolean fuera) {
    }

    /** Foto completa de la pista en un segmento. */
    public record Estado(int segmento, int totalSegmentos, List<Marcador> marcadores) {

        public Estado {
            marcadores = List.copyOf(marcadores);
        }

        public double progreso() {
            return segmento / (double) totalSegmentos;
        }
    }

    /** Olvida el historial de fracciones. Se llama al empezar cada sesión. */
    public void reiniciar() {
        ultimaValida.clear();
    }

    /**
     * Coloca a los veinte pilotos a partir de la clasificación de ese segmento.
     *
     * @param segmento      segmento en curso, de 1 a {@link #TOTAL_SEGMENTOS}
     * @param clasificacion parrilla ya ordenada que entrega el motor
     */
    public Estado construir(int segmento, List<LapResult> clasificacion) {
        int seguro = Math.max(1, Math.min(segmento, TOTAL_SEGMENTOS));
        double base = seguro / (double) TOTAL_SEGMENTOS;
        double vueltaLider = vueltaProyectadaDelLider(clasificacion, seguro);

        List<Marcador> marcadores = new ArrayList<>(clasificacion.size());
        for (LapResult resultado : clasificacion) {
            marcadores.add(marcadorDe(resultado, base, vueltaLider));
        }
        return new Estado(seguro, TOTAL_SEGMENTOS, marcadores);
    }

    private Marcador marcadorDe(LapResult resultado, double base, double vueltaLider) {
        boolean valida = resultado.isVueltaValida();
        double piso = SEPARACION_MIN * Math.max(0, resultado.getPosicion() - 1);
        double fraccion;

        if (valida) {
            // El retraso real convierte el gap en segundos a distancia recorrida.
            // El suelo solo entra cuando el pelotón viene demasiado apretado.
            double retrasoReal = resultado.getGap() / vueltaLider;
            fraccion = base - Math.max(retrasoReal, piso);
            ultimaValida.put(resultado.getPilotoId(), fraccion);
        } else {
            // `ordenarParrilla` deja el gap a cero en las vueltas invalidadas, así
            // que la fórmula las apilaría sobre el líder. Un coche que ya no gira
            // se queda donde estaba.
            fraccion = ultimaValida.getOrDefault(resultado.getPilotoId(), base - piso);
        }
        return new Marcador(resultado.getPilotoId(), resultado.getPiloto(),
                resultado.getEquipo(), resultado.getPosicion(), fraccion,
                resultado.getGap(), valida, estaFuera(resultado));
    }

    private static boolean estaFuera(LapResult resultado) {
        return resultado.getEstadoVuelta() == com.formula1.model.LapStatus.OUT;
    }

    /**
     * Vuelta completa que lleva el líder a ese ritmo. Se proyecta desde el tiempo
     * acumulado en vez de usar el tiempo final porque durante la sesión el tiempo
     * final todavía no está a la vista.
     */
    private static double vueltaProyectadaDelLider(List<LapResult> clasificacion, int segmento) {
        double tiempoLider = 0;
        for (LapResult resultado : clasificacion) {
            if (resultado.isVueltaValida() && resultado.getTiempoSegundos() > 0) {
                tiempoLider = resultado.getTiempoSegundos();
                break;
            }
        }
        return Math.max(0.001, tiempoLider * TOTAL_SEGMENTOS / segmento);
    }

    /**
     * Interpola entre dos fracciones de vuelta <b>por el arco corto</b>.
     *
     * <p>Sin esto, un coche que cruza la meta entre dos segmentos (0,98 → 0,02)
     * retrocedería barriendo el trazado entero en lugar de avanzar cuatro
     * centésimas de vuelta.
     */
    public static double interpolar(double origen, double destino, double avance) {
        double delta = destino - origen;
        delta -= Math.floor(delta + 0.5);
        double fraccion = origen + delta * avance;
        return fraccion - Math.floor(fraccion);
    }

    /** Suavizado clásico: arranca y frena sin tirón. */
    public static double suavizar(double avance) {
        double u = Math.max(0, Math.min(1, avance));
        return u * u * (3 - 2 * u);
    }
}
