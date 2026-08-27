package com.formula1.util;

import javafx.animation.Interpolator;
import javafx.util.Duration;

/**
 * Duraciones e interpoladores compartidos por el menú, la navegación del
 * shell y las transiciones entre pantallas.
 *
 * Un único sitio para afinar el "ritmo" de la interfaz sin ir a buscar
 * números sueltos por varias clases.
 */
public final class Animaciones {

    public static final Interpolator EASE_OUT = Interpolator.EASE_OUT;
    public static final Interpolator EASE_BOTH = Interpolator.EASE_BOTH;

    /** Hover de un tile del menú y atenuación de sus vecinas. */
    public static final Duration HOVER = Duration.millis(180);

    /** Pulso de confirmación de la tarjeta al elegirla. */
    public static final Duration PULSO_TILE = Duration.millis(110);

    /** Cruce con slide entre el menú y el shell (y su inversa). */
    public static final Duration TRANSICION_SECCION = Duration.millis(380);

    /** Entrada de cada vista nueva dentro del shell (Navigator). */
    public static final Duration ENTRADA_PANTALLA = Duration.millis(360);

    /** Cuánto se mantiene cada foto del fondo del menú antes de cambiar. */
    public static final Duration FONDO_HOLD = Duration.seconds(3);

    /** Duración del crossfade entre fotos del fondo del menú. */
    public static final Duration FONDO_CROSSFADE = Duration.millis(1000);

    private Animaciones() {
    }
}
