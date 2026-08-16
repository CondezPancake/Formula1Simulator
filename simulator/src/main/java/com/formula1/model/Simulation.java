package com.formula1.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Simulation {

    private String id;
    private SimulationConfig config;
    private Weather weather;
    private SessionPhase fase;
    private List<Result> resultados;
    private LocalDateTime iniciadoEn;
    private LocalDateTime finalizadoEn;

    public Simulation() {
        this.resultados = new ArrayList<>();
    }

    public Simulation(String id, SimulationConfig config, Weather weather, SessionPhase fase) {
        this();
        this.id = id;
        this.config = config;
        this.weather = weather;
        this.fase = fase;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public SimulationConfig getConfig() {
        return config;
    }

    public void setConfig(SimulationConfig config) {
        this.config = config;
    }

    public Weather getWeather() {
        return weather;
    }

    public void setWeather(Weather weather) {
        this.weather = weather;
    }

    public SessionPhase getFase() {
        return fase;
    }

    public void setFase(SessionPhase fase) {
        this.fase = fase;
    }

    public List<Result> getResultados() {
        return resultados;
    }

    public void setResultados(List<Result> resultados) {
        this.resultados = resultados;
    }

    public LocalDateTime getIniciadoEn() {
        return iniciadoEn;
    }

    public void setIniciadoEn(LocalDateTime iniciadoEn) {
        this.iniciadoEn = iniciadoEn;
    }

    public LocalDateTime getFinalizadoEn() {
        return finalizadoEn;
    }

    public void setFinalizadoEn(LocalDateTime finalizadoEn) {
        this.finalizadoEn = finalizadoEn;
    }

    @Override
    public String toString() {
        return "Simulation{" +
                "id='" + id + '\'' +
                ", fase=" + fase +
                ", resultados=" + resultados.size() +
                '}';
    }
}
