package com.formula1.domain.service;

import com.formula1.domain.event.RaceEvent;
import com.formula1.domain.model.EventOccurrence;
import com.formula1.domain.model.EventType;
import com.formula1.domain.model.LapResult;
import com.formula1.domain.model.PitStopRecord;
import com.formula1.domain.model.RadioMessage;
import com.formula1.domain.model.RadioMessage.Prioridad;
import com.formula1.domain.model.TelemetrySnapshot;
import com.formula1.domain.model.TireChangeRecord;
import com.formula1.domain.model.TrackFlag;
import com.formula1.domain.model.TrackSector;

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
 * evento, parada o cambio de neumático—; esta clase decide cuándo hablar y
 * con qué urgencia, y le pasa esa decisión ya empaquetada en un
 * {@link RaceEvent} a {@link RaceNarratorService}, que es quien pone las
 * palabras.
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

    private final RaceNarratorService narratorService;

    /** Estado del piloto seguido, para no repetir el mismo aviso cada segmento. */
    private int ultimaPosicion;
    private boolean avisadoDesgaste;
    private boolean avisadoCombustible;
    private boolean avisadoMotor;
    private TrackFlag ultimaBandera = TrackFlag.GREEN;
    private boolean saludoHecho;

    public RaceRadioService() {
        this(new RaceNarratorService());
    }

    public RaceRadioService(RaceNarratorService narratorService) {
        this.narratorService = narratorService;
    }

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
        String texto = narratorService.generate(RaceEvent.radioCheck(codigoPiloto));
        return List.of(
                RadioMessage.ingeniero(texto, 1, TrackSector.SECTOR_1, Prioridad.RUTINA),
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
            mensajes.add(hablar(RaceEvent.tireWear("CRITICO", segmento, sector, Prioridad.IMPORTANTE)));
            mensajes.add(RadioMessage.piloto("Recibido, no tengo agarre atrás.",
                    segmento, sector));
        } else if (!avisadoDesgaste && desgaste >= DESGASTE_AVISO) {
            avisadoDesgaste = true;
            mensajes.add(hablar(RaceEvent.tireWear("AVISO", segmento, sector, Prioridad.RUTINA)));
        }

        double combustible = muestra.combustibleRestantePorcentaje();
        if (!avisadoCombustible && combustible <= COMBUSTIBLE_CRITICO) {
            avisadoCombustible = true;
            mensajes.add(hablar(RaceEvent.fuel("CRITICO", combustible, segmento, sector,
                    Prioridad.IMPORTANTE)));
        } else if (!avisadoCombustible && combustible <= COMBUSTIBLE_AVISO) {
            avisadoCombustible = true;
            mensajes.add(hablar(RaceEvent.fuel("AVISO", combustible, segmento, sector,
                    Prioridad.RUTINA)));
        }

        double motor = muestra.temperaturaMotorC();
        if (!avisadoMotor && motor >= MOTOR_CRITICO) {
            avisadoMotor = true;
            mensajes.add(hablar(RaceEvent.engineTemp("CRITICO", motor, segmento, sector,
                    Prioridad.CRITICA)));
        } else if (!avisadoMotor && motor >= MOTOR_AVISO) {
            avisadoMotor = true;
            mensajes.add(hablar(RaceEvent.engineTemp("AVISO", motor, segmento, sector,
                    Prioridad.RUTINA)));
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
        Prioridad prioridad = switch (bandera) {
            case GREEN -> Prioridad.RUTINA;
            case LOCAL_YELLOW, YELLOW -> Prioridad.IMPORTANTE;
            case RED -> Prioridad.CRITICA;
        };
        return Optional.of(hablar(RaceEvent.flag(bandera.name(), segmento, sector, prioridad)));
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
        String movimiento = posicion == 1 ? "POLE" : mejora ? "GANADA" : empeora ? "PERDIDA" : "IGUAL";
        Prioridad prioridad = posicion == 1 || empeora ? Prioridad.IMPORTANTE : Prioridad.RUTINA;

        return List.of(hablar(RaceEvent.position(movimiento, posicion, resultado.getGap(),
                segmento, sector, prioridad)));
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
        boolean global = evento.alcance() == com.formula1.domain.model.EventScope.GLOBAL;
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
                case ENTERING -> List.of(hablar(RaceEvent.pitStop("ENTERING", parada.piloto(),
                        false, parada.posicionActual(), 0, null, segmento, sector,
                        Prioridad.RUTINA)));
                case EXITING -> List.of(hablar(RaceEvent.pitStop("EXITING", parada.piloto(),
                        false, parada.posicionActual(), 0, null, segmento, sector,
                        Prioridad.RUTINA)));
                default -> List.of();
            };
        }

        return switch (parada.fase()) {
            case ENTERING -> List.of(
                    hablar(RaceEvent.pitStop("ENTERING", parada.piloto(), true,
                            parada.posicionActual(), 0, null, segmento, sector, Prioridad.CRITICA)),
                    RadioMessage.piloto("Box confirmado.", segmento, sector));
            case STOPPED -> List.of(hablar(RaceEvent.pitStop("STOPPED", parada.piloto(), true,
                    parada.posicionActual(), 0, parada.motivo().getEtiqueta().toLowerCase(Locale.ROOT),
                    segmento, sector, Prioridad.IMPORTANTE)));
            case EXITING -> List.of(hablar(RaceEvent.pitStop("EXITING", parada.piloto(), true,
                    parada.posicionActual(), 0, null, segmento, sector, Prioridad.IMPORTANTE)));
            case COMPLETED -> List.of(hablar(RaceEvent.pitStop("COMPLETED", parada.piloto(), true,
                    parada.posicionActual(), parada.tiempoDetenidoSegundos(),
                    balanceDeParada(parada), segmento, sector, Prioridad.RUTINA)));
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
        String compuestoEtiqueta = cambio.nuevo().getEtiqueta().toLowerCase(Locale.ROOT);
        if (cambio.pilotoId() != pilotoSeguido) {
            return Optional.of(hablar(RaceEvent.tireChange(cambio.piloto(), false,
                    compuestoEtiqueta, null, cambio.segmento(), sector, Prioridad.RUTINA)));
        }
        String lectura = switch (cambio.nuevo()) {
            case SOFT -> "Máximo agarre, pero dura poco. Ve a por la vuelta ya.";
            case MEDIUM -> "Compromiso. Puedes empujar sin castigarlos.";
            case HARD -> "Tardan en entrar en temperatura. Dales una vuelta.";
        };
        return Optional.of(hablar(RaceEvent.tireChange(cambio.piloto(), true, compuestoEtiqueta,
                lectura, cambio.segmento(), sector, Prioridad.IMPORTANTE)));
    }

    /**
     * Cierre de sesión: lo primero que se oye al cruzar la meta.
     *
     * <p>La primera línea no es radio de equipo, es la voz del narrador
     * cantando el resultado como en una retransmisión; el piloto responde
     * después, ya en su propio canal.
     */
    public List<RadioMessage> cierreDeSesion(LapResult resultado, int totalSegmentos) {
        if (resultado == null) {
            return List.of(RadioMessage.ingeniero(
                    "Bandera a cuadros. La sesión termina sin una vuelta válida que contar.",
                    totalSegmentos, TrackSector.SECTOR_3, Prioridad.RUTINA));
        }
        int posicion = resultado.getPosicion();
        List<RadioMessage> cierre = new ArrayList<>();
        cierre.add(hablar(RaceEvent.sessionEnd(posicion, resultado.getPiloto(), resultado.getEquipo(),
                totalSegmentos)));
        cierre.add(RadioMessage.piloto(posicion == 1
                        ? "¡Vamos! Gran trabajo todos."
                        : "Recibido. Gracias, chicos.",
                totalSegmentos, TrackSector.SECTOR_3));
        return cierre;
    }

    private RadioMessage hablar(RaceEvent evento) {
        String texto = narratorService.generate(evento);
        return RadioMessage.ingeniero(texto, evento.getLap(), evento.getSector(),
                evento.getPriority());
    }

    private String etiquetaSector(TrackSector sector) {
        return sector == null || sector == TrackSector.NONE
                ? "pista" : sector.getEtiqueta().toLowerCase(Locale.ROOT);
    }
}
