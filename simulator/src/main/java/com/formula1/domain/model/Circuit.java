package com.formula1.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.formula1.util.FormatUtils;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Circuito del calendario. El nombre es la clave natural.
 *
 * Además de los datos de la especificación incorpora cuatro factores de
 * simulación: la distribución de clima típica y el impacto de la pista
 * sobre el tiempo, el consumo y el desgaste.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Circuit {

    /** Factor técnico por defecto para circuitos nuevos sin récord conocido. */
    public static final double FACTOR_TECNICO_POR_DEFECTO = 1.40;

    /** Velocidad de referencia usada para derivar el factor técnico. */
    public static final double VELOCIDAD_REFERENCIA_KMH = 340.0;

    private String nombre;
    private String pais;

    @JsonProperty("longitud_km")
    private double longitudKm;

    private int vueltas;
    private String descripcion;

    @JsonProperty("record_vuelta")
    private LapRecord recordVuelta;

    private List<Winner> ganadores;
    private String imagen;

    @JsonProperty("probabilidad_clima")
    private Map<WeatherCondition, Double> probabilidadClima;

    @JsonProperty("factor_tecnico")
    private double factorTecnico;

    @JsonProperty("factor_consumo")
    private double factorConsumo;

    @JsonProperty("factor_desgaste")
    private double factorDesgaste;

    public Circuit() {
        this.ganadores = new ArrayList<>();
        this.probabilidadClima = new EnumMap<>(WeatherCondition.class);
        this.factorTecnico = FACTOR_TECNICO_POR_DEFECTO;
        this.factorConsumo = 1.0;
        this.factorDesgaste = 1.0;
    }

    public Circuit(String nombre, String pais, double longitudKm, int vueltas) {
        this();
        this.nombre = nombre;
        this.pais = pais;
        this.longitudKm = longitudKm;
        this.vueltas = vueltas;
    }

    /**
     * Deriva el factor técnico (sinuosidad) del récord real de la pista,
     * comparándolo con el tiempo que se tardaría a velocidad de referencia.
     * Así Mónaco sale ~1,98 y Monza ~1,32 sin números inventados.
     */
    @JsonIgnore
    public double calcularFactorTecnico() {
        if (recordVuelta == null || recordVuelta.getTiempoSegundos() <= 0 || longitudKm <= 0) {
            return FACTOR_TECNICO_POR_DEFECTO;
        }
        double tiempoReferencia = 3600.0 * longitudKm / VELOCIDAD_REFERENCIA_KMH;
        return recordVuelta.getTiempoSegundos() / tiempoReferencia;
    }

    /** Probabilidad de una condición climática, o 0 si no está definida. */
    public double probabilidadDe(WeatherCondition clima) {
        return probabilidadClima.getOrDefault(clima, 0.0);
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getPais() {
        return pais;
    }

    public void setPais(String pais) {
        this.pais = pais;
    }

    public double getLongitudKm() {
        return longitudKm;
    }

    public void setLongitudKm(double longitudKm) {
        this.longitudKm = longitudKm;
    }

    public int getVueltas() {
        return vueltas;
    }

    public void setVueltas(int vueltas) {
        this.vueltas = vueltas;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public LapRecord getRecordVuelta() {
        return recordVuelta;
    }

    public void setRecordVuelta(LapRecord recordVuelta) {
        this.recordVuelta = recordVuelta;
    }

    public List<Winner> getGanadores() {
        return ganadores;
    }

    public void setGanadores(List<Winner> ganadores) {
        this.ganadores = ganadores;
    }

    public String getImagen() {
        return imagen;
    }

    public void setImagen(String imagen) {
        this.imagen = imagen;
    }

    public Map<WeatherCondition, Double> getProbabilidadClima() {
        return probabilidadClima;
    }

    public void setProbabilidadClima(Map<WeatherCondition, Double> probabilidadClima) {
        this.probabilidadClima = new EnumMap<>(WeatherCondition.class);
        if (probabilidadClima != null) {
            this.probabilidadClima.putAll(probabilidadClima);
        }
    }

    public double getFactorTecnico() {
        return factorTecnico;
    }

    public void setFactorTecnico(double factorTecnico) {
        this.factorTecnico = factorTecnico;
    }

    public double getFactorConsumo() {
        return factorConsumo;
    }

    public void setFactorConsumo(double factorConsumo) {
        this.factorConsumo = factorConsumo;
    }

    public double getFactorDesgaste() {
        return factorDesgaste;
    }

    public void setFactorDesgaste(double factorDesgaste) {
        this.factorDesgaste = factorDesgaste;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Circuit)) return false;
        return Objects.equals(nombre, ((Circuit) o).nombre);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nombre);
    }

    @Override
    public String toString() {
        return nombre;
    }

    /**
     * Récord de vuelta del circuito. El tiempo se guarda en segundos porque
     * se compara con los tiempos simulados; se serializa como "1:10.166".
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LapRecord {

        private double tiempoSegundos;
        private String piloto;
        private int anio;

        public LapRecord() {
        }

        public LapRecord(double tiempoSegundos, String piloto, int anio) {
            this.tiempoSegundos = tiempoSegundos;
            this.piloto = piloto;
            this.anio = anio;
        }

        @JsonProperty("tiempo")
        public String getTiempo() {
            return FormatUtils.formatLapTime(tiempoSegundos);
        }

        @JsonProperty("tiempo")
        public void setTiempo(String tiempo) {
            this.tiempoSegundos = FormatUtils.parseLapTime(tiempo);
        }

        @JsonIgnore
        public double getTiempoSegundos() {
            return tiempoSegundos;
        }

        @JsonIgnore
        public void setTiempoSegundos(double tiempoSegundos) {
            this.tiempoSegundos = tiempoSegundos;
        }

        public String getPiloto() {
            return piloto;
        }

        public void setPiloto(String piloto) {
            this.piloto = piloto;
        }

        public int getAnio() {
            return anio;
        }

        public void setAnio(int anio) {
            this.anio = anio;
        }

        @Override
        public String toString() {
            return getTiempo() + " — " + piloto + " (" + anio + ")";
        }
    }

    /** Ganador de una temporada; el piloto se referencia por su id. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Winner {

        private int temporada;

        @JsonProperty("piloto")
        private int pilotoId;

        public Winner() {
        }

        public Winner(int temporada, int pilotoId) {
            this.temporada = temporada;
            this.pilotoId = pilotoId;
        }

        public int getTemporada() {
            return temporada;
        }

        public void setTemporada(int temporada) {
            this.temporada = temporada;
        }

        public int getPilotoId() {
            return pilotoId;
        }

        public void setPilotoId(int pilotoId) {
            this.pilotoId = pilotoId;
        }
    }
}
