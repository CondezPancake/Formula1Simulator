package com.formula1.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Monoplaza. El modelo es la clave natural (RB20, W15, ...).
 *
 * El rendimiento se modela como un mapa {@code modo -> Performance}, y
 * dentro de cada uno el consumo y el desgaste son mapas
 * {@code clima -> valor}. Es exactamente la forma que tiene el JSON de la
 * especificación, y permite resolver la fórmula sin un solo {@code switch}:
 * {@code vehiculo.rendimientoDe(modo).consumoCon(clima)}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Vehicle {

    private String modelo;
    private String equipo;
    private String motor;

    @JsonProperty("velocidad_maxima_kmh")
    private int velocidadMaximaKmh;

    @JsonProperty("aceleracion_0_100")
    private double aceleracion0100;

    private List<Integer> pilotos;
    private Map<DrivingMode, Performance> rendimiento;
    private String imagen;

    public Vehicle() {
        this.pilotos = new ArrayList<>();
        this.rendimiento = new EnumMap<>(DrivingMode.class);
    }

    public Vehicle(String modelo, String equipo, String motor, int velocidadMaximaKmh, double aceleracion0100) {
        this();
        this.modelo = modelo;
        this.equipo = equipo;
        this.motor = motor;
        this.velocidadMaximaKmh = velocidadMaximaKmh;
        this.aceleracion0100 = aceleracion0100;
    }

    /**
     * Rendimiento en un modo concreto; si el vehículo no lo define, cae al
     * modo normal para que la simulación nunca falle por datos incompletos.
     */
    public Performance rendimientoDe(DrivingMode modo) {
        Performance performance = rendimiento.get(modo);
        return performance != null ? performance : rendimiento.get(DrivingMode.NORMAL);
    }

    public boolean conduce(int pilotoId) {
        return pilotos.contains(pilotoId);
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public String getEquipo() {
        return equipo;
    }

    public void setEquipo(String equipo) {
        this.equipo = equipo;
    }

    public String getMotor() {
        return motor;
    }

    public void setMotor(String motor) {
        this.motor = motor;
    }

    public int getVelocidadMaximaKmh() {
        return velocidadMaximaKmh;
    }

    public void setVelocidadMaximaKmh(int velocidadMaximaKmh) {
        this.velocidadMaximaKmh = velocidadMaximaKmh;
    }

    public double getAceleracion0100() {
        return aceleracion0100;
    }

    public void setAceleracion0100(double aceleracion0100) {
        this.aceleracion0100 = aceleracion0100;
    }

    public List<Integer> getPilotos() {
        return pilotos;
    }

    public void setPilotos(List<Integer> pilotos) {
        this.pilotos = pilotos;
    }

    public Map<DrivingMode, Performance> getRendimiento() {
        return rendimiento;
    }

    public void setRendimiento(Map<DrivingMode, Performance> rendimiento) {
        this.rendimiento = new EnumMap<>(DrivingMode.class);
        if (rendimiento != null) {
            this.rendimiento.putAll(rendimiento);
        }
    }

    public String getImagen() {
        return imagen;
    }

    public void setImagen(String imagen) {
        this.imagen = imagen;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Vehicle)) return false;
        return Objects.equals(modelo, ((Vehicle) o).modelo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modelo);
    }

    @Override
    public String toString() {
        return modelo;
    }

    /**
     * Rendimiento del vehículo en un modo de conducción: velocidad media y
     * las tablas de consumo y desgaste por condición climática.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Performance {

        @JsonProperty("velocidad_promedio_kmh")
        private int velocidadPromedioKmh;

        @JsonProperty("consumo_combustible")
        private Map<WeatherCondition, Double> consumo;

        @JsonProperty("desgaste_neumaticos")
        private Map<WeatherCondition, Double> desgaste;

        public Performance() {
            this.consumo = new EnumMap<>(WeatherCondition.class);
            this.desgaste = new EnumMap<>(WeatherCondition.class);
        }

        public Performance(int velocidadPromedioKmh) {
            this();
            this.velocidadPromedioKmh = velocidadPromedioKmh;
        }

        public double consumoCon(WeatherCondition clima) {
            return consumo.getOrDefault(clima, 2.0);
        }

        public double desgasteCon(WeatherCondition clima) {
            return desgaste.getOrDefault(clima, 1.5);
        }

        public int getVelocidadPromedioKmh() {
            return velocidadPromedioKmh;
        }

        public void setVelocidadPromedioKmh(int velocidadPromedioKmh) {
            this.velocidadPromedioKmh = velocidadPromedioKmh;
        }

        public Map<WeatherCondition, Double> getConsumo() {
            return consumo;
        }

        public void setConsumo(Map<WeatherCondition, Double> consumo) {
            this.consumo = new EnumMap<>(WeatherCondition.class);
            if (consumo != null) {
                this.consumo.putAll(consumo);
            }
        }

        public Map<WeatherCondition, Double> getDesgaste() {
            return desgaste;
        }

        public void setDesgaste(Map<WeatherCondition, Double> desgaste) {
            this.desgaste = new EnumMap<>(WeatherCondition.class);
            if (desgaste != null) {
                this.desgaste.putAll(desgaste);
            }
        }
    }
}
