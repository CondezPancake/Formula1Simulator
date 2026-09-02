package com.formula1.controller;

import java.util.List;
import java.util.function.Consumer;
import java.util.concurrent.atomic.AtomicReference;

import com.formula1.model.Circuit;
import com.formula1.model.TrackLayout;
import com.formula1.util.TrackLayouts;
import com.formula1.util.TeamColors;

import javafx.animation.AnimationTimer;
import javafx.animation.PauseTransition;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.geometry.VPos;
import javafx.util.Duration;

/**
 * Mapa del circuito con los coches recorriéndolo en vivo.
 *
 * <p>Dos lienzos apilados. El de abajo lleva la pista, que se dibuja <b>una sola
 * vez</b> por circuito y tamaño; el de arriba solo los veinte marcadores, que se
 * repintan en cada fotograma. Repintar la pista sesenta veces por segundo sería
 * gastar el presupuesto en algo que no cambia.
 *
 * <p>Los dos lienzos van <b>sin gestionar</b> ({@code setManaged(false)}). Un
 * {@code Canvas} devuelve su propio tamaño como tamaño preferido y un
 * {@code StackPane} se dimensiona a partir de sus hijos: dejarlo gestionado y a
 * la vez atado al padre realimenta el bucle y el panel crece sin parar.
 * {@code IntroController} se libra de esto porque a su {@code StackPane} lo
 * dimensiona la ventana, así que ese detalle no se puede copiar tal cual.
 */
public final class CircuitoEnVivo {

    // Paleta tomada de style.css (.root, líneas 8-38). Se repite aquí porque el
    // lienzo pinta con Color y no con CSS; si allí cambia, cambia aquí.
    private static final Color FONDO = Color.web("#08080C");
    private static final Color CONTORNO = Color.web("#05050A");
    private static final Color REJILLA = Color.rgb(255, 255, 255, 0.045);
    private static final Color[] SECTORES = {
        Color.web("#FF0101"), Color.web("#3671C6"), Color.web("#FFC906"),
    };
    private static final Color TENUE = Color.web("#565656");
    private static final Color MARCA = Color.web("#E10600");

    /** Separación de la rejilla técnica del fondo, en píxeles. */
    private static final double PASO_REJILLA = 48;

    /** Cortes de sector del motor: {@code TrackSector.desdeSegmento} con 20 segmentos. */
    private static final double[] CORTES_SECTOR = {0.0, 0.35, 0.70, 1.0};

    private static final long EASE_MINIMO_NS = 80_000_000L;
    private static final long EASE_MAXIMO_NS = 1_200_000_000L;

    private final StackPane contenedor;
    private final Canvas lienzoPista;
    private final Canvas lienzoCoches;
    private final PauseTransition rehorneado;

    private final AtomicReference<MapaProgreso.Estado> objetivo = new AtomicReference<>();
    private final ReadOnlyObjectWrapper<Integer> pilotoResaltado =
            new ReadOnlyObjectWrapper<>(null);

    private TrackLayout trazado;
    private String nombreCircuito = "";
    /** Valor que acompaña a cada rótulo de sector; lo inyecta la pantalla. */
    private String[] etiquetasSector = new String[3];

    private MapaProgreso.Estado anterior;
    private long inicioEaseNanos;
    private long duracionEaseNanos = EASE_MINIMO_NS;
    private boolean sesionViva;
    private Integer pilotoUsuario;
    private double anchoHorneado;
    private double altoHorneado;

    private final AnimationTimer temporizador;
    private Consumer<Integer> alSeleccionarPiloto = id -> { };

