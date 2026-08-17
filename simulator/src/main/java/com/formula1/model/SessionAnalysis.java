package com.formula1.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Diagnostico persistible de una clasificacion.
 *
 * <p>El texto se guarda ya resuelto porque el historial debe poder explicar
 * una sesion antigua aunque las reglas de analisis evolucionen despues.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionAnalysis(
        String pilotoPole,
        double tiempoPoleSegundos,
        int participantesValidos,
        String resumen,
        List<String> factoresPositivos,
        List<String> factoresNegativos,
        List<String> factoresClave) {

    public SessionAnalysis {
        pilotoPole = normalizar(pilotoPole, "Sin pole");
        resumen = normalizar(resumen, "Sesion sin analisis disponible");
        factoresPositivos = copiaSegura(factoresPositivos);
        factoresNegativos = copiaSegura(factoresNegativos);
        factoresClave = copiaSegura(factoresClave);
        if (!Double.isFinite(tiempoPoleSegundos) || tiempoPoleSegundos < 0) {
            throw new IllegalArgumentException("El tiempo de pole debe ser finito y no negativo");
        }
        if (participantesValidos < 0) {
            throw new IllegalArgumentException("Los participantes validos no pueden ser negativos");
        }
    }

    public static SessionAnalysis vacio() {
        return new SessionAnalysis(
                "Sin pole",
                0,
                0,
                "No hubo vueltas validas para generar un analisis deportivo.",
                List.of(),
                List.of("Todas las vueltas quedaron invalidadas o sin tiempo representativo."),
                List.of("Sin datos suficientes"));
    }

    @JsonIgnore
    public boolean tieneResultados() {
        return participantesValidos > 0 && tiempoPoleSegundos > 0;
    }

    private static List<String> copiaSegura(List<String> valores) {
        return valores == null ? List.of() : List.copyOf(valores);
    }

    private static String normalizar(String valor, String fallback) {
        return valor == null || valor.isBlank() ? fallback : valor;
    }
}
