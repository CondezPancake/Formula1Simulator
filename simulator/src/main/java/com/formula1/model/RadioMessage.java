package com.formula1.model;

import java.util.Objects;

/**
 * Un mensaje de la radio del equipo.
 *
 * <p>La radio real de la Fórmula 1 habla en titulares —instrucción, número y
 * acuse— y alterna dos voces: el ingeniero de pista y el piloto. Ese es el
 * contrato que modela este record: quién habla, qué dice, en qué punto de la
 * vuelta y con cuánta urgencia.
 *
 * <p>La {@link Prioridad} no es decoración: decide qué mensajes merecen
 * interrumpir con el rótulo sobre el mapa y un aviso sonoro, y cuáles se
 * quedan en el historial lateral.
 */
public record RadioMessage(Emisor emisor, String texto, int segmento,
                           TrackSector sector, Prioridad prioridad) {

    public RadioMessage {
        Objects.requireNonNull(emisor, "El emisor es obligatorio");
        Objects.requireNonNull(prioridad, "La prioridad es obligatoria");
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("Un mensaje de radio no puede ir vacío");
        }
        sector = sector == null ? TrackSector.NONE : sector;
    }

    /** Mensaje del ingeniero, que es quien lleva la voz cantante. */
    public static RadioMessage ingeniero(String texto, int segmento, TrackSector sector,
                                         Prioridad prioridad) {
        return new RadioMessage(Emisor.INGENIERO, texto, segmento, sector, prioridad);
    }

    /** Acuse o comentario del piloto; siempre rutina, nunca interrumpe. */
    public static RadioMessage piloto(String texto, int segmento, TrackSector sector) {
        return new RadioMessage(Emisor.PILOTO, texto, segmento, sector, Prioridad.RUTINA);
    }

    /** Si el mensaje merece rótulo sobre el mapa y aviso sonoro. */
    public boolean interrumpe() {
        return prioridad != Prioridad.RUTINA;
    }

    /** Quién habla. */
    public enum Emisor {
        INGENIERO("Ingeniero"),
        PILOTO("Piloto");

        private final String etiqueta;

        Emisor(String etiqueta) {
            this.etiqueta = etiqueta;
        }

        public String getEtiqueta() {
            return etiqueta;
        }
    }

    /** Cuánto urge. */
    public enum Prioridad {
        /** Información de seguimiento: solo al historial. */
        RUTINA,
        /** Merece que el piloto levante la vista: rótulo y aviso. */
        IMPORTANTE,
        /** Bandera roja, abandono o box inmediato. */
        CRITICA
    }
}
