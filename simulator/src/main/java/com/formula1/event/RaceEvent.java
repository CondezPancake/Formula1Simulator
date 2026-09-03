package com.formula1.event;

import com.formula1.model.RadioMessage.Prioridad;
import com.formula1.model.TrackSector;

/**
 * Envoltorio de datos que {@code RaceRadioService} entrega a
 * {@code RaceNarratorService} para que ponga las palabras.
 *
 * <p>El significado de {@code detail} y {@code extra} depende del
 * {@link RaceEventType}: son huecos de plantilla, no texto ya redactado.
 * Las fábricas estáticas documentan qué va en cada uno por tipo de suceso.
 */
public final class RaceEvent {

    private final RaceEventType type;
    private final String driverName;
    private final String detail;
    private final String extra;
    private final double value;
    private final int count;
    private final int lap;
    private final TrackSector sector;
    private final Prioridad priority;
    private final boolean ownDriver;

    public RaceEvent(RaceEventType type, String driverName, String detail, String extra,
                      double value, int count, int lap, TrackSector sector, Prioridad priority,
                      boolean ownDriver) {
        this.type = type;
        this.driverName = driverName;
        this.detail = detail;
        this.extra = extra;
        this.value = value;
        this.count = count;
        this.lap = lap;
        this.sector = sector;
        this.priority = priority;
        this.ownDriver = ownDriver;
    }

    public static RaceEvent radioCheck(String codigoPiloto) {
        return new RaceEvent(RaceEventType.RADIO_CHECK, codigoPiloto, null, null, 0, 0, 1,
                TrackSector.SECTOR_1, Prioridad.RUTINA, true);
    }

    /** {@code severidad}: {@code "AVISO"} o {@code "CRITICO"}. */
    public static RaceEvent tireWear(String severidad, int segmento, TrackSector sector,
                                      Prioridad prioridad) {
        return new RaceEvent(RaceEventType.TIRE_WEAR, null, severidad, null, 0, 0, segmento,
                sector, prioridad, true);
    }

    public static RaceEvent fuel(String severidad, double porcentaje, int segmento,
                                  TrackSector sector, Prioridad prioridad) {
        return new RaceEvent(RaceEventType.FUEL_LEVEL, null, severidad, null, porcentaje, 0,
                segmento, sector, prioridad, true);
    }

    public static RaceEvent engineTemp(String severidad, double temperatura, int segmento,
                                        TrackSector sector, Prioridad prioridad) {
        return new RaceEvent(RaceEventType.ENGINE_TEMPERATURE, null, severidad, null, temperatura,
                0, segmento, sector, prioridad, true);
    }

    /** {@code bandera}: {@code GREEN}, {@code LOCAL_YELLOW}, {@code YELLOW} o {@code RED}. */
    public static RaceEvent flag(String bandera, int segmento, TrackSector sector,
                                  Prioridad prioridad) {
        return new RaceEvent(RaceEventType.FLAG_CHANGE, null, bandera, null, 0, 0, segmento,
                sector, prioridad, true);
    }

    /** {@code movimiento}: {@code POLE}, {@code GANADA}, {@code PERDIDA} o {@code IGUAL}. */
    public static RaceEvent position(String movimiento, int posicion, double gap, int segmento,
                                      TrackSector sector, Prioridad prioridad) {
        return new RaceEvent(RaceEventType.POSITION_UPDATE, null, movimiento, null, gap, posicion,
                segmento, sector, prioridad, true);
    }

    /**
     * {@code fase}: {@code ENTERING}, {@code STOPPED}, {@code EXITING} o {@code COMPLETED}.
     * {@code motivoOBalance}: el motivo de la parada (fase STOPPED) o el balance de
     * posiciones (fase COMPLETED); no se usa en las otras fases.
     */
    public static RaceEvent pitStop(String fase, String piloto, boolean propio, int posicionActual,
                                     double tiempoDetenidoSegundos, String motivoOBalance,
                                     int segmento, TrackSector sector, Prioridad prioridad) {
        return new RaceEvent(RaceEventType.PIT_STOP, piloto, fase, motivoOBalance,
                tiempoDetenidoSegundos, posicionActual, segmento, sector, prioridad, propio);
    }

    public static RaceEvent tireChange(String piloto, boolean propio, String compuestoEtiqueta,
                                        String lectura, int segmento, TrackSector sector,
                                        Prioridad prioridad) {
        return new RaceEvent(RaceEventType.TIRE_CHANGE, piloto, compuestoEtiqueta, lectura, 0, 0,
                segmento, sector, prioridad, propio);
    }

    /** {@code equipo} llega en {@code detail}: hace falta para la llamada final del narrador. */
    public static RaceEvent sessionEnd(int posicion, String piloto, String equipo, int totalSegmentos) {
        return new RaceEvent(RaceEventType.SESSION_END, piloto, equipo, null, 0, posicion,
                totalSegmentos, TrackSector.SECTOR_3, Prioridad.IMPORTANTE, true);
    }

    public RaceEventType getType() {
        return type;
    }

    public String getDriverName() {
        return driverName;
    }

    public String getDetail() {
        return detail;
    }

    public String getExtra() {
        return extra;
    }

    public double getValue() {
        return value;
    }

    public int getCount() {
        return count;
    }

    public int getLap() {
        return lap;
    }

    public TrackSector getSector() {
        return sector;
    }

    public Prioridad getPriority() {
        return priority;
    }

    public boolean isOwnDriver() {
        return ownDriver;
    }
}