    public CircuitoEnVivo(StackPane contenedor) {
        this.contenedor = contenedor;
        this.lienzoPista = nuevoLienzo();
        this.lienzoCoches = nuevoLienzo();
        lienzoPista.setMouseTransparent(true);
        contenedor.getChildren().addAll(lienzoPista, lienzoCoches);

        this.temporizador = new AnimationTimer() {
            @Override
            public void handle(long ahora) {
                pintarCoches(ahora);
            }
        };

        // El rehorneado se retrasa para no repintar la pista en cada píxel que
        // se arrastra al redimensionar la ventana.
        this.rehorneado = new PauseTransition(Duration.millis(90));
        rehorneado.setOnFinished(e -> hornearPista());

        contenedor.widthProperty().addListener((o, a, b) -> alCambiarTamano());
        contenedor.heightProperty().addListener((o, a, b) -> alCambiarTamano());

        // Al salir de Carrera el nodo se desengancha de la escena. Es la única
        // señal de ciclo de vida que hay: Navigator cachea la vista y su
        // controlador para siempre, y sin esto el temporizador quedaría vivo
        // quemando un pulso cada 16 ms el resto de la ejecución.
        lienzoCoches.sceneProperty().addListener((o, vieja, nueva) -> {
            if (nueva == null) {
                temporizador.stop();
            } else if (sesionViva) {
                temporizador.start();
            }
        });

        lienzoCoches.setOnMouseMoved(e -> resaltar(pilotoBajoElRaton(e.getX(), e.getY())));
        lienzoCoches.setOnMouseExited(e -> resaltar(null));
        lienzoCoches.setOnMouseClicked(e -> {
            Integer id = pilotoBajoElRaton(e.getX(), e.getY());
            if (id != null) {
                alSeleccionarPiloto.accept(id);
            }
        });
    }

    private Canvas nuevoLienzo() {
        Canvas lienzo = new Canvas();
        lienzo.setManaged(false);
        lienzo.widthProperty().bind(contenedor.widthProperty());
        lienzo.heightProperty().bind(contenedor.heightProperty());
        return lienzo;
    }

    // ------------------------------------------------------------------ ciclo

    /** Carga el trazado del circuito y lo dibuja. Tolera el nulo y el circuito sin calco. */
    public void mostrarCircuito(Circuit circuito) {
        this.trazado = circuito == null ? null : TrackLayouts.de(circuito.getNombre());
        this.nombreCircuito = circuito == null ? "" : circuito.getNombre();
        this.etiquetasSector = new String[3];
        this.objetivo.set(null);
        this.anterior = null;
        hornearPista();
        limpiarCoches();
    }

    /** Prepara una sesión nueva: reinicia los marcadores y arranca la animación. */
    public void iniciarSesion(Integer pilotoSeleccionadoId, int duracionSegundos) {
        this.pilotoUsuario = pilotoSeleccionadoId;
        this.objetivo.set(null);
        this.anterior = null;
        this.sesionViva = true;
        this.duracionEaseNanos = Math.max(EASE_MINIMO_NS, Math.min(EASE_MAXIMO_NS,
                duracionSegundos * 1_000_000_000L / MapaProgreso.TOTAL_SEGMENTOS));
        this.inicioEaseNanos = System.nanoTime();
        temporizador.start();
    }

    /**
     * Publica la foto de un segmento. Se llama desde el hilo de JavaFX, con el
     * mismo objeto que acaba de poblar la torre de tiempos.
     */
    public void publicar(MapaProgreso.Estado estado) {
        // Se parte de donde están los coches ahora, no del objetivo anterior:
        // así un segmento que llega tarde o repetido no da tirón ni rebote.
        this.anterior = instantanea();
        this.objetivo.set(estado);
        this.inicioEaseNanos = System.nanoTime();
        if (sesionViva) {
            temporizador.start();
        }
    }

    /** Cierra la sesión dejando los coches donde estén. */
    public void finalizar(boolean manual) {
        this.sesionViva = false;
        if (manual) {
            // Congelar donde están: saltar al final leería como que todos
            // cruzaron la meta, y precisamente no lo hicieron.
            this.anterior = instantanea();
            this.objetivo.set(this.anterior);
        }
        pintarCoches(System.nanoTime());
        temporizador.stop();
    }

    /** Parada dura, para cuando la simulación falla. */
    public void detener() {
        this.sesionViva = false;
        temporizador.stop();
    }

    public boolean estaAnimando() {
        return sesionViva;
    }

    public void resaltar(Integer pilotoId) {
        if (!java.util.Objects.equals(pilotoResaltado.get(), pilotoId)) {
            pilotoResaltado.set(pilotoId);
            pintarCoches(System.nanoTime());
        }
    }

