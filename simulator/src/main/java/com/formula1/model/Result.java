package com.formula1.model;

import java.util.ArrayList;
import java.util.List;

public class Result {

    private int posicion;
    private String driverId;
    private String teamId;
    private double tiempo;
    private List<Double> sectores;
    private TireCompound neumaticoUsado;

    public Result() {
        this.sectores = new ArrayList<>();
    }

    public Result(int posicion, String driverId, String teamId, double tiempo, TireCompound neumaticoUsado) {
        this();
        this.posicion = posicion;
        this.driverId = driverId;
        this.teamId = teamId;
        this.tiempo = tiempo;
        this.neumaticoUsado = neumaticoUsado;
    }

    public int getPosicion() {
        return posicion;
    }

    public void setPosicion(int posicion) {
        this.posicion = posicion;
    }

    public String getDriverId() {
        return driverId;
    }

    public void setDriverId(String driverId) {
        this.driverId = driverId;
    }

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public double getTiempo() {
        return tiempo;
    }

    public void setTiempo(double tiempo) {
        this.tiempo = tiempo;
    }

    public List<Double> getSectores() {
        return sectores;
    }

    public void setSectores(List<Double> sectores) {
        this.sectores = sectores;
    }

    public TireCompound getNeumaticoUsado() {
        return neumaticoUsado;
    }

    public void setNeumaticoUsado(TireCompound neumaticoUsado) {
        this.neumaticoUsado = neumaticoUsado;
    }

    @Override
    public String toString() {
        return "Result{" +
                "posicion=" + posicion +
                ", driverId='" + driverId + '\'' +
                ", teamId='" + teamId + '\'' +
                ", tiempo=" + tiempo +
                ", neumaticoUsado=" + neumaticoUsado +
                '}';
    }
}
