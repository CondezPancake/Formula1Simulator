package com.formula1.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Objects;
import java.util.UUID;

/**
 * Configuración con la que el usuario afronta una sesión de clasificación.
 *
 * Se guarda automáticamente al lanzar cada simulación (queda embebida en la
 * {@link QualifyingSession}), de modo que el historial puede ofrecerla para
 * reutilizarla sin necesidad de una colección aparte.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SimulationConfig {

    public static final int DURACION_PREDETERMINADA_SEGUNDOS = 10;
    public static final int DURACION_MINIMA_SEGUNDOS = 1;
    public static final int DURACION_MAXIMA_SEGUNDOS = 3_600;

    private String id;
    private String circuito;
    private Integer pilotoId;
    private String vehiculo;
    private DrivingMode modo;
    private AerodynamicLoad aerodinamica;
    private TirePressure presion;
    private TireCompound compuestoInicial;
    private FuelStrategy combustible;
    private int duracionSegundos;
    private String guardadoEn;

    public SimulationConfig() {
        this.id = UUID.randomUUID().toString();
        this.modo = DrivingMode.NORMAL;
        this.aerodinamica = AerodynamicLoad.MEDIA;
        this.presion = TirePressure.ESTANDAR;
        this.compuestoInicial = TireCompound.MEDIUM;
        this.combustible = FuelStrategy.BALANCEADA;
        this.duracionSegundos = DURACION_PREDETERMINADA_SEGUNDOS;
    }

    public SimulationConfig(String circuito, Integer pilotoId, String vehiculo, DrivingMode modo,
                             AerodynamicLoad aerodinamica, TirePressure presion, FuelStrategy combustible) {
        this();
        this.circuito = circuito;
        this.pilotoId = pilotoId;
        this.vehiculo = vehiculo;
        this.modo = modo;
        this.aerodinamica = aerodinamica;
        this.presion = presion;
        this.combustible = combustible;
    }

    /** Configuración neutra: punto de partida de un formulario vacío. */
    public static SimulationConfig porDefecto() {
        return new SimulationConfig();
    }

    /**
     * Configuración del resto de la parrilla.
     *
     * En una clasificación todos los equipos aprietan, así que los rivales
     * corren en modo agresivo. Si usaran la configuración neutra, elegir
     * modo agresivo abriría una diferencia irreal de varios segundos con
     * el resto en lugar de las décimas que se ven en la realidad.
     */
    public static SimulationConfig paraClasificacion() {
        return new SimulationConfig(null, null, null, DrivingMode.AGRESIVA,
                AerodynamicLoad.MEDIA, TirePressure.ESTANDAR, FuelStrategy.AGRESIVA);
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

    public Integer getPilotoId() {
        return pilotoId;
    }

    public void setPilotoId(Integer pilotoId) {
        this.pilotoId = pilotoId;
    }

    public String getVehiculo() {
        return vehiculo;
    }

    public void setVehiculo(String vehiculo) {
        this.vehiculo = vehiculo;
    }

    public DrivingMode getModo() {
        return modo;
    }

    public void setModo(DrivingMode modo) {
        this.modo = modo;
    }

    public AerodynamicLoad getAerodinamica() {
        return aerodinamica;
    }

    public void setAerodinamica(AerodynamicLoad aerodinamica) {
        this.aerodinamica = aerodinamica;
    }

    public TirePressure getPresion() {
        return presion;
    }

    public void setPresion(TirePressure presion) {
        this.presion = presion;
    }

    public TireCompound getCompuestoInicial() {
        return compuestoInicial == null ? TireCompound.MEDIUM : compuestoInicial;
    }

    public void setCompuestoInicial(TireCompound compuestoInicial) {
        this.compuestoInicial = compuestoInicial == null
                ? TireCompound.MEDIUM : compuestoInicial;
    }

    public FuelStrategy getCombustible() {
        return combustible;
    }

    public void setCombustible(FuelStrategy combustible) {
        this.combustible = combustible;
    }

    public int getDuracionSegundos() {
        return duracionSegundos <= 0
                ? DURACION_PREDETERMINADA_SEGUNDOS : duracionSegundos;
    }

    public void setDuracionSegundos(int duracionSegundos) {
        if (duracionSegundos < DURACION_MINIMA_SEGUNDOS
                || duracionSegundos > DURACION_MAXIMA_SEGUNDOS) {
            throw new IllegalArgumentException("La duración debe estar entre "
                    + DURACION_MINIMA_SEGUNDOS + " y "
                    + DURACION_MAXIMA_SEGUNDOS + " segundos");
        }
        this.duracionSegundos = duracionSegundos;
    }

    public String getGuardadoEn() {
        return guardadoEn;
    }

    public void setGuardadoEn(String guardadoEn) {
        this.guardadoEn = guardadoEn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SimulationConfig)) return false;
        return Objects.equals(id, ((SimulationConfig) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        String seleccion = pilotoId == null
                ? vehiculo
                : "Piloto #" + pilotoId + " · " + vehiculo;
        return seleccion + " · " + modo + " · Aero " + aerodinamica
                + " · Presión " + presion + " · Compuesto " + getCompuestoInicial()
                + " · " + combustible
                + " · " + getDuracionSegundos() + " s";
    }
}
