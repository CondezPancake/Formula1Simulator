package com.formula1.service;

import com.formula1.model.TrackEvolutionSnapshot;
import com.formula1.model.WeatherSnapshot;
import com.formula1.util.MathUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Evoluciona la adherencia entre vueltas sin mezclarla con la generación del clima.
 * El servicio no conserva estado mutable, por lo que distintas simulaciones pueden
 * ejecutarlo de forma segura en paralelo.
 */
final class TrackEvolutionService {

    static final double MAX_GOMA_PORCENTAJE = 10;
    private static final double DEFICIT_PISTA_VERDE = 10;
    private static final double GOMA_POR_VUELTA_SECA = 0.55;
    private static final double LAVADO_POR_VUELTA_MOJADA = 7.0;

    Evolution evolucionar(List<WeatherSnapshot> clima, double gomaInicial,
                          int vuelta, String piloto) {
        if (clima == null || clima.isEmpty()) {
            throw new ValidationException("La evolución climática no puede estar vacía");
        }
        if (!Double.isFinite(gomaInicial)
                || gomaInicial < 0 || gomaInicial > MAX_GOMA_PORCENTAJE) {
            throw new ValidationException("El nivel de goma de la pista no es válido");
        }

        double goma = gomaInicial;
        double lluviaAcumulada = 0;
        List<WeatherSnapshot> pista = new ArrayList<>(clima.size());

        for (WeatherSnapshot muestra : clima) {
            double lluvia = muestra.intensidadLluviaPorcentaje() / 100.0;
            lluviaAcumulada += muestra.intensidadLluviaPorcentaje();
            if (lluvia < 0.02) {
                goma += GOMA_POR_VUELTA_SECA / clima.size();
            } else {
                goma -= LAVADO_POR_VUELTA_MOJADA * lluvia / clima.size();
            }
            goma = MathUtils.clamp(goma, 0, MAX_GOMA_PORCENTAJE);

            // Una pista verde parte con déficit de adherencia. Cada punto de
            // goma recupera uno de grip, tracción y frenado mediante el contrato
            // inmutable que WeatherSnapshot ya ofrece para impactos de pista.
            double modificadorGrip = -DEFICIT_PISTA_VERDE + goma;
            pista.add(muestra.conImpacto(0, modificadorGrip));
        }

        List<WeatherSnapshot> resultado = List.copyOf(pista);
        TrackEvolutionSnapshot resumen = new TrackEvolutionSnapshot(
                vuelta,
                piloto,
                resultado.get(0).gripPorcentaje(),
                resultado.get(resultado.size() - 1).gripPorcentaje(),
                gomaInicial,
                goma,
                lluviaAcumulada / clima.size());
        return new Evolution(resultado, goma, resumen);
    }

    /** Resultado inmutable que permite al orquestador decidir cuándo conservar el estado. */
    record Evolution(
            List<WeatherSnapshot> clima,
            double gomaFinalPorcentaje,
            TrackEvolutionSnapshot resumen) {
    }
}
