package com.formula1.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Resultado de un piloto en la sesión de clasificación.
 *
 * Guarda los nombres de piloto, equipo y vehículo denormalizados a
 * propósito: el historial debe poder mostrarse aunque después se renombre o
 * se elimine alguna de esas entidades.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LapResult {

    private int posicion;
    private int pilotoId;
    private String piloto;
    private String equipo;
    private String vehiculo;
    private double tiempoSegundos;
    private double gap;
    private double consumoEstimado;
    private double desgasteEstimado;
    private SectorTimes sectorTimes;
    private LapStatus estadoVuelta;
    private TrackSector sectorIncidente;
    private List<EventOccurrence> eventos;

    public LapResult() {
        this.estadoVuelta = LapStatus.VALID;
        this.sectorIncidente = TrackSector.NONE;
        this.eventos = new ArrayList<>();
    }

    public LapResult(int pilotoId, String piloto, String equipo, String vehiculo, double tiempoSegundos) {
        this.pilotoId = pilotoId;
        this.piloto = piloto;
        this.equipo = equipo;
        this.vehiculo = vehiculo;
        this.tiempoSegundos = tiempoSegundos;
    }

    public int getPosicion() {
        return posicion;
    }

    public void setPosicion(int posicion) {
        this.posicion = posicion;
    }

    public int getPilotoId() {
        return pilotoId;
    }

    public void setPilotoId(int pilotoId) {
        this.pilotoId = pilotoId;
    }

    public String getPiloto() {
        return piloto;
    }

    public void setPiloto(String piloto) {
        this.piloto = piloto;
    }

    public String getEquipo() {
        return equipo;
    }

    public void setEquipo(String equipo) {
        this.equipo = equipo;
    }

    public String getVehiculo() {
        return vehiculo;
    }

    public void setVehiculo(String vehiculo) {
        this.vehiculo = vehiculo;
    }

    public double getTiempoSegundos() {
        return tiempoSegundos;
    }

    public void setTiempoSegundos(double tiempoSegundos) {
        this.tiempoSegundos = tiempoSegundos;
    }

    public double getGap() {
        return gap;
    }

    public void setGap(double gap) {
        this.gap = gap;
    }

    public double getConsumoEstimado() {
        return consumoEstimado;
    }

    public void setConsumoEstimado(double consumoEstimado) {
        this.consumoEstimado = consumoEstimado;
    }

    public double getDesgasteEstimado() {
        return desgasteEstimado;
    }

    public void setDesgasteEstimado(double desgasteEstimado) {
        this.desgasteEstimado = desgasteEstimado;
    }

    public SectorTimes getSectorTimes() {
        return sectorTimes;
    }

    public void setSectorTimes(SectorTimes sectorTimes) {
        this.sectorTimes = sectorTimes;
    }

    @JsonIgnore
    public boolean hasSectorTimes() {
        return sectorTimes != null;
    }

    public LapStatus getEstadoVuelta() {
        return estadoVuelta == null ? LapStatus.VALID : estadoVuelta;
    }

    public void setEstadoVuelta(LapStatus estadoVuelta) {
        this.estadoVuelta = estadoVuelta == null ? LapStatus.VALID : estadoVuelta;
    }

    public TrackSector getSectorIncidente() {
        return sectorIncidente == null ? TrackSector.NONE : sectorIncidente;
    }

    public void setSectorIncidente(TrackSector sectorIncidente) {
        this.sectorIncidente = sectorIncidente == null ? TrackSector.NONE : sectorIncidente;
    }

    public List<EventOccurrence> getEventos() {
        return eventos == null ? List.of() : eventos;
    }

    public void setEventos(List<EventOccurrence> eventos) {
        this.eventos = eventos == null ? new ArrayList<>() : new ArrayList<>(eventos);
    }

    @JsonIgnore
    public boolean isVueltaValida() {
        return getEstadoVuelta() == LapStatus.VALID;
    }

    @JsonIgnore
    public String getEventoResumen() {
        return getEventos().stream()
                .filter(EventOccurrence::ocurrio)
                .map(evento -> evento.tipo().getEtiqueta())
                .findFirst()
                .orElse(EventType.NO_EVENT.getEtiqueta());
    }

    @Override
    public String toString() {
        return posicion + ". " + piloto + " (" + equipo + ") · "
                + getEstadoVuelta().getEtiqueta();
    }
}
