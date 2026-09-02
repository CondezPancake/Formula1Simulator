package com.formula1.service;

import com.formula1.model.EventOccurrence;
import com.formula1.model.EventType;
import com.formula1.model.LapResult;
import com.formula1.model.PitStopRecord;
import com.formula1.model.RadioMessage;
import com.formula1.model.RadioMessage.Prioridad;
import com.formula1.model.TelemetrySnapshot;
import com.formula1.model.TireChangeRecord;
import com.formula1.model.TrackFlag;
import com.formula1.model.TrackSector;
import com.formula1.util.FormatUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Convierte lo que emite el motor en radio del equipo.
 *
 * <p>Está calcado del habla real del muro de boxes, que es telegráfica:
 * instrucción, número y acuse. Nada de frases largas, porque en la vida real
 * el piloto las oye a 300 km/h; y el ingeniero solo abre el canal cuando tiene
 * algo que decir, así que aquí también se calla si no hay noticia.
 *
 * <p>No inventa datos: cada frase nace de una señal que el motor ya produce
 * —posición, gap, sector, desgaste, combustible, temperaturas, clima, bandera,
 * evento, parada o cambio de neumático— y se limita a ponerle palabras.
 *
 * <p>Sin dependencias de JavaFX a propósito: es lógica pura y se prueba sin
 * levantar el toolkit, igual que el resto del paquete {@code service}.
 */
public final class RaceRadioService {

    /** Por debajo de esto los neumáticos ya no dan lo que prometen. */
    private static final double DESGASTE_AVISO = 60;
    private static final double DESGASTE_CRITICO = 80;
    private static final double COMBUSTIBLE_AVISO = 25;
    private static final double COMBUSTIBLE_CRITICO = 12;
    private static final double MOTOR_AVISO = 105;
    private static final double MOTOR_CRITICO = 120;

    /** Estado del piloto seguido, para no repetir el mismo aviso cada segmento. */
    private int ultimaPosicion;
    private boolean avisadoDesgaste;
    private boolean avisadoCombustible;
    private boolean avisadoMotor;
    private TrackFlag ultimaBandera = TrackFlag.GREEN;
    private boolean saludoHecho;

    /** Olvida lo dicho. Se llama al arrancar cada sesión. */
    public void reiniciar() {
        ultimaPosicion = 0;
        avisadoDesgaste = false;
        avisadoCombustible = false;
        avisadoMotor = false;
        ultimaBandera = TrackFlag.GREEN;
        saludoHecho = false;
    }

    /** Comprobación de radio del principio, como en cada sesión real. */
    public List<RadioMessage> saludoInicial(String codigoPiloto) {
        if (saludoHecho) {
            return List.of();
        }
        saludoHecho = true;
        return List.of(
                RadioMessage.ingeniero("Radio check, " + codigoPiloto + ". ¿Me copias?",
                        1, TrackSector.SECTOR_1, Prioridad.RUTINA),
                RadioMessage.piloto("Alto y claro.", 1, TrackSector.SECTOR_1));
    }

    /**
     * Mensajes derivados de la telemetría del piloto seguido.
     *
     * <p>Llega una muestra por segmento, veinte por sesión, así que cada aviso
     * se da <b>una sola vez</b> mientras dure la condición: un ingeniero que
     * repitiese "los neumáticos están cayendo" veinte veces sería ruido.
     */
    public List<RadioMessage> desdeTelemetria(TelemetrySnapshot muestra) {
        if (muestra == null) {
            return List.of();
        }
        List<RadioMessage> mensajes = new ArrayList<>();
        int segmento = muestra.segmento();
        TrackSector sector = TrackSector.desdeSegmento(
                Math.max(1, segmento), Math.max(1, muestra.totalSegmentos()));

        double desgaste = muestra.desgasteNeumaticosPorcentaje();
        if (!avisadoDesgaste && desgaste >= DESGASTE_CRITICO) {
            avisadoDesgaste = true;
            mensajes.add(RadioMessage.ingeniero(
                    "Neumáticos al límite. Gestiona la salida de curva.",
                    segmento, sector, Prioridad.IMPORTANTE));
            mensajes.add(RadioMessage.piloto("Recibido, no tengo agarre atrás.",
                    segmento, sector));
        } else if (!avisadoDesgaste && desgaste >= DESGASTE_AVISO) {
            avisadoDesgaste = true;
            mensajes.add(RadioMessage.ingeniero(
                    "Los neumáticos están cayendo. Cuida la tracción.",
                    segmento, sector, Prioridad.RUTINA));
        }

        double combustible = muestra.combustibleRestantePorcentaje();
        if (!avisadoCombustible && combustible <= COMBUSTIBLE_CRITICO) {
            avisadoCombustible = true;
            mensajes.add(RadioMessage.ingeniero(
                    "Vamos justos de combustible. Levanta y arrastra en la recta.",
                    segmento, sector, Prioridad.IMPORTANTE));
        } else if (!avisadoCombustible && combustible <= COMBUSTIBLE_AVISO) {
            avisadoCombustible = true;
            mensajes.add(RadioMessage.ingeniero(
                    String.format(Locale.ROOT, "Combustible al %.0f %%. Modo ahorro.",
                            combustible),
                    segmento, sector, Prioridad.RUTINA));
        }

        double motor = muestra.temperaturaMotorC();
        if (!avisadoMotor && motor >= MOTOR_CRITICO) {
            avisadoMotor = true;
            mensajes.add(RadioMessage.ingeniero(
                    String.format(Locale.ROOT, "Temperatura de motor %.0f grados. Baja el modo.",
                            motor),
                    segmento, sector, Prioridad.CRITICA));
        } else if (!avisadoMotor && motor >= MOTOR_AVISO) {
            avisadoMotor = true;
            mensajes.add(RadioMessage.ingeniero(
                    "Temperatura de motor alta. Vigílala.",
                    segmento, sector, Prioridad.RUTINA));
        }

        TrackFlag bandera = muestra.evento().impacto().bandera();
        if (bandera != ultimaBandera) {
            ultimaBandera = bandera;
            banderaEnRadio(bandera, segmento, sector).ifPresent(mensajes::add);
        }
        return mensajes;
    }

