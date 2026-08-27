package com.formula1.util;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.net.URL;
import java.util.prefs.Preferences;

/**
 * Punto único de reproducción de sonido de la app: efectos cortos (clics,
 * stingers) y música de fondo con loop y crossfade entre pistas.
 *
 * La carga de cada recurso es defensiva a propósito: todavía no hay archivos
 * de audio empaquetados (el usuario los añadirá después), así que cualquier
 * ausencia o fallo de códec debe degradar a silencio, nunca a una excepción
 * que tumbe la intro, el menú o el arranque de la app.
 */
public final class AudioManager {

    private static final Duration CROSSFADE = Duration.millis(400);

    private static final Preferences PREFS = Preferences.userNodeForPackage(AudioManager.class);
    private static final String CLAVE_VOL_MUSICA = "volumenMusica";
    private static final String CLAVE_VOL_SFX = "volumenSfx";
    private static final String CLAVE_MUTE = "silenciado";

    private static double volumenMusica = PREFS.getDouble(CLAVE_VOL_MUSICA, 0.6);
    private static double volumenSfx = PREFS.getDouble(CLAVE_VOL_SFX, 0.8);
    private static boolean silenciado = PREFS.getBoolean(CLAVE_MUTE, false);

    private static MediaPlayer musicaActual;

    private AudioManager() {
    }

    /** Reproduce un efecto corto una sola vez (clic, stinger). */
    public static void reproducirSfx(String recursoClasspath) {
        if (silenciado || recursoClasspath == null) {
            return;
        }
        URL recurso = AudioManager.class.getResource(recursoClasspath);
        if (recurso == null) {
            return;
        }
        try {
            AudioClip clip = new AudioClip(recurso.toExternalForm());
            clip.setVolume(volumenSfx);
            clip.play();
        } catch (RuntimeException ignorado) {
            // Sin el archivo o sin códec disponible, la app sigue siendo usable en silencio.
        }
    }

    /** Arranca música de fondo, deteniendo la anterior si la había. */
    public static void reproducirMusica(String recursoClasspath, boolean loop) {
        detenerMusica();
        MediaPlayer reproductor = crear(recursoClasspath, loop);
        if (reproductor == null) {
            return;
        }
        musicaActual = reproductor;
        reproductor.setVolume(silenciado ? 0 : volumenMusica);
        reproductor.play();
    }

    /** Detiene la música actual con un fade corto para no cortar en seco. */
    public static void detenerMusica() {
        MediaPlayer anterior = musicaActual;
        musicaActual = null;
        if (anterior == null) {
            return;
        }
        apagarConFade(anterior);
    }

    /** Cruza de la pista actual a otra: fade-out de la que suena, fade-in de la nueva. */
    public static void crossfadeMusica(String siguienteRecurso) {
        detenerMusica();
        MediaPlayer reproductor = crear(siguienteRecurso, true);
        if (reproductor == null) {
            return;
        }
        musicaActual = reproductor;
        reproductor.setVolume(0);
        reproductor.play();
        if (!silenciado) {
            Timeline entrada = new Timeline(new KeyFrame(CROSSFADE,
                    new KeyValue(reproductor.volumeProperty(), volumenMusica)));
            entrada.play();
        }
    }

    private static MediaPlayer crear(String recursoClasspath, boolean loop) {
        if (recursoClasspath == null) {
            return null;
        }
        URL recurso = AudioManager.class.getResource(recursoClasspath);
        if (recurso == null) {
            return null;
        }
        try {
            MediaPlayer reproductor = new MediaPlayer(new Media(recurso.toExternalForm()));
            if (loop) {
                reproductor.setCycleCount(MediaPlayer.INDEFINITE);
            }
            reproductor.setOnError(() -> { });
            return reproductor;
        } catch (RuntimeException ignorado) {
            return null;
        }
    }

    private static void apagarConFade(MediaPlayer reproductor) {
        Timeline salida = new Timeline(new KeyFrame(CROSSFADE,
                new KeyValue(reproductor.volumeProperty(), 0)));
        salida.setOnFinished(e -> {
            reproductor.stop();
            reproductor.dispose();
        });
        salida.play();
    }

    // --- volumen / silencio, persistidos -----------------------------------

    public static double getVolumenMusica() {
        return volumenMusica;
    }

    public static double getVolumenSfx() {
        return volumenSfx;
    }

    public static boolean isMute() {
        return silenciado;
    }

    public static void setVolumenMusica(double v01) {
        volumenMusica = Math.max(0, Math.min(1, v01));
        PREFS.putDouble(CLAVE_VOL_MUSICA, volumenMusica);
        if (musicaActual != null && !silenciado) {
            musicaActual.setVolume(volumenMusica);
        }
    }

    public static void setVolumenSfx(double v01) {
        volumenSfx = Math.max(0, Math.min(1, v01));
        PREFS.putDouble(CLAVE_VOL_SFX, volumenSfx);
    }

    public static void setMute(boolean mute) {
        silenciado = mute;
        PREFS.putBoolean(CLAVE_MUTE, mute);
        if (musicaActual != null) {
            musicaActual.setVolume(mute ? 0 : volumenMusica);
        }
    }
}
