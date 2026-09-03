package com.formula1.model;

/**
 * Trazado de un circuito como curva cerrada y suave, reparametrizada por
 * longitud de arco.
 *
 * <p>Los puntos de control se interpolan con una spline <b>Catmull-Rom
 * centrípeta cerrada</b> convertida a Bézier cúbica. Se elige Catmull-Rom y no
 * una Bézier suelta porque la curva pasa <em>por</em> los puntos de control:
 * calcar un trazado real se reduce entonces a colocar puntos sobre él, sin
 * tener que calcular tiradores.
 *
 * <p>Y se elige la variante <b>centrípeta</b> (α = ½) y no la uniforme porque
 * la uniforme forma cúspides y se cruza consigo misma cuando los puntos de
 * control están desigualmente espaciados, que es exactamente lo que ocurre en
 * la horquilla de Mónaco o en la cuchara de Suzuka: allí hay muchos puntos muy
 * juntos seguidos de una recta larga.
 *
 * <p>La reparametrización por longitud de arco es lo que permite que
 * {@link #puntoEn(double)} avance a velocidad constante. Sobre la Bézier cruda
 * el parámetro corre más deprisa en las rectas que en las curvas, así que un
 * coche movido con el parámetro directo aceleraría al entrar en cada curva.
 */
public final class TrackLayout {

    /**
     * Muestras por tramo de control. 24 mantiene el error de aplanado por
     * debajo del grosor de la línea con la que se pinta la pista, y deja la
     * polilínea en unos 800 puntos para un trazado de 35 curvas: barata de
     * recorrer entera en cada repintado.
     */
    private static final int MUESTRAS_POR_TRAMO = 24;

    /** Por debajo de esto dos puntos de control se consideran el mismo. */
    private static final double EPSILON = 1e-9;

    private final double[] xs;
    private final double[] ys;
    /** Longitud acumulada hasta cada muestra; {@code acumulado[muestras]} cierra el lazo. */
    private final double[] acumulado;
    private final double longitud;

    private TrackLayout(double[] xs, double[] ys) {
        this.xs = xs;
        this.ys = ys;
        int muestras = xs.length;
        this.acumulado = new double[muestras + 1];
        for (int i = 1; i <= muestras; i++) {
            int previo = i - 1;
            int actual = i % muestras;
            acumulado[i] = acumulado[previo]
                    + Math.hypot(xs[actual] - xs[previo], ys[actual] - ys[previo]);
        }
        this.longitud = acumulado[muestras];
    }

    /**
     * Construye el trazado a partir de sus puntos de control, en el orden en
     * que se recorre la pista. El lazo se cierra solo: no hay que repetir el
     * primer punto al final.
     *
     * @param control pares {@code {x, y}}; se recomiendan coordenadas
     *                normalizadas en [0,1] con la Y hacia abajo, como en pantalla
     */
    public static TrackLayout desdePuntosDeControl(double[][] control) {
        if (control == null || control.length < 4) {
            throw new IllegalArgumentException(
                    "Un trazado necesita al menos 4 puntos de control");
        }
        int n = control.length;
        double[] xs = new double[n * MUESTRAS_POR_TRAMO];
        double[] ys = new double[n * MUESTRAS_POR_TRAMO];
        int destino = 0;
        for (int i = 0; i < n; i++) {
            // Los índices se toman en módulo n, que es lo que cierra el lazo
            // sin costura: el último tramo interpola contra los primeros puntos.
            double[] p0 = control[(i - 1 + n) % n];
            double[] p1 = control[i];
            double[] p2 = control[(i + 1) % n];
            double[] p3 = control[(i + 2) % n];

            // Intervalos de nudo centrípetos: la raíz de la distancia (α = ½).
            double d1 = Math.sqrt(Math.hypot(p1[0] - p0[0], p1[1] - p0[1]));
            double d2 = Math.sqrt(Math.hypot(p2[0] - p1[0], p2[1] - p1[1]));
            double d3 = Math.sqrt(Math.hypot(p3[0] - p2[0], p3[1] - p2[1]));

            double b1x;
            double b1y;
            double b2x;
            double b2y;
            if (d1 < EPSILON || d2 < EPSILON || d3 < EPSILON) {
                // Puntos de control duplicados: la fórmula centrípeta divide
                // por cero. Se cae a las tangentes uniformes, que ahí valen.
                b1x = p1[0] + (p2[0] - p0[0]) / 6.0;
                b1y = p1[1] + (p2[1] - p0[1]) / 6.0;
                b2x = p2[0] - (p3[0] - p1[0]) / 6.0;
                b2y = p2[1] - (p3[1] - p1[1]) / 6.0;
            } else {
                double pesoB1 = 3 * d1 * (d1 + d2);
                double pesoB2 = 3 * d3 * (d3 + d2);
                double centralB1 = 2 * d1 * d1 + 3 * d1 * d2 + d2 * d2;
                double centralB2 = 2 * d3 * d3 + 3 * d3 * d2 + d2 * d2;
                b1x = (d1 * d1 * p2[0] - d2 * d2 * p0[0] + centralB1 * p1[0]) / pesoB1;
                b1y = (d1 * d1 * p2[1] - d2 * d2 * p0[1] + centralB1 * p1[1]) / pesoB1;
                b2x = (d3 * d3 * p1[0] - d2 * d2 * p3[0] + centralB2 * p2[0]) / pesoB2;
                b2y = (d3 * d3 * p1[1] - d2 * d2 * p3[1] + centralB2 * p2[1]) / pesoB2;
            }

            for (int k = 0; k < MUESTRAS_POR_TRAMO; k++) {
                double t = k / (double) MUESTRAS_POR_TRAMO;
                xs[destino] = bezier(p1[0], b1x, b2x, p2[0], t);
                ys[destino] = bezier(p1[1], b1y, b2y, p2[1], t);
                destino++;
            }
        }
        return new TrackLayout(xs, ys);
    }