    private Optional<RadioMessage> banderaEnRadio(TrackFlag bandera, int segmento,
                                                  TrackSector sector) {
        return switch (bandera) {
            case GREEN -> Optional.of(RadioMessage.ingeniero(
                    "Pista libre. Verde, verde.", segmento, sector, Prioridad.RUTINA));
            case LOCAL_YELLOW -> Optional.of(RadioMessage.ingeniero(
                    "Amarilla local en " + etiquetaSector(sector) + ". Atento.",
                    segmento, sector, Prioridad.IMPORTANTE));
            case YELLOW -> Optional.of(RadioMessage.ingeniero(
                    "Bandera amarilla en " + etiquetaSector(sector) + ". Levanta el pie.",
                    segmento, sector, Prioridad.IMPORTANTE));
            case RED -> Optional.of(RadioMessage.ingeniero(
                    "Bandera roja. Bandera roja. Entra a boxes.",
                    segmento, sector, Prioridad.CRITICA));
        };
    }

    /**
     * Seguimiento de posición y diferencia con el de delante.
     *
     * <p>Solo habla cuando la posición cambia: el gap por sí solo no es noticia
     * en una clasificación, pero ganar o perder un puesto sí.
     */
    public List<RadioMessage> desdeClasificacion(List<LapResult> clasificacion, int pilotoId,
                                                 int segmento) {
        if (clasificacion == null || clasificacion.isEmpty()) {
            return List.of();
        }
        Optional<LapResult> propio = clasificacion.stream()
                .filter(r -> r.getPilotoId() == pilotoId)
                .findFirst();
        if (propio.isEmpty()) {
            return List.of();
        }
        LapResult resultado = propio.get();
        int posicion = resultado.getPosicion();
        if (posicion == ultimaPosicion) {
            return List.of();
        }
        boolean mejora = ultimaPosicion != 0 && posicion < ultimaPosicion;
        boolean empeora = ultimaPosicion != 0 && posicion > ultimaPosicion;
        ultimaPosicion = posicion;

        TrackSector sector = TrackSector.desdeSegmento(Math.max(1, segmento), 20);
        String gap = posicion == 1 ? "" : " Diferencia con la pole "
                + FormatUtils.formatGap(resultado.getGap()) + ".";

        if (posicion == 1) {
            return List.of(RadioMessage.ingeniero(
                    "P1. Estás en la pole provisional." + gap,
                    segmento, sector, Prioridad.IMPORTANTE));
        }
        if (mejora) {
            return List.of(RadioMessage.ingeniero(
                    "Buen trabajo, subes a P" + posicion + "." + gap,
                    segmento, sector, Prioridad.RUTINA));
        }
        if (empeora) {
            return List.of(RadioMessage.ingeniero(
                    "Te han pasado. Ahora P" + posicion + "." + gap,
                    segmento, sector, Prioridad.IMPORTANTE));
        }
        return List.of(RadioMessage.ingeniero(
                "Vas P" + posicion + "." + gap, segmento, sector, Prioridad.RUTINA));
    }

