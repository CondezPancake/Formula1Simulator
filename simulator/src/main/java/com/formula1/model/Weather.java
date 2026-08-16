package com.formula1.model;

public class Weather {

    private double temperatura;
    private double humedad;
    private double probabilidadLluvia;
    private double intensidadLluvia;
    private double temperaturaPista;
    private TrackStatus estadoPista;

    public Weather() {
    }

    public Weather(double temperatura, double humedad, double probabilidadLluvia,
                    double intensidadLluvia, double temperaturaPista, TrackStatus estadoPista) {
        this.temperatura = temperatura;
        this.humedad = humedad;
        this.probabilidadLluvia = probabilidadLluvia;
        this.intensidadLluvia = intensidadLluvia;
        this.temperaturaPista = temperaturaPista;
        this.estadoPista = estadoPista;
    }

    public double getTemperatura() {
        return temperatura;
    }

    public void setTemperatura(double temperatura) {
        this.temperatura = temperatura;
    }

    public double getHumedad() {
        return humedad;
    }

    public void setHumedad(double humedad) {
        this.humedad = humedad;
    }

    public double getProbabilidadLluvia() {
        return probabilidadLluvia;
    }

    public void setProbabilidadLluvia(double probabilidadLluvia) {
        this.probabilidadLluvia = probabilidadLluvia;
    }

    public double getIntensidadLluvia() {
        return intensidadLluvia;
    }

    public void setIntensidadLluvia(double intensidadLluvia) {
        this.intensidadLluvia = intensidadLluvia;
    }

    public double getTemperaturaPista() {
        return temperaturaPista;
    }

    public void setTemperaturaPista(double temperaturaPista) {
        this.temperaturaPista = temperaturaPista;
    }

    public TrackStatus getEstadoPista() {
        return estadoPista;
    }

    public void setEstadoPista(TrackStatus estadoPista) {
        this.estadoPista = estadoPista;
    }

    @Override
    public String toString() {
        return "Weather{" +
                "temperatura=" + temperatura +
                ", estadoPista=" + estadoPista +
                ", probabilidadLluvia=" + probabilidadLluvia +
                '}';
    }
}
