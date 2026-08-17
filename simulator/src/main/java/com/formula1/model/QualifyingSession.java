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
    private List<WeatherSnapshot> evolucionClimatica;
    private List<TelemetrySnapshot> evolucionVuelta;
    private List<TrackEvolutionSnapshot> evolucionPista;
    private List<EventOccurrence> eventos;
    private String fecha;

    public QualifyingSession() {
        this.id = UUID.randomUUID().toString();
        this.resultados = new ArrayList<>();
        this.evolucionClimatica = new ArrayList<>();
        this.evolucionVuelta = new ArrayList<>();
        this.evolucionPista = new ArrayList<>();
        this.eventos = new ArrayList<>();
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
        return resultados.stream()
                .filter(LapResult::isVueltaValida)
                .findFirst()
                .orElse(null);
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

    public List<WeatherSnapshot> getEvolucionClimatica() {
        return evolucionClimatica;
    }

    public void setEvolucionClimatica(List<WeatherSnapshot> evolucionClimatica) {
        this.evolucionClimatica = evolucionClimatica == null
                ? new ArrayList<>()
                : new ArrayList<>(evolucionClimatica);
    }

    public List<EventOccurrence> getEventos() {
        return eventos == null ? List.of() : eventos;
    }

    public void setEventos(List<EventOccurrence> eventos) {
        this.eventos = eventos == null ? new ArrayList<>() : new ArrayList<>(eventos);
    }

    /**
     * Muestras ordenadas de la vuelta del piloto configurado. Se guardan con
     * la sesión para que HU-32 no dependa de callbacks transitorios de JavaFX.
     */
    public List<TelemetrySnapshot> getEvolucionVuelta() {
        return evolucionVuelta == null ? List.of() : List.copyOf(evolucionVuelta);
    }

    public void setEvolucionVuelta(List<TelemetrySnapshot> evolucionVuelta) {
        this.evolucionVuelta = evolucionVuelta == null
                ? new ArrayList<>()
                : new ArrayList<>(evolucionVuelta);
    }

    /** Evolución de goma y grip, con una lectura consolidada por vuelta. */
    public List<TrackEvolutionSnapshot> getEvolucionPista() {
        return evolucionPista == null ? List.of() : List.copyOf(evolucionPista);
    }

    public void setEvolucionPista(List<TrackEvolutionSnapshot> evolucionPista) {
        this.evolucionPista = evolucionPista == null
                ? new ArrayList<>()
                : new ArrayList<>(evolucionPista);
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