    /**
     * Traduce una incidencia de pista a radio.
     *
     * <p>El catálogo tiene veintinueve tipos; agruparlos por categoría evita
     * un switch de veintinueve ramas que envejecería mal cada vez que el
     * catálogo crezca, y da igualmente una frase con sentido para cada uno.
     */
    public Optional<RadioMessage> desdeEvento(EventOccurrence evento, int pilotoSeguido) {
        if (evento == null || !evento.ocurrio()) {
            return Optional.empty();
        }
        boolean propio = evento.pilotoId() == pilotoSeguido;
        boolean global = evento.alcance() == com.formula1.model.EventScope.GLOBAL;
        if (!propio && !global) {
            return Optional.empty();
        }
        String sector = etiquetaSector(evento.sector());
        String texto = switch (evento.tipo()) {
            case PERFECT_LAP -> "Vuelta perfecta. Eso es exactamente lo que buscábamos.";
            case CLEAN_AIR -> "Tienes aire limpio delante. Aprovecha ahora.";
            case SLIPSTREAM -> "Rebufo disponible en la recta. Pégate.";
            case TRACK_EVOLUTION_ADVANTAGE -> "La pista está mejorando. Hay tiempo ahí fuera.";
            case STRONG_SECTOR -> "Gran " + sector + ". Sigue así.";
            case TRAFFIC -> "Tráfico delante en " + sector + ". Busca hueco.";
            case HEAVY_TRAFFIC -> "Tráfico intenso en " + sector + ". Aborta la vuelta si hace falta.";
            case DRIVER_MISTAKE -> "Sin problema, olvídalo. Vamos a por la siguiente.";
            case LOCK_UP -> "Has bloqueado. Cuidado con el plano en el delantero.";
            case WHEELSPIN -> "Patinaste a la salida. Suaviza el gas.";
            case WIDE_CORNER -> "Te has abierto en " + sector + ". Ajusta la referencia.";
            case OVERSTEER -> "Se te va de atrás. Subimos el ala trasera en la próxima.";
            case UNDERSTEER -> "Mucho subviraje. Prueba a entrar más despacio.";
            case TYRE_OVERHEATING -> "Neumáticos sobrecalentados. Deja un hueco y enfríalos.";
            case TYRE_TOO_COLD -> "Los neumáticos están fríos. Zigzaguea para meterles temperatura.";
            case BRAKE_OVERHEATING -> "Frenos calientes. Frena más pronto y más suave.";
            case ENGINE_TEMPERATURE_HIGH -> "Motor caliente. Vamos a bajar el modo.";
            case MINOR_MECHANICAL_ISSUE -> "Vemos algo raro en los datos. Estamos revisando, mantén el ritmo.";
            case POWER_UNIT_DERATING -> "Estamos perdiendo potencia. Gestiona el despliegue.";
            case YELLOW_FLAG -> "Bandera amarilla en " + sector + ". Levanta el pie.";
            case LOCAL_YELLOW_FLAG -> "Amarilla local en " + sector + ". Atento al salir de la curva.";
            case RAIN_STARTS -> "Empieza a caer agua en " + sector + ".";
            case RAIN_INTENSIFIES -> "La lluvia va a más. Cuidado con el agarre.";
            case RAIN_STOPS -> "Deja de llover. La pista se va a secar.";
            case TRACK_DRYING -> "La pista se seca. Habrá tiempo en las próximas vueltas.";
            case WIND_GUST -> "Racha de viento en " + sector + ". Ojo en la frenada.";
            case RED_FLAG -> "Bandera roja. Bandera roja. Reduce y vuelve a boxes.";
            case CRASH -> "Accidente en " + sector + ". Ve con mucho cuidado.";
            case NO_EVENT -> null;
        };
        if (texto == null) {
            return Optional.empty();
        }
        // El piloto ajeno se narra en tercera persona: no es una orden para ti.
        if (!propio) {
            texto = evento.piloto() + ": " + evento.tipo().getEtiqueta().toLowerCase(Locale.ROOT)
                    + " en " + sector + ".";
        }
        return Optional.of(RadioMessage.ingeniero(texto, evento.vuelta(), evento.sector(),
                prioridadDe(evento.tipo())));
    }

    private Prioridad prioridadDe(EventType tipo) {
        return switch (tipo.getCategoria()) {
            case EXCEPTIONAL -> Prioridad.CRITICA;
            case MAJOR_NEGATIVE, WEATHER_TRACK -> Prioridad.IMPORTANTE;
            default -> Prioridad.RUTINA;
        };
    }

