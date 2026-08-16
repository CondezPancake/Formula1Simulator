package com.formula1.model;

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

    private String id;
    private String circuito;
    private String vehiculo;
    private DrivingMode modo;
    private AerodynamicLoad aerodinamica;
    private TirePressure presion;
    private FuelStrategy combustible;
    private String guardadoEn;

    public SimulationConfig() {
        this.id = UUID.randomUUID().toString();
        this.modo = DrivingMode.NORMAL;
        this.aerodinamica = AerodynamicLoad.MEDIA;
        this.presion = TirePressure.ESTANDAR;
        this.combustible = FuelStrategy.BALANCEADA;
    }

    public SimulationConfig(String circuito, String vehiculo, DrivingMode modo,
                             AerodynamicLoad aerodinamica, TirePressure presion, FuelStrategy combustible) {
        this();
        this.circuito = circuito;
        this.vehiculo = vehiculo;
        this.modo = modo;
        this.aerodinamica = aerodinamica;
        this.presion = presion;
        this.combustible = combustible;
    }

    /** Configuración neutra, usada para los pilotos que no controla el usuario. */
    public static SimulationConfig porDefecto() {
        return new SimulationConfig();
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

    public FuelStrategy getCombustible() {
        return combustible;
    }

    public void setCombustible(FuelStrategy combustible) {
        this.combustible = combustible;
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
        return modo + " · Aero " + aerodinamica + " · Presión " + presion + " · " + combustible;
    }
}
