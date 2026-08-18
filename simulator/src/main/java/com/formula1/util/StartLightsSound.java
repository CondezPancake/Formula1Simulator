package com.formula1.util;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

/**
 * Pitidos del semáforo de salida, generados por síntesis.
 *
 * Se sintetizan en lugar de reproducir un archivo por dos razones: el
 * proyecto no incluye ningún recurso de audio, y añadir {@code javafx-media}
 * solo para esto arrastraría una dependencia nativa a todo el build. Con
 * {@code javax.sound.sampled}, que ya viene en el JDK, basta.
 *
 * Nunca propaga errores: si la máquina no tiene salida de audio la secuencia
 * visual debe seguir igual, sin sonido pero sin romperse.
 */
public final class StartLightsSound {

    private static final float FRECUENCIA_MUESTREO = 44_100f;

    /** Grave para cada luz que se enciende, como en la señal real. */
    private static final double TONO_LUZ_HZ = 700;

    /** Agudo y más largo al apagarse: es la orden de salida. */
    private static final double TONO_SALIDA_HZ = 1_180;

    private StartLightsSound() {
    }

    /** Pitido corto de una luz encendida. */
    public static void luz() {
        reproducir(TONO_LUZ_HZ, 320, 0.32);
    }

    /** Pitido largo de la salida. */
    public static void salida() {
        reproducir(TONO_SALIDA_HZ, 900, 0.40);
    }

    /**
     * Emite un tono en un hilo aparte para no bloquear la animación.
     *
     * @param frecuencia   altura del tono en hercios
     * @param milisegundos duración
     * @param volumen      0..1
     */
    private static void reproducir(double frecuencia, int milisegundos, double volumen) {
        Thread hilo = new Thread(() -> emitir(frecuencia, milisegundos, volumen), "semaforo-f1");
        hilo.setDaemon(true);
        hilo.start();
    }

    private static void emitir(double frecuencia, int milisegundos, double volumen) {
        AudioFormat formato = new AudioFormat(FRECUENCIA_MUESTREO, 16, 1, true, false);
        try (SourceDataLine linea = AudioSystem.getSourceDataLine(formato)) {
            linea.open(formato);
            linea.start();
            byte[] onda = generarOnda(frecuencia, milisegundos, volumen);
            linea.write(onda, 0, onda.length);
            linea.drain();
        } catch (Exception sinAudio) {
            // Sin tarjeta de sonido, o con el dispositivo ocupado: la
            // secuencia de luces continúa igual. No es un fallo de la
            // aplicación y no debe interrumpir la salida.
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Onda senoidal con entrada y salida suavizadas. Sin ese suavizado, el
     * corte brusco de la señal produce un chasquido muy audible.
     */
    private static byte[] generarOnda(double frecuencia, int milisegundos, double volumen) {
        int muestras = (int) (FRECUENCIA_MUESTREO * milisegundos / 1000.0);
        byte[] datos = new byte[muestras * 2];
        int rampa = Math.min(muestras / 8, (int) (FRECUENCIA_MUESTREO * 0.01));

        for (int i = 0; i < muestras; i++) {
            double angulo = 2.0 * Math.PI * i * frecuencia / FRECUENCIA_MUESTREO;
            double atenuacion = 1.0;
            if (i < rampa) {
                atenuacion = i / (double) rampa;
            } else if (i > muestras - rampa) {
                atenuacion = (muestras - i) / (double) rampa;
            }
            short valor = (short) (Math.sin(angulo) * volumen * atenuacion * Short.MAX_VALUE);
            datos[i * 2] = (byte) (valor & 0xFF);
            datos[i * 2 + 1] = (byte) ((valor >> 8) & 0xFF);
        }
        return datos;
    }
}
