package com.formula1.service;

import com.formula1.event.RaceEvent;
import com.formula1.model.TrackSector;
import com.formula1.util.FormatUtils;

import java.util.Locale;

/**
 * Pone en palabras un {@link RaceEvent}. Es lógica pura: dado el mismo
 * evento siempre produce el mismo texto, sin estado ni dependencias de
 * JavaFX.
 */
public class RaceNarratorService {

    public String generate(RaceEvent event) {
        return switch (event.getType()) {
            case RADIO_CHECK -> "Radio check, " + event.getDriverName() + ". ¿Me copias?";
            case TIRE_WEAR -> "CRITICO".equals(event.getDetail())
                    ? "Neumáticos al límite. Gestiona la salida de curva."
                    : "Los neumáticos están cayendo. Cuida la tracción.";
            case FUEL_LEVEL -> "CRITICO".equals(event.getDetail())
                    ? "Vamos justos de combustible. Levanta y arrastra en la recta."
                    : String.format(Locale.ROOT, "Combustible al %.0f %%. Modo ahorro.",
                            event.getValue());
            case ENGINE_TEMPERATURE -> "CRITICO".equals(event.getDetail())
                    ? String.format(Locale.ROOT, "Temperatura de motor %.0f grados. Baja el modo.",
                            event.getValue())
                    : "Temperatura de motor alta. Vigílala.";
            case FLAG_CHANGE -> banderaTexto(event);
            case POSITION_UPDATE -> posicionTexto(event);
            case PIT_STOP -> pitStopTexto(event);
            case TIRE_CHANGE -> tireChangeTexto(event);
            case SESSION_END -> sessionEndTexto(event);
        };
    }

    private String banderaTexto(RaceEvent event) {
        return switch (event.getDetail()) {
            case "GREEN" -> "Pista libre. Verde, verde.";
            case "LOCAL_YELLOW" -> "Amarilla local en " + etiquetaSector(event.getSector())
                    + ". Atento.";
            case "YELLOW" -> "Bandera amarilla en " + etiquetaSector(event.getSector())
                    + ". Levanta el pie.";
            case "RED" -> "Bandera roja. Bandera roja. Entra a boxes.";
            default -> "";
        };
    }

    private String posicionTexto(RaceEvent event) {
        int posicion = event.getCount();
        String gap = posicion == 1 ? ""
                : " Diferencia con la pole " + FormatUtils.formatGap(event.getValue()) + ".";
        return switch (event.getDetail()) {
            case "POLE" -> "P1. Estás en la pole provisional." + gap;
            case "GANADA" -> "Buen trabajo, subes a P" + posicion + "." + gap;
            case "PERDIDA" -> "Te han pasado. Ahora P" + posicion + "." + gap;
            default -> "Vas P" + posicion + "." + gap;
        };
    }

    private String pitStopTexto(RaceEvent event) {
        String fase = event.getDetail();
        if (!event.isOwnDriver()) {
            return switch (fase) {
                case "ENTERING" -> event.getDriverName() + " entra a boxes.";
                case "EXITING" -> event.getDriverName() + " sale de boxes en P"
                        + event.getCount() + ".";
                default -> "";
            };
        }
        return switch (fase) {
            case "ENTERING" -> "Box, box, box. Confirma.";
            case "STOPPED" -> "Estamos contigo. " + event.getExtra() + ".";
            case "EXITING" -> "Fuera. P" + event.getCount() + ", empuja ahora.";
            case "COMPLETED" -> String.format(Locale.ROOT, "Parada de %.1f s. %s",
                    event.getValue(), event.getExtra());
            default -> "";
        };
    }

    private String tireChangeTexto(RaceEvent event) {
        String compuestoEtiqueta = event.getDetail();
        if (!event.isOwnDriver()) {
            return event.getDriverName() + " monta " + compuestoEtiqueta + ".";
        }
        return "Montamos " + compuestoEtiqueta + ". " + event.getExtra();
    }

    /**
     * La bandera a cuadros es el único momento que no habla el ingeniero: es
     * la voz del narrador, con el registro de un locutor deportivo dando el
     * resultado en directo. El resto de la radio —boxes, neumáticos,
     * telemetría— sigue siendo la conversación técnica de siempre.
     */
    private String sessionEndTexto(RaceEvent event) {
        int posicion = event.getCount();
        String piloto = event.getDriverName();
        String equipo = event.getDetail();
        String conEquipo = equipo == null || equipo.isBlank() ? "" : ", de " + equipo + ",";
        return switch (posicion) {
            case 1 -> "¡Bandera a cuadros! ¡" + piloto + conEquipo
                    + " se lleva la pole position con una vuelta espectacular!";
            case 2 -> "¡Bandera a cuadros! " + piloto + conEquipo
                    + " certifica la primera fila, P2 de la parrilla.";
            case 3 -> "¡Bandera a cuadros! " + piloto + conEquipo
                    + " se cuela en el top tres, P3 de la parrilla.";
            default -> "Bandera a cuadros para " + piloto + conEquipo
                    + ". Termina en la P" + posicion + " de la parrilla.";
        };
    }

    private String etiquetaSector(TrackSector sector) {
        return sector == null || sector == TrackSector.NONE
                ? "pista" : sector.getEtiqueta().toLowerCase(Locale.ROOT);
    }
}
