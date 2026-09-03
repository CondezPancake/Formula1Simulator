package com.formula1.util;

import javafx.application.Platform;
import javafx.scene.media.MediaPlayer;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

/**
 * Convierte texto en la voz de la radio con
 * <a href="https://github.com/rhasspy/piper">Piper</a>: un sintetizador
 * neuronal que corre en local, sin conexión ni coste.
 *
 * <p>Cada frase es un proceso de Piper aparte, así que la síntesis se lanza
 * siempre en el pool de {@link Async} y nunca en el hilo de FX —el proceso
 * tarda unos cientos de milisegundos, tiempo de sobra para notarse si
 * bloqueara la interfaz. Si Piper no está instalado en {@code tools/piper},
 * si el proceso falla o si el usuario ha apagado la voz, se degrada a
 * silencio y se avisa igual al que llamó: la radio sigue funcionando solo
 * con texto, como antes de que existiera esta clase.
 */
public final class TtsManager {

    private static final Preferences PREFS = Preferences.userNodeForPackage(TtsManager.class);
    private static final String CLAVE_VOZ_HABILITADA = "vozRadioHabilitada";
    private static final long TIMEOUT_SEGUNDOS = 10;

    private static final Path RAIZ_PIPER = localizarRaiz();
    private static final Path PIPER_EXE = RAIZ_PIPER == null ? null : RAIZ_PIPER.resolve("bin/piper.exe");
    private static final Path MODELO_VOZ = RAIZ_PIPER == null ? null
            : RAIZ_PIPER.resolve("voices/es_ES-davefx-medium.onnx");

    private static boolean vozHabilitada = PREFS.getBoolean(CLAVE_VOZ_HABILITADA, true);

    /** La voz en curso, para poder callarla si la sesión se reinicia a media frase. */
    private static volatile MediaPlayer reproductorActual;

    private TtsManager() {
    }

    public static boolean isDisponible() {
        return PIPER_EXE != null && Files.isRegularFile(PIPER_EXE) && Files.isRegularFile(MODELO_VOZ);
    }

    public static boolean isVozHabilitada() {
        return vozHabilitada;
    }

    public static void setVozHabilitada(boolean habilitada) {
        vozHabilitada = habilitada;
        PREFS.putBoolean(CLAVE_VOZ_HABILITADA, habilitada);
    }

    /** Corta en seco la frase que estuviera sonando. */
    public static void detener() {
        MediaPlayer actual = reproductorActual;
        if (actual != null) {
            actual.stop();
        }
    }

    /**
     * Sintetiza y reproduce {@code texto}. Llama a {@code alTerminar} en el
     * hilo de FX cuando termina, con {@code true} si de verdad sonó voz y
     * {@code false} si se quedó en silencio por cualquier motivo —así quien
     * marca el ritmo de la radio puede dar más tiempo de lectura al texto
     * cuando no hay audio que lo acompañe.
     */
    public static void hablar(String texto, double factorVolumen, Consumer<Boolean> alTerminar) {
        if (!vozHabilitada || !isDisponible() || AudioManager.isMute()
                || texto == null || texto.isBlank()) {
            Platform.runLater(() -> alTerminar.accept(false));
            return;
        }
        Async.ejecutar(() -> sintetizar(texto, factorVolumen, alTerminar));
    }

    private static void sintetizar(String texto, double factorVolumen, Consumer<Boolean> alTerminar) {
        Path wav;
        try {
            wav = Files.createTempFile("f1-radio-", ".wav");
            wav.toFile().deleteOnExit();
        } catch (IOException e) {
            Platform.runLater(() -> alTerminar.accept(false));
            return;
        }
        Process proceso = null;
        try {
            proceso = new ProcessBuilder(PIPER_EXE.toString(), "--model", MODELO_VOZ.toString(),
                    "--output_file", wav.toString())
                    .redirectErrorStream(true)
                    .start();
            try (OutputStream entrada = proceso.getOutputStream()) {
                entrada.write(texto.getBytes(StandardCharsets.UTF_8));
            }
            // Piper escribe sus logs en la salida ya fusionada arriba; hay que
            // drenarla o el proceso puede bloquearse si el búfer del pipe se
            // llena antes de terminar.
            proceso.getInputStream().readAllBytes();
            boolean termino = proceso.waitFor(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
            if (!termino || proceso.exitValue() != 0 || Files.size(wav) == 0) {
                if (!termino) {
                    proceso.destroyForcibly();
                }
                borrar(wav);
                Platform.runLater(() -> alTerminar.accept(false));
                return;
            }
        } catch (IOException | InterruptedException e) {
            if (proceso != null) {
                proceso.destroyForcibly();
            }
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            borrar(wav);
            Platform.runLater(() -> alTerminar.accept(false));
            return;
        }
        Path wavFinal = wav;
        Platform.runLater(() -> {
            // AudioManager llama a este Runnable en dos momentos distintos:
            // en el acto si no pudo arrancar (silenciado, fichero corrupto...),
            // o al terminar de sonar. "reproductor" solo queda asignado antes
            // del segundo caso, así que sirve para distinguir uno de otro.
            MediaPlayer[] reproductor = new MediaPlayer[1];
            reproductor[0] = AudioManager.reproducirArchivo(wavFinal.toFile(), factorVolumen, () -> {
                reproductorActual = null;
                borrar(wavFinal);
                alTerminar.accept(reproductor[0] != null);
            });
            reproductorActual = reproductor[0];
        });
    }

    private static void borrar(Path archivo) {
        try {
            Files.deleteIfExists(archivo);
        } catch (IOException ignorado) {
            // Un temporal huérfano no es un problema del que valga la pena avisar.
        }
    }

    private static Path localizarRaiz() {
        if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
            return null;
        }
        String override = System.getProperty("piper.home");
        Path[] candidatos = override != null
                ? new Path[]{Path.of(override)}
                : new Path[]{Path.of("tools/piper"), Path.of("../tools/piper")};
        for (Path candidato : candidatos) {
            if (Files.isRegularFile(candidato.resolve("bin/piper.exe"))) {
                return candidato.toAbsolutePath().normalize();
            }
        }
        return null;
    }
}