    /** La parada, fase a fase, con el acuse que siempre acompaña al "box". */
    public List<RadioMessage> desdeParada(PitStopRecord parada, int pilotoSeguido) {
        if (parada == null) {
            return List.of();
        }
        boolean propio = parada.pilotoId() == pilotoSeguido;
        int segmento = parada.segmentoActual();
        TrackSector sector = TrackSector.desdeSegmento(Math.max(1, segmento), 20);

        if (!propio) {
            return switch (parada.fase()) {
                case ENTERING -> List.of(RadioMessage.ingeniero(
                        parada.piloto() + " entra a boxes.", segmento, sector, Prioridad.RUTINA));
                case EXITING -> List.of(RadioMessage.ingeniero(
                        parada.piloto() + " sale de boxes en P" + parada.posicionActual() + ".",
                        segmento, sector, Prioridad.RUTINA));
                default -> List.of();
            };
        }

        return switch (parada.fase()) {
            case ENTERING -> List.of(
                    RadioMessage.ingeniero("Box, box, box. Confirma.",
                            segmento, sector, Prioridad.CRITICA),
                    RadioMessage.piloto("Box confirmado.", segmento, sector));
            case STOPPED -> List.of(RadioMessage.ingeniero(
                    "Estamos contigo. " + parada.motivo().getEtiqueta().toLowerCase(Locale.ROOT) + ".",
                    segmento, sector, Prioridad.IMPORTANTE));
            case EXITING -> List.of(RadioMessage.ingeniero(
                    "Fuera. P" + parada.posicionActual() + ", empuja ahora.",
                    segmento, sector, Prioridad.IMPORTANTE));
            case COMPLETED -> List.of(RadioMessage.ingeniero(
                    String.format(Locale.ROOT, "Parada de %.1f s. %s",
                            parada.tiempoDetenidoSegundos(), balanceDeParada(parada)),
                    segmento, sector, Prioridad.RUTINA));
        };
    }

    private String balanceDeParada(PitStopRecord parada) {
        int perdidas = parada.posicionesPerdidas();
        if (perdidas <= 0) {
            return "No has perdido posiciones.";
        }
        return perdidas == 1 ? "Has perdido una posición."
                : "Has perdido " + perdidas + " posiciones.";
    }

    /** Cambio de compuesto, con la lectura de para qué sirve el que entra. */
    public Optional<RadioMessage> desdeCambioNeumaticos(TireChangeRecord cambio,
                                                        int pilotoSeguido) {
        if (cambio == null) {
            return Optional.empty();
        }
        TrackSector sector = TrackSector.desdeSegmento(Math.max(1, cambio.segmento()), 20);
        if (cambio.pilotoId() != pilotoSeguido) {
            return Optional.of(RadioMessage.ingeniero(
                    cambio.piloto() + " monta " + cambio.nuevo().getEtiqueta().toLowerCase(Locale.ROOT)
                            + ".", cambio.segmento(), sector, Prioridad.RUTINA));
        }
        String lectura = switch (cambio.nuevo()) {
            case SOFT -> "Máximo agarre, pero dura poco. Ve a por la vuelta ya.";
            case MEDIUM -> "Compromiso. Puedes empujar sin castigarlos.";
            case HARD -> "Tardan en entrar en temperatura. Dales una vuelta.";
        };
        return Optional.of(RadioMessage.ingeniero(
                "Montamos " + cambio.nuevo().getEtiqueta().toLowerCase(Locale.ROOT) + ". " + lectura,
                cambio.segmento(), sector, Prioridad.IMPORTANTE));
    }

    /** Cierre de sesión: lo primero que se oye al cruzar la meta. */
    public List<RadioMessage> cierreDeSesion(LapResult resultado, int totalSegmentos) {
        if (resultado == null) {
            return List.of(RadioMessage.ingeniero(
                    "Sesión terminada. Vuelve al garaje.",
                    totalSegmentos, TrackSector.SECTOR_3, Prioridad.RUTINA));
        }
        int posicion = resultado.getPosicion();
        String texto = posicion == 1
                ? "¡Eso es la pole! Vuelta espectacular, tío."
                : posicion <= 3
                        ? "P" + posicion + ". Muy buena vuelta, estamos ahí."
                        : "P" + posicion + ". Buen trabajo, lo miramos dentro.";
        List<RadioMessage> cierre = new ArrayList<>();
        cierre.add(RadioMessage.ingeniero(texto, totalSegmentos, TrackSector.SECTOR_3,
                Prioridad.IMPORTANTE));
        cierre.add(RadioMessage.piloto(posicion == 1
                        ? "¡Vamos! Gran trabajo todos."
                        : "Recibido. Gracias, chicos.",
                totalSegmentos, TrackSector.SECTOR_3));
        return cierre;
    }

    private String etiquetaSector(TrackSector sector) {
        return sector == null || sector == TrackSector.NONE
                ? "pista" : sector.getEtiqueta().toLowerCase(Locale.ROOT);
    }
}
