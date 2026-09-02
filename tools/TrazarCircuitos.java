import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;

/**
 * Genera los puntos de control de {@code util/TrackLayouts} calcando los mapas
 * oficiales de {@code /images/circuits/}.
 *
 * <p>Se ejecuta con el JDK a secas, sin dependencias y sin Maven:
 *
 * <pre>
 *   cd tools
 *   java TrazarCircuitos                 # imprime los arrays Java
 *   java TrazarCircuitos --debug         # ademas vuelca las mascaras a PNG
 * </pre>
 *
 * <p>Existe por lo mismo que {@code gen_seed.py}: para que los 588 numeros del
 * catalogo de trazados sean reproducibles y auditables en lugar de constantes
 * llovidas del cielo. Si se cambia el mapa de un circuito, se vuelve a correr
 * y se pega la salida.
 *
 * <h2>Como funciona</h2>
 *
 * <ol>
 *   <li><b>Mascara.</b> Pixel opaco y oscuro: el asfalto. Los rotulos de DRS y
 *       del speed trap van en verde y magenta, asi que quedan fuera solos.</li>
 *   <li><b>Cierre morfologico.</b> Imprescindible: el mapa no dibuja la pista
 *       como una banda maciza sino como <i>dos</i> bandas oscuras con una linea
 *       clara en medio (8 px + 3 px + 8 px). Sin cerrar ese hueco se acaba
 *       calcando las dos orillas en vez del eje.</li>
 *   <li><b>Relleno de huecos pequenos.</b> Los numeros de curva van en blanco
 *       dentro de un disco oscuro; sin rellenarlos, cada badge deja un anillo
 *       que descarrila el recorrido. El interior del circuito tambien esta
 *       encerrado, pero es enorme y el umbral lo deja fuera.</li>
 *   <li><b>Contorno de Moore.</b> Se recorre el borde de la banda, que por
 *       construccion es una unica curva cerrada y ordenada. Se prefirio al
 *       esqueleto porque el adelgazado deja bifurcaciones y cortes donde las
 *       lineas de color cruzan la pista, y el recorrido se atasca en ellos.</li>
 *   <li><b>Desplazamiento al eje.</b> El contorno se mete hacia dentro medio
 *       ancho de pista, medido sobre la propia imagen.</li>
 *   <li><b>Suavizado y remuestreo.</b> Una media movil ciclica corta las puas
 *       que dejan los discos de las curvas, y se toman 84 puntos equiespaciados
 *       por longitud de arco.</li>
 * </ol>
 */
public final class TrazarCircuitos {

    /** Puntos de control por circuito. 84 capta las chicanes sin sobrecargar. */
    private static final int PUNTOS = 84;

    /** Radio del cierre: el hueco de la linea central mide 3-4 px. */
    private static final int RADIO_CIERRE = 3;

    /** Area maxima de un hueco relleno; el interior del circuito supera esto. */
    private static final int HUECO_MAXIMO = 2000;

    /**
     * Fichero, constante, nombre, X e Y de la bandera a cuadros como fraccion del
     * tamano de la imagen, y sentido real de la marcha sobre ese mapa.
     *
     * <p>La bandera y el sentido se leen del propio mapa oficial: la primera
     * situa la linea de meta y el segundo dice si el contorno hay que invertirlo.
     * El trazado de Moore siempre sale horario, asi que los circuitos que se
     * corren al reves se marcan aqui.
     */
    private static final Object[][] CIRCUITOS = {
        // El nombre es el del seed, para que la salida se pueda pegar tal cual.
        {"monza.png", "MONZA", "Circuito de Monza", 0.78, 0.85, "horario"},
        {"monaco.png", "MONACO", "Circuito de Mónaco", 0.095, 0.40, "horario"},
        {"silverstone.jpg", "SILVERSTONE", "Silverstone", 0.39, 0.15, "horario"},
        {"spa-francorchamps.png", "SPA", "Circuito de Spa-Francorchamps", 0.31, 0.83, "horario"},
        {"interlagos.png", "INTERLAGOS", "Interlagos", 0.28, 0.075, "antihorario"},
        {"yas-marina.png", "YAS_MARINA", "Circuito de Yas Marina", 0.49, 0.42, "antihorario"},
        {"suzuka.jpg", "SUZUKA", "Circuito de Suzuka", 0.76, 0.35, "horario"},
    };