    public ReadOnlyObjectProperty<Integer> pilotoResaltadoProperty() {
        return pilotoResaltado.getReadOnlyProperty();
    }

    public void setAlSeleccionarPiloto(Consumer<Integer> accion) {
        this.alSeleccionarPiloto = accion == null ? id -> { } : accion;
    }

    // ------------------------------------------------------------- horneado

    private void alCambiarTamano() {
        double ancho = contenedor.getWidth();
        // Un salto grande (maximizar) se atiende ya, para no dejar el panel en
        // blanco 90 ms; el arrastre fino espera al debounce.
        if (Math.abs(ancho - anchoHorneado) > 24) {
            hornearPista();
        }
        rehorneado.playFromStart();
    }

    private void hornearPista() {
        double w = lienzoPista.getWidth();
        double h = lienzoPista.getHeight();
        GraphicsContext g = lienzoPista.getGraphicsContext2D();
        g.clearRect(0, 0, Math.max(w, 1), Math.max(h, 1));
        if (w <= 0 || h <= 0) {
            return;
        }
        anchoHorneado = w;
        altoHorneado = h;

        g.setFill(FONDO);
        g.fillRect(0, 0, w, h);
        pintarRejilla(g, w, h);
        if (trazado == null) {
            mensaje(g, w, h, nombreCircuito.isBlank()
                    ? "Elige un circuito"
                    : "Sin trazado para " + nombreCircuito);
            return;
        }

        double anchoPista = anchoDePista(w, h);
        TrackLayout.Encaje encaje = trazado.encajarEn(w, h, anchoPista + 18);

        // Contorno oscuro: despega la pista del fondo sin robarle saturación al
        // color del sector, que es quien tiene que mandar aquí.
        trazarTramo(g, encaje, 0, 1, CONTORNO, anchoPista + 6, false);

        // Resplandor por acumulación de trazos anchos y translúcidos. Es más
        // barato que un DropShadow sobre el GraphicsContext y no depende de
        // efectos de píxel, que en este proyecto no se usan en ningún sitio.
        for (int s = 0; s < 3; s++) {
            g.setGlobalAlpha(0.10);
            trazarTramo(g, encaje, CORTES_SECTOR[s], CORTES_SECTOR[s + 1],
                    SECTORES[s], anchoPista * 2.2, true);
            g.setGlobalAlpha(0.18);
            trazarTramo(g, encaje, CORTES_SECTOR[s], CORTES_SECTOR[s + 1],
                    SECTORES[s], anchoPista * 1.6, true);
        }
        g.setGlobalAlpha(1);

        // Cuerpo del sector a color pleno. Los cortes son los que usa el motor
        // (TrackSector.desdeSegmento con 20 segmentos), no unos inventados.
        for (int s = 0; s < 3; s++) {
            trazarTramo(g, encaje, CORTES_SECTOR[s], CORTES_SECTOR[s + 1],
                    SECTORES[s], anchoPista, true);
        }

        // Trazada iluminada: una veta clara por el centro del asfalto.
        g.setGlobalAlpha(0.30);
        trazarTramo(g, encaje, 0, 1, Color.WHITE, anchoPista * 0.28, false);
        g.setGlobalAlpha(1);

        pintarMeta(g, encaje, anchoPista);
    }

    /** Rejilla técnica del fondo: da profundidad sin competir con la pista. */
    private void pintarRejilla(GraphicsContext g, double w, double h) {
        g.setStroke(REJILLA);
        g.setLineWidth(1);
        for (double x = PASO_REJILLA; x < w; x += PASO_REJILLA) {
            g.strokeLine(x, 0, x, h);
        }
        for (double y = PASO_REJILLA; y < h; y += PASO_REJILLA) {
            g.strokeLine(0, y, w, y);
        }
    }

    private double anchoDePista(double w, double h) {
        return Math.max(9, Math.min(20, Math.min(w, h) / 18));
    }

