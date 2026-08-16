package com.formula1.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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

    public LapResult() {
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

    @Override
    public String toString() {
        return posicion + ". " + piloto + " (" + equipo + ")";
    }
}