    private static boolean depurar;

    private TrazarCircuitos() {
    }

    public static void main(String[] args) {
        depurar = args.length > 0 && "--debug".equals(args[0]);
        File base = localizarImagenes();
        System.out.println("    // Generado por tools/TrazarCircuitos.java");
        StringBuilder mapa = new StringBuilder();
        for (Object[] circuito : CIRCUITOS) {
            try {
                mapa.append(procesar(new File(base, (String) circuito[0]),
                        (String) circuito[1], (String) circuito[2],
                        (Double) circuito[3], (Double) circuito[4],
                        "antihorario".equals(circuito[5])));
            } catch (Exception fallo) {
                System.out.println("    // FALLO " + circuito[0] + ": " + fallo);
            }
        }
        System.out.println();
        System.out.println("    private static final Map<String, Trazado> TRAZADOS = Map.of(");
        System.out.print(mapa);
    }

    /** Busca images/circuits subiendo desde el directorio actual. */
    private static File localizarImagenes() {
        File actual = new File(".").getAbsoluteFile();
        while (actual != null) {
            File candidato = new File(actual,
                    "simulator/src/main/resources/images/circuits");
            if (candidato.isDirectory()) {
                return candidato;
            }
            actual = actual.getParentFile();
        }
        throw new IllegalStateException(
                "No encuentro simulator/src/main/resources/images/circuits");
    }

    private static String procesar(File fichero, String constante, String nombre,
                                   double banderaX, double banderaY, boolean antihorario)
            throws Exception {
        BufferedImage imagen = ImageIO.read(fichero);
        int w = imagen.getWidth();
        int h = imagen.getHeight();

        boolean[] mascara = new boolean[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = imagen.getRGB(x, y);
                int alfa = (argb >>> 24) & 0xFF;
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                mascara[y * w + x] = alfa >= 128 && r < 100 && g < 100 && b < 100;
            }
        }

        boolean[] pista = cerrar(mascara, w, h, RADIO_CIERRE);
        rellenarHuecos(pista, w, h, HUECO_MAXIMO);
        pista = componenteMayor(pista, w, h);
        if (depurar) {
            volcar(pista, w, h, new File(fichero.getParentFile(),
                    "dbg-" + constante.toLowerCase(Locale.ROOT) + ".png"));
        }

        List<int[]> contorno = contornoMoore(pista, w, h);
        double medioAncho = medirMedioAncho(pista, contorno, w, h);
        List<double[]> eje = desplazarAlEje(pista, contorno, w, h, medioAncho);
        List<double[]> suave = suavizar(eje, Math.max(5, eje.size() / 70), 2);
        double[][] pixeles = remuestrear(suave, PUNTOS);

        // La meta se busca en coordenadas de la imagen, antes de normalizar:
        // es donde esta la bandera a cuadros del mapa oficial.
        int indiceMeta = masCercano(pixeles, banderaX * w, banderaY * h);
        // El calibrado invierte primero y gira despues, asi que al invertir el
        // punto que estaba en `i` pasa a estar en `n-1-i`.
        int n = pixeles.length;
        double meta = (antihorario ? (n - 1 - indiceMeta) : indiceMeta) / (double) n;