    private void trazarTramo(GraphicsContext g, TrackLayout.Encaje encaje,
                             double desde, double hasta, Color color,
                             double grosor, boolean corteRecto) {
        g.setStroke(color);
        g.setLineWidth(grosor);
        g.setLineCap(corteRecto ? javafx.scene.shape.StrokeLineCap.BUTT
                : javafx.scene.shape.StrokeLineCap.ROUND);
        g.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
        g.beginPath();
        int pasos = Math.max(60, (int) ((hasta - desde) * 600));
        for (int i = 0; i <= pasos; i++) {
            TrackLayout.Punto p = trazado.puntoEn(desde + (hasta - desde) * i / (double) pasos);
            double x = encaje.x(p.x());
            double y = encaje.y(p.y());
            if (i == 0) {
                g.moveTo(x, y);
            } else {
                g.lineTo(x, y);
            }
        }
        g.stroke();
    }

    private void pintarMeta(GraphicsContext g, TrackLayout.Encaje encaje, double anchoPista) {
        TrackLayout.Punto meta = trazado.puntoEn(0);
        double celda = anchoPista / 3.0;
        g.save();
        g.translate(encaje.x(meta.x()), encaje.y(meta.y()));
        g.rotate(Math.toDegrees(meta.angulo()));
        for (int fila = 0; fila < 3; fila++) {
            for (int columna = 0; columna < 2; columna++) {
                g.setFill((fila + columna) % 2 == 0 ? Color.WHITE : Color.web("#15151E"));
                g.fillRect(columna * celda - celda, fila * celda - anchoPista / 2.0,
                        celda, celda);
            }
        }
        g.restore();
    }

    /**
     * Valor que se pinta bajo cada rótulo de sector (por ejemplo "+0.038" o el
     * mejor tiempo del sector). Lo calcula la pantalla, que es quien tiene los
     * tiempos por sector; aquí solo se dibuja. Un hueco a {@code null} deja el
     * rótulo sin cifra.
     */
    public void setEtiquetasSector(String[] valores) {
        this.etiquetasSector = valores == null ? new String[3] : valores.clone();
        pintarCoches(System.nanoTime());
    }

    /** Rótulos flotantes de sector, junto al tramo que describen. */
    private void pintarEtiquetasSector(GraphicsContext g, TrackLayout.Encaje encaje,
                                       double anchoPista) {
        for (int s = 0; s < 3; s++) {
            double medio = (CORTES_SECTOR[s] + CORTES_SECTOR[s + 1]) / 2.0;
            TrackLayout.Punto p = trazado.puntoEn(medio);
            // Se desplaza por la normal para no taparse con la propia pista.
            double nx = -Math.sin(p.angulo());
            double ny = Math.cos(p.angulo());
            double x = encaje.x(p.x()) + nx * anchoPista * 2.0;
            double y = encaje.y(p.y()) + ny * anchoPista * 2.0;

            String titulo = "SECTOR " + (s + 1);
            String valor = etiquetasSector.length > s ? etiquetasSector[s] : null;

            g.setTextAlign(TextAlignment.LEFT);
            g.setTextBaseline(VPos.CENTER);
            // Filete del color del sector a la izquierda del rótulo.
            g.setFill(SECTORES[s]);
            g.fillRect(x - 6, y - 9, 2, valor == null ? 12 : 22);
            g.setFont(Font.font("Titillium Web", FontWeight.BOLD, 10));
            g.fillText(titulo, x, y - 3);
            if (valor != null) {
                g.setFill(Color.WHITE);
                g.setFont(Font.font("Titillium Web", FontWeight.BOLD, 12));
                g.fillText(valor, x, y + 10);
            }
        }
    }

