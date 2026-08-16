package com.formula1.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Sesión de clasificación ya disputada: el clima que tocó, la configuración
 * empleada y la parrilla resultante. Es la unidad que se guarda en el
 * historial.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class QualifyingSession {

    private String id;
    private String circuito;
    private WeatherCondition clima;
    private SimulationConfig config;
    private List<LapResult> resultados;
    private String fecha;

    public QualifyingSession() {
        this.id = UUID.randomUUID().toString();
        this.resultados = new ArrayList<>();
    }

    public QualifyingSession(String circuito, WeatherCondition clima, SimulationConfig config) {
        this();
        this.circuito = circuito;
        this.clima = clima;
        this.config = config;
    }

    /** Piloto que logró la pole, o {@code null} si la sesión está vacía. */
    @JsonIgnore
    public LapResult getPole() {
        return resultados.isEmpty() ? null : resultados.get(0);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCircuito() {
        return circuito;
    }

    public void setCircuito(String circuito) {
        this.circuito = circuito;
    }

    public WeatherCondition getClima() {
        return clima;
    }

    public void setClima(WeatherCondition clima) {
        this.clima = clima;
    }

    public SimulationConfig getConfig() {
        return config;
    }

    public void setConfig(SimulationConfig config) {
        this.config = config;
    }

    public List<LapResult> getResultados() {
        return resultados;
    }

    public void setResultados(List<LapResult> resultados) {
        this.resultados = resultados;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QualifyingSession)) return false;
        return Objects.equals(id, ((QualifyingSession) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return circuito + " — " + fecha;
    }
}