        imprimir(constante, nombre, fichero.getName(), normalizar(pixeles), contorno.size());
        return String.format(Locale.ROOT,
                "            \"%s\", new Trazado(%s, %.4f, %s),%n",
                nombre, constante, meta, antihorario);
    }

    private static int masCercano(double[][] puntos, double x, double y) {
        int mejor = 0;
        double distancia = Double.MAX_VALUE;
        for (int i = 0; i < puntos.length; i++) {
            double d = Math.hypot(puntos[i][0] - x, puntos[i][1] - y);
            if (d < distancia) {
                distancia = d;
                mejor = i;
            }
        }
        return mejor;
    }

    // ------------------------------------------------------------- morfologia

    private static boolean[] cerrar(boolean[] m, int w, int h, int radio) {
        return erosionar(dilatar(m, w, h, radio), w, h, radio);
    }

    private static boolean[] dilatar(boolean[] m, int w, int h, int radio) {
        boolean[] salida = new boolean[m.length];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (!m[y * w + x]) {
                    continue;
                }
                for (int dy = -radio; dy <= radio; dy++) {
                    int ny = y + dy;
                    if (ny < 0 || ny >= h) {
                        continue;
                    }
                    for (int dx = -radio; dx <= radio; dx++) {
                        int nx = x + dx;
                        if (nx >= 0 && nx < w) {
                            salida[ny * w + nx] = true;
                        }
                    }
                }
            }
        }
        return salida;
    }

    private static boolean[] erosionar(boolean[] m, int w, int h, int radio) {
        boolean[] salida = new boolean[m.length];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                boolean todos = true;
                for (int dy = -radio; dy <= radio && todos; dy++) {
                    for (int dx = -radio; dx <= radio && todos; dx++) {
                        int nx = x + dx;
                        int ny = y + dy;
                        todos = nx >= 0 && ny >= 0 && nx < w && ny < h && m[ny * w + nx];
                    }
                }
                salida[y * w + x] = todos;
            }
        }
        return salida;
    }

    private static void rellenarHuecos(boolean[] m, int w, int h, int areaMaxima) {
        boolean[] alcanzado = new boolean[m.length];
        Deque<Integer> cola = new ArrayDeque<>();
        for (int x = 0; x < w; x++) {
            encolarFondo(m, alcanzado, cola, x);
            encolarFondo(m, alcanzado, cola, (h - 1) * w + x);
        }
        for (int y = 0; y < h; y++) {
            encolarFondo(m, alcanzado, cola, y * w);
            encolarFondo(m, alcanzado, cola, y * w + w - 1);
        }
        while (!cola.isEmpty()) {
            expandir(m, alcanzado, cola, cola.poll(), w, h);
        }

        boolean[] contado = new boolean[m.length];
        for (int i = 0; i < m.length; i++) {
            if (m[i] || alcanzado[i] || contado[i]) {
                continue;
            }
            List<Integer> hueco = new ArrayList<>();
            Deque<Integer> pila = new ArrayDeque<>();
            pila.push(i);
            contado[i] = true;
            while (!pila.isEmpty()) {
                int p = pila.pop();
                hueco.add(p);
                expandir(m, contado, pila, p, w, h, alcanzado);
            }
            if (hueco.size() <= areaMaxima) {
                for (int p : hueco) {
                    m[p] = true;
                }
            }
        }
    }

    private static void encolarFondo(boolean[] m, boolean[] visto,
                                     Deque<Integer> cola, int indice) {
        if (!m[indice] && !visto[indice]) {
            visto[indice] = true;
            cola.add(indice);
        }
    }

    private static void expandir(boolean[] m, boolean[] visto, Deque<Integer> pendientes,
                                 int p, int w, int h) {
        expandir(m, visto, pendientes, p, w, h, null);
    }

    private static void expandir(boolean[] m, boolean[] visto, Deque<Integer> pendientes,
                                 int p, int w, int h, boolean[] excluidos) {
        int px = p % w;
        int py = p / w;
        for (int[] d : new int[][] {{0, -1}, {1, 0}, {0, 1}, {-1, 0}}) {
            int nx = px + d[0];
            int ny = py + d[1];
            if (nx < 0 || ny < 0 || nx >= w || ny >= h) {
                continue;
            }
            int q = ny * w + nx;
            if (!m[q] && !visto[q] && (excluidos == null || !excluidos[q])) {
                visto[q] = true;
                pendientes.add(q);
            }
        }
    }

    private static boolean[] componenteMayor(boolean[] mascara, int w, int h) {
        int[] etiqueta = new int[w * h];
        int mejorEtiqueta = 0;
        int mejorTamano = 0;
        int actual = 0;
        for (int i = 0; i < mascara.length; i++) {
            if (!mascara[i] || etiqueta[i] != 0) {
                continue;
            }
            actual++;
            int tamano = 0;
            Deque<Integer> pila = new ArrayDeque<>();
            pila.push(i);
            etiqueta[i] = actual;
            while (!pila.isEmpty()) {
                int p = pila.pop();
                tamano++;
                int px = p % w;
                int py = p / w;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int nx = px + dx;
                        int ny = py + dy;
                        if (nx < 0 || ny < 0 || nx >= w || ny >= h) {
                            continue;
                        }
                        int q = ny * w + nx;
                        if (mascara[q] && etiqueta[q] == 0) {
                            etiqueta[q] = actual;
                            pila.push(q);
                        }
                    }
                }
            }
            if (tamano > mejorTamano) {
                mejorTamano = tamano;
                mejorEtiqueta = actual;
            }
        }
        boolean[] salida = new boolean[w * h];
        for (int i = 0; i < salida.length; i++) {
            salida[i] = etiqueta[i] == mejorEtiqueta;
        }
        return salida;
    }

    // --------------------------------------------------------------- contorno

    /** Vecinos en orden horario, empezando por el oeste. */
    private static final int[][] HORARIO = {
        {-1, 0}, {-1, -1}, {0, -1}, {1, -1}, {1, 0}, {1, 1}, {0, 1}, {-1, 1},
    };

    private static List<int[]> contornoMoore(boolean[] m, int w, int h) {
        int inicio = -1;
        for (int i = 0; i < m.length && inicio < 0; i++) {
            if (m[i]) {
                inicio = i;
            }
        }
        if (inicio < 0) {
            throw new IllegalStateException("no hay asfalto en la mascara");
        }
        List<int[]> contorno = new ArrayList<>();
        int bx = inicio % w;
        int by = inicio / w;
        int dirEntrada = 0;
        for (int paso = 0; paso < 8 * m.length; paso++) {
            contorno.add(new int[] {bx, by});
            int siguiente = -1;
            int dirSiguiente = 0;
            for (int k = 1; k <= 8; k++) {
                int d = (dirEntrada + k) % 8;
                int nx = bx + HORARIO[d][0];
                int ny = by + HORARIO[d][1];
                if (nx < 0 || ny < 0 || nx >= w || ny >= h) {
                    continue;
                }
                if (m[ny * w + nx]) {
                    siguiente = ny * w + nx;
                    dirSiguiente = (d + 5) % 8;
                    break;
                }
            }
            if (siguiente < 0) {
                break;
            }
            bx = siguiente % w;
            by = siguiente / w;
            dirEntrada = dirSiguiente;
            if (bx == inicio % w && by == inicio / w && contorno.size() > 8) {
                break;
            }
        }
        return contorno;
    }

    private static double medirMedioAncho(boolean[] m, List<int[]> contorno, int w, int h) {
        List<Double> medidas = new ArrayList<>();
        int n = contorno.size();
        for (int i = 0; i < n; i += Math.max(1, n / 120)) {
            double[] normal = normalInterior(m, contorno, i, w, h);
            if (normal == null) {
                continue;
            }
            double d = 0;
            while (d < 60) {
                int nx = (int) Math.round(contorno.get(i)[0] + normal[0] * d);
                int ny = (int) Math.round(contorno.get(i)[1] + normal[1] * d);
                if (nx < 0 || ny < 0 || nx >= w || ny >= h || !m[ny * w + nx]) {
                    break;
                }
                d += 1;
            }
            if (d > 1 && d < 60) {
                medidas.add(d);
            }
        }
        if (medidas.isEmpty()) {
            return 5;
        }
        medidas.sort(Double::compareTo);
        return medidas.get(medidas.size() / 2) / 2.0;
    }

    private static double[] normalInterior(boolean[] m, List<int[]> contorno, int i,
                                           int w, int h) {
        int n = contorno.size();
        int ventana = Math.max(2, n / 200);
        int[] previo = contorno.get((i - ventana + n) % n);
        int[] posterior = contorno.get((i + ventana) % n);
        double tx = posterior[0] - previo[0];
        double ty = posterior[1] - previo[1];
        double norma = Math.hypot(tx, ty);
        if (norma < 1e-6) {
            return null;
        }
        tx /= norma;
        ty /= norma;
        for (double signo : new double[] {1, -1}) {
            double nx = -ty * signo;
            double ny = tx * signo;
            int px = (int) Math.round(contorno.get(i)[0] + nx * 3);
            int py = (int) Math.round(contorno.get(i)[1] + ny * 3);
            if (px >= 0 && py >= 0 && px < w && py < h && m[py * w + px]) {
                return new double[] {nx, ny};
            }
        }
        return null;
    }

    private static List<double[]> desplazarAlEje(boolean[] m, List<int[]> contorno,
                                                 int w, int h, double medioAncho) {
        List<double[]> eje = new ArrayList<>();
        for (int i = 0; i < contorno.size(); i++) {
            double[] normal = normalInterior(m, contorno, i, w, h);
            int[] p = contorno.get(i);
            eje.add(normal == null
                    ? new double[] {p[0], p[1]}
                    : new double[] {p[0] + normal[0] * medioAncho,
                                    p[1] + normal[1] * medioAncho});
        }
        return eje;
    }

    // ------------------------------------------------------- suavizado y toma

    private static List<double[]> suavizar(List<double[]> puntos, int ventana, int pasadas) {
        List<double[]> actual = puntos;
        int n = puntos.size();
        for (int p = 0; p < pasadas; p++) {
            List<double[]> siguiente = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                double sx = 0;
                double sy = 0;
                for (int k = -ventana; k <= ventana; k++) {
                    double[] q = actual.get(((i + k) % n + n) % n);
                    sx += q[0];
                    sy += q[1];
                }
                int cuenta = 2 * ventana + 1;
                siguiente.add(new double[] {sx / cuenta, sy / cuenta});
            }
            actual = siguiente;
        }
        return actual;
    }

    private static double[][] remuestrear(List<double[]> ciclo, int n) {
        int m = ciclo.size();
        double[] acumulado = new double[m + 1];
        for (int i = 1; i <= m; i++) {
            double[] a = ciclo.get(i - 1);
            double[] b = ciclo.get(i % m);
            acumulado[i] = acumulado[i - 1] + Math.hypot(b[0] - a[0], b[1] - a[1]);
        }
        double total = acumulado[m];
        double[][] puntos = new double[n][2];
        int j = 0;
        for (int k = 0; k < n; k++) {
            double objetivo = total * k / n;
            while (j < m && acumulado[j + 1] < objetivo) {
                j++;
            }
            double tramo = acumulado[j + 1] - acumulado[j];
            double u = tramo <= 0 ? 0 : (objetivo - acumulado[j]) / tramo;
            double[] a = ciclo.get(j % m);
            double[] b = ciclo.get((j + 1) % m);
            puntos[k][0] = a[0] + (b[0] - a[0]) * u;
            puntos[k][1] = a[1] + (b[1] - a[1]) * u;
        }
        return puntos;
    }

    /** Escala por el lado mayor: el eje largo ocupa [0,1] y se conserva la proporcion. */
    private static double[][] normalizar(double[][] puntos) {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        for (double[] p : puntos) {
            minX = Math.min(minX, p[0]);
            minY = Math.min(minY, p[1]);
            maxX = Math.max(maxX, p[0]);
            maxY = Math.max(maxY, p[1]);
        }
        double escala = 1.0 / Math.max(maxX - minX, maxY - minY);
        double[][] salida = new double[puntos.length][2];
        for (int i = 0; i < puntos.length; i++) {
            salida[i][0] = (puntos[i][0] - minX) * escala;
            salida[i][1] = (puntos[i][1] - minY) * escala;
        }
        return salida;
    }

    private static void imprimir(String constante, String nombre, String fichero,
                                 double[][] puntos, int pixelesContorno) {
        double maxX = 0;
        double maxY = 0;
        for (double[] p : puntos) {
            maxX = Math.max(maxX, p[0]);
            maxY = Math.max(maxY, p[1]);
        }
        System.out.printf(Locale.ROOT,
                "%n    // %s · calcado de %s · contorno de %d px · aspecto %.2f:1%n",
                nombre, fichero, pixelesContorno, maxX / maxY);
        System.out.printf("    private static final double[][] %s = {%n", constante);
        for (int i = 0; i < puntos.length; i += 4) {
            StringBuilder fila = new StringBuilder("       ");
            for (int k = i; k < Math.min(i + 4, puntos.length); k++) {
                fila.append(String.format(Locale.ROOT, " {%.4f, %.4f},",
                        puntos[k][0], puntos[k][1]));
            }
            System.out.println(fila);
        }
        System.out.println("    };");
    }

    private static void volcar(boolean[] m, int w, int h, File destino) throws Exception {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                img.setRGB(x, y, m[y * w + x] ? 0xFFFFFF : 0x101015);
            }
        }
        ImageIO.write(img, "png", destino);
    }
}