    private void mensaje(GraphicsContext g, double w, double h, String texto) {
        g.setFill(TENUE);
        g.setFont(Font.font("Titillium Web", FontWeight.NORMAL, 13));
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.CENTER);
        g.fillText(texto, w / 2, h / 2);
    }

    // --------------------------------------------------------------- coches

    private void limpiarCoches() {
        GraphicsContext g = lienzoCoches.getGraphicsContext2D();
        g.clearRect(0, 0, Math.max(lienzoCoches.getWidth(), 1),
                Math.max(lienzoCoches.getHeight(), 1));
    }

    /** Estado visible ahora mismo, ya interpolado. Es el punto de partida del siguiente tramo. */
    private MapaProgreso.Estado instantanea() {
        MapaProgreso.Estado destino = objetivo.get();
        if (destino == null) {
            return null;
        }
        if (anterior == null) {
            return destino;
        }
        double avance = MapaProgreso.suavizar(
                (System.nanoTime() - inicioEaseNanos) / (double) duracionEaseNanos);
        return mezclar(anterior, destino, avance);
    }

    private static MapaProgreso.Estado mezclar(MapaProgreso.Estado origen,
                                               MapaProgreso.Estado destino, double avance) {
        List<MapaProgreso.Marcador> mezcla = new java.util.ArrayList<>(
                destino.marcadores().size());
        for (MapaProgreso.Marcador m : destino.marcadores()) {
            double desde = fraccionDe(origen, m.pilotoId(), m.fraccion());
            mezcla.add(new MapaProgreso.Marcador(m.pilotoId(), m.piloto(), m.equipo(),
                    m.posicion(), MapaProgreso.interpolar(desde, m.fraccion(), avance),
                    m.gapSegundos(), m.valida(), m.fuera()));
        }
        return new MapaProgreso.Estado(destino.segmento(), destino.totalSegmentos(), mezcla);
    }

    private static double fraccionDe(MapaProgreso.Estado estado, int pilotoId, double siFalta) {
        if (estado == null) {
            return siFalta;
        }
        for (MapaProgreso.Marcador m : estado.marcadores()) {
            if (m.pilotoId() == pilotoId) {
                return m.fraccion();
            }
        }
        return siFalta;
    }

    private void pintarCoches(long ahora) {
        GraphicsContext g = lienzoCoches.getGraphicsContext2D();
        double w = lienzoCoches.getWidth();
        double h = lienzoCoches.getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        // Limpiar, no velar: este lienzo es transparente y un velo oscuro
        // apagaría la pista que hay debajo.
        g.clearRect(0, 0, w, h);

        MapaProgreso.Estado destino = objetivo.get();
        if (trazado == null || destino == null) {
            return;
        }

        double avance = MapaProgreso.suavizar(
                (ahora - inicioEaseNanos) / (double) duracionEaseNanos);
        MapaProgreso.Estado visible = anterior == null ? destino
                : mezclar(anterior, destino, avance);

        double anchoPista = anchoDePista(w, h);
        TrackLayout.Encaje encaje = trazado.encajarEn(w, h, anchoPista + 18);
        double radio = Math.max(4, Math.min(7.5, Math.min(w, h) / 44));

        pintarEtiquetasSector(g, encaje, anchoPista);

        // El líder y el piloto del usuario se pintan al final, encima de todos.
        List<MapaProgreso.Marcador> orden = new java.util.ArrayList<>(visible.marcadores());
        orden.sort((a, b) -> Integer.compare(prioridad(a), prioridad(b)));
        int ultima = visible.marcadores().size();
        for (MapaProgreso.Marcador m : orden) {
            pintarCoche(g, encaje, m, radio, anchoPista, ultima);
        }

        // Cuando el tramo se ha consumido y la sesión terminó, no hay nada más
        // que animar: parar el temporizador deja el mapa a coste cero.
        if (!sesionViva && avance >= 1) {
            temporizador.stop();
        }
    }

    private int prioridad(MapaProgreso.Marcador m) {
        if (esDelUsuario(m)) {
            return 3;
        }
        if (java.util.Objects.equals(pilotoResaltado.get(), m.pilotoId())) {
            return 2;
        }
        return m.posicion() == 1 ? 1 : 0;
    }

    private boolean esDelUsuario(MapaProgreso.Marcador m) {
        return pilotoUsuario != null && pilotoUsuario == m.pilotoId();
    }

    private void pintarCoche(GraphicsContext g, TrackLayout.Encaje encaje,
                             MapaProgreso.Marcador m, double radio, double anchoPista,
                             int ultimaPosicion) {
        TrackLayout.Punto p = trazado.puntoEn(m.fraccion());
        double x = encaje.x(p.x());
        double y = encaje.y(p.y());
        boolean resaltado = java.util.Objects.equals(pilotoResaltado.get(), m.pilotoId());
        double r = resaltado ? radio + 2 : radio;

        if (!m.valida()) {
            // Coche parado: aro hueco y apagado, sin etiqueta.
            g.setGlobalAlpha(0.35);
            g.setStroke(m.fuera() ? TENUE : Color.web(TeamColors.hex(m.equipo())));
            g.setLineWidth(1.6);
            g.strokeOval(x - r, y - r, r * 2, r * 2);
            g.setGlobalAlpha(1);
            return;
        }

        Color color = Color.web(TeamColors.hex(m.equipo()));

        // Halo oscuro: separa el marcador del asfalto, que es casi del mismo tono.
        g.setFill(Color.rgb(0, 0, 0, 0.55));
        g.fillOval(x - r - 2, y - r - 2, (r + 2) * 2, (r + 2) * 2);

        // Resplandor del color de la escudería, el mismo recurso que la pista.
        g.setGlobalAlpha(resaltado ? 0.45 : 0.28);
        g.setFill(color);
        g.fillOval(x - r * 2, y - r * 2, r * 4, r * 4);
        g.setGlobalAlpha(1);

        g.setFill(color);
        g.fillOval(x - r, y - r, r * 2, r * 2);

        boolean destacado = esDelUsuario(m) || resaltado;
        g.setStroke(esDelUsuario(m) ? MARCA : resaltado ? Color.WHITE
                : m.posicion() == 1 ? Color.WHITE : Color.web("#0A0A0A"));
        g.setLineWidth(destacado ? 2.2 : 1.3);
        g.strokeOval(x - r, y - r, r * 2, r * 2);

        // Etiqueta solo para los que importan: veinte etiquetas serían barro.
        if (resaltado || esDelUsuario(m) || m.posicion() == 1 || m.posicion() == ultimaPosicion) {
            etiqueta(g, m, x, y, r, anchoPista, p.angulo());
        }
    }

    private void etiqueta(GraphicsContext g, MapaProgreso.Marcador m, double x, double y,
                          double radio, double anchoPista, double angulo) {
        String texto = "P" + m.posicion();
        // Se desplaza por la normal para no tapar la pista.
        double nx = -Math.sin(angulo);
        double ny = Math.cos(angulo);
        double ex = x + nx * (radio + anchoPista * 0.6);
        double ey = y + ny * (radio + anchoPista * 0.6);
        double ancho = 9 + texto.length() * 6;

        g.setFill(Color.web(TeamColors.accesible(m.equipo())));
        g.fillRoundRect(ex - ancho / 2, ey - 8, ancho, 15, 5, 5);
        g.setFill(Color.WHITE);
        // JetBrains Mono no está empaquetada: solo Titillium Web se carga en App.
        g.setFont(Font.font("Titillium Web", FontWeight.BOLD, 9));
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.CENTER);
        g.fillText(texto, ex, ey - 1);
    }

    private Integer pilotoBajoElRaton(double x, double y) {
        MapaProgreso.Estado visible = instantanea();
        double w = lienzoCoches.getWidth();
        double h = lienzoCoches.getHeight();
        if (trazado == null || visible == null || w <= 0 || h <= 0) {
            return null;
        }
        double anchoPista = anchoDePista(w, h);
        TrackLayout.Encaje encaje = trazado.encajarEn(w, h, anchoPista + 14);
        double radio = Math.max(3.5, Math.min(6.5, Math.min(w, h) / 48)) + 4;

        Integer mejor = null;
        double distanciaMejor = radio;
        for (MapaProgreso.Marcador m : visible.marcadores()) {
            TrackLayout.Punto p = trazado.puntoEn(m.fraccion());
            double d = Math.hypot(encaje.x(p.x()) - x, encaje.y(p.y()) - y);
            if (d <= distanciaMejor) {
                distanciaMejor = d;
                mejor = m.pilotoId();
            }
        }
        return mejor;
    }
}
