package com.formula1.model;

import java.util.Objects;

public class Vehicle {

    private String id;
    private String teamId;
    private String modelo;
    private String motor;
    private double velocidadMaxima;
    private double aceleracion;
    private double rendimiento;
    private double consumo;
    private double desgaste;

    public Vehicle() {
    }

    public Vehicle(String id, String teamId, String modelo, String motor) {
        this.id = id;
        this.teamId = teamId;
        this.modelo = modelo;
        this.motor = motor;
    }

    public Vehicle(String id, String teamId, String modelo, String motor, double velocidadMaxima,
                    double aceleracion, double rendimiento, double consumo, double desgaste) {
        this(id, teamId, modelo, motor);
        this.velocidadMaxima = velocidadMaxima;
        this.aceleracion = aceleracion;
        this.rendimiento = rendimiento;
        this.consumo = consumo;
        this.desgaste = desgaste;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public String getMotor() {
        return motor;
    }

    public void setMotor(String motor) {
        this.motor = motor;
    }

    public double getVelocidadMaxima() {
        return velocidadMaxima;
    }

    public void setVelocidadMaxima(double velocidadMaxima) {
        this.velocidadMaxima = velocidadMaxima;
    }

    public double getAceleracion() {
        return aceleracion;
    }

    public void setAceleracion(double aceleracion) {
        this.aceleracion = aceleracion;
    }

    public double getRendimiento() {
        return rendimiento;
    }

    public void setRendimiento(double rendimiento) {
        this.rendimiento = rendimiento;
    }

    public double getConsumo() {
        return consumo;
    }

    public void setConsumo(double consumo) {
        this.consumo = consumo;
    }

    public double getDesgaste() {
        return desgaste;
    }

    public void setDesgaste(double desgaste) {
        this.desgaste = desgaste;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Vehicle)) return false;
        Vehicle vehicle = (Vehicle) o;
        return Objects.equals(id, vehicle.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Vehicle{" +
                "id='" + id + '\'' +
                ", teamId='" + teamId + '\'' +
                ", modelo='" + modelo + '\'' +
                ", motor='" + motor + '\'' +
                '}';
    }
}