    private static double bezier(double a, double b, double c, double d, double t) {
        double u = 1 - t;
        return u * u * u * a + 3 * u * u * t * b + 3 * u * t * t * c + t * t * t * d;
    }

    /**
     * Posición y orientación a la fracción {@code t} de la vuelta.
     *
     * @param t fracción de vuelta; se envuelve, así que 1.25 equivale a 0.25
     */
    public Punto puntoEn(double t) {
        double normalizada = t - Math.floor(t);
        double objetivo = normalizada * longitud;
        int tramo = buscarTramo(objetivo);
        double largoTramo = acumulado[tramo + 1] - acumulado[tramo];
        double local = largoTramo <= 0 ? 0 : (objetivo - acumulado[tramo]) / largoTramo;
        int siguiente = (tramo + 1) % xs.length;
        double dx = xs[siguiente] - xs[tramo];
        double dy = ys[siguiente] - ys[tramo];
        return new Punto(xs[tramo] + dx * local, ys[tramo] + dy * local, Math.atan2(dy, dx));
    }

    /** Índice del tramo cuyo arco acumulado contiene {@code objetivo}. */
    private int buscarTramo(double objetivo) {
        int bajo = 0;
        int alto = xs.length - 1;
        while (bajo < alto) {
            int medio = (bajo + alto + 1) >>> 1;
            if (acumulado[medio] <= objetivo) {
                bajo = medio;
            } else {
                alto = medio - 1;
            }
        }
        return bajo;
    }

    /** Longitud total del lazo, en las unidades de los puntos de control. */
    public double longitud() {
        return longitud;
    }

    public int muestras() {
        return xs.length;
    }

    public double x(int indice) {
        return xs[Math.floorMod(indice, xs.length)];
    }

    public double y(int indice) {
        return ys[Math.floorMod(indice, ys.length)];
    }

    /** Caja envolvente como {@code {minX, minY, maxX, maxY}}. */
    public double[] limites() {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        for (int i = 0; i < xs.length; i++) {
            minX = Math.min(minX, xs[i]);
            minY = Math.min(minY, ys[i]);
            maxX = Math.max(maxX, xs[i]);
            maxY = Math.max(maxY, ys[i]);
        }
        return new double[] {minX, minY, maxX, maxY};
    }

    /**
     * Transformación que encaja el trazado en un lienzo conservando la
     * proporción y centrándolo. Es matemática pura para poder comprobarla sin
     * levantar JavaFX, igual que {@code ImageCrop.recorteDe}.
     */
    public Encaje encajarEn(double ancho, double alto, double margen) {
        double[] caja = limites();
        double anchoTrazado = Math.max(1e-9, caja[2] - caja[0]);
        double altoTrazado = Math.max(1e-9, caja[3] - caja[1]);
        double disponibleAncho = Math.max(1, ancho - 2 * margen);
        double disponibleAlto = Math.max(1, alto - 2 * margen);
        double escala = Math.min(disponibleAncho / anchoTrazado, disponibleAlto / altoTrazado);
        double sobranteX = disponibleAncho - anchoTrazado * escala;
        double sobranteY = disponibleAlto - altoTrazado * escala;
        return new Encaje(escala,
                margen + sobranteX / 2.0 - caja[0] * escala,
                margen + sobranteY / 2.0 - caja[1] * escala);
    }

    /** Punto del trazado con la orientación de la pista en ese punto. */
    public record Punto(double x, double y, double angulo) {
    }

    /** Escala y desplazamiento que llevan el trazado a coordenadas de lienzo. */
    public record Encaje(double escala, double desplazamientoX, double desplazamientoY) {

        public double x(double valor) {
            return valor * escala + desplazamientoX;
        }

        public double y(double valor) {
            return valor * escala + desplazamientoY;
        }
    }
}
