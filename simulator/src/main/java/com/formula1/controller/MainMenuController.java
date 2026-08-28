package com.formula1.controller;

import com.formula1.data.DataStore;
import com.formula1.util.AudioManager;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Text;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.util.Duration;

import java.io.InputStream;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Menú principal: hub estilo videojuego que se muestra tras la intro.
 *
 * La geometría se aplica aquí y no en el FXML porque el mockup
 * (docs/assets/menu_mockup.png, 1672x941) no es una rejilla regular: las
 * cinco tarjetas tienen anchos, alturas y bordes superiores distintos, y
 * todo —incluidos los cuerpos de letra— escala con la ventana.
 */
public class MainMenuController {

    private static final Duration HOVER = Duration.millis(160);

    /** Alto y ancho del mockup del que salen todas las proporciones. */
    private static final double REF_ALTO = 941;
    private static final double REF_ANCHO = 1672;

    /** Corte a 45 grados de la esquina superior derecha, constante en px. */
    private static final double CHAFLAN = 20;

    private static final double VIDEO_ANCHO = 1280;
    private static final double VIDEO_ALTO = 720;
    private static final double FONDO_ANCHO = 1920;
    private static final double FONDO_ALTO = 1080;

    /** Confirmación: entrar a una sección principal. */
    private static final String SFX_PRINCIPAL = "/audio/sound1.mp3";
    /** Acción secundaria: ajustes, salir. */
    private static final String SFX_SECUNDARIO = "/audio/sound2.mp3";

    /**
     * El wordmark ocupa solo la banda central de LogoF1.png (920x800): sin
     * recortar el margen transparente el logo saldría diminuto.
     */
    private static final Rectangle2D RECORTE_LOGO = new Rectangle2D(85, 295, 750, 190);

    /** x, y, ancho y alto de cada tarjeta como fracción del lienzo. */
    private record Caja(double x, double y, double w, double h) { }

    private static final Caja CAJA_CLASIFICACION = new Caja(0.02273, 0.42295, 0.23923, 0.43783);
    private static final Caja CAJA_GESTION       = new Caja(0.27452, 0.45483, 0.20155, 0.38789);
    private static final Caja CAJA_EXPLORAR      = new Caja(0.48565, 0.45483, 0.17943, 0.38789);
    private static final Caja CAJA_HISTORIAL     = new Caja(0.67464, 0.45483, 0.16507, 0.38789);
    private static final Caja CAJA_AJUSTES       = new Caja(0.84809, 0.45377, 0.12560, 0.23379);

    @FXML private StackPane raiz;
    @FXML private StackPane capaFondo;
    @FXML private BorderPane capaContenido;
    @FXML private Pane capaTarjetas;

    @FXML private ImageView logoF1;
    @FXML private Label lblAnio;
    @FXML private HBox cajaTemporada;
    @FXML private HBox cajaStats;
    @FXML private HBox cajaSalir;
    @FXML private Region fileteSalir;

    @FXML private StackPane envolturaClasificacion;
    @FXML private StackPane envolturaGestion;
    @FXML private StackPane envolturaExplorar;
    @FXML private StackPane envolturaHistorial;
    @FXML private StackPane envolturaAjustes;

    @FXML private Button tileClasificacion;
    @FXML private Button tileGestion;
    @FXML private Button tileExplorar;
    @FXML private Button tileHistorial;
    @FXML private Button tileAjustes;

    @FXML private VBox cuerpoClasificacion;
    @FXML private VBox cuerpoGestion;
    @FXML private VBox cuerpoExplorar;
    @FXML private VBox cuerpoHistorial;
    @FXML private VBox cuerpoAjustes;

    @FXML private Label tituloClasificacion;
    @FXML private Label tituloGestion;
    @FXML private Label tituloExplorar;
    @FXML private Label tituloHistorial;
    @FXML private Label tituloAjustes;

    @FXML private Label subtituloClasificacion;
    @FXML private Label subtituloGestion;
    @FXML private Label subtituloExplorar;
    @FXML private Label subtituloHistorial;

    @FXML private Region fileteClasificacion;

    @FXML private Scale escalaIconoClasificacion;
    @FXML private Scale escalaIconoGestion;
    @FXML private Scale escalaIconoExplorar;
    @FXML private Scale escalaIconoHistorial;
    @FXML private Scale escalaIconoAjustes;

    /** App inyecta aquí cómo pasar de este menú al shell, para no depender de él. */
    private Consumer<Runnable> alEntrarAlShell = irA -> { };

    private final AtomicBoolean respaldoActivo = new AtomicBoolean(false);
    private MediaPlayer reproductorFondo;
    private Timeline kenBurns;
    private Timeline sondeoDatos;

    @FXML
    public void initialize() {
        montarFondo();
        montarCapaDeTarjetas();
        montarChaflanes();
        montarEscalado();
        actualizarStats();
        aplicarHover(envolturaClasificacion);
        aplicarHover(envolturaGestion);
        aplicarHover(envolturaExplorar);
        aplicarHover(envolturaHistorial);
        aplicarHover(envolturaAjustes);
    }

    public void setAlEntrarAlShell(Consumer<Runnable> callback) {
        this.alEntrarAlShell = callback;
    }

    // --- geometría --------------------------------------------------------

    /**
     * Coloca las tarjetas por fracciones del lienzo.
     *
     * Los hijos van sin gestionar: si estuvieran gestionados, el propio
     * {@code Pane} los recolocaría en cada pasada de layout y pisaría estas
     * posiciones.
     */
    private void montarCapaDeTarjetas() {
        Map<Region, Caja> cajas = new LinkedHashMap<>();
        cajas.put(envolturaClasificacion, CAJA_CLASIFICACION);
        cajas.put(envolturaGestion, CAJA_GESTION);
        cajas.put(envolturaExplorar, CAJA_EXPLORAR);
        cajas.put(envolturaHistorial, CAJA_HISTORIAL);
        cajas.put(envolturaAjustes, CAJA_AJUSTES);

        cajas.keySet().forEach(nodo -> nodo.setManaged(false));

        InvalidationListener recolocar = observable -> {
            double w = capaTarjetas.getWidth();
            double h = capaTarjetas.getHeight();
            if (w <= 0 || h <= 0) {
                return;
            }
            cajas.forEach((nodo, caja) ->
                    nodo.resizeRelocate(caja.x() * w, caja.y() * h, caja.w() * w, caja.h() * h));
            // Los títulos se miden contra el ancho ya asignado a cada tarjeta.
            aplicarTipografiaTarjetas();
        };
        capaTarjetas.widthProperty().addListener(recolocar);
        capaTarjetas.heightProperty().addListener(recolocar);
        recolocar.invalidated(null);
    }

    private void montarChaflanes() {
        for (Button tile : new Button[]{tileClasificacion, tileGestion, tileExplorar,
                                        tileHistorial, tileAjustes}) {
            tile.setClip(chaflanDe(tile));
        }
        // El borde de AJUSTES no puede ser CSS: el clip lo recortaría sin
        // seguir el chaflán. Va como contorno hermano con los mismos puntos.
        Polygon contorno = chaflanDe(tileAjustes);
        contorno.setFill(null);
        contorno.setStroke(Color.web("#6B6A69", 0.85));
        contorno.setStrokeWidth(1.4);
        contorno.setStrokeType(StrokeType.INSIDE);
        contorno.setMouseTransparent(true);
        contorno.setManaged(false);
        StackPane.setAlignment(contorno, Pos.TOP_LEFT);
        envolturaAjustes.getChildren().add(contorno);
    }

    /** Rectángulo con la esquina superior derecha cortada a 45 grados. */
    private static Polygon chaflanDe(Region region) {
        Polygon poligono = new Polygon();
        Runnable rehacer = () -> {
            double w = region.getWidth();
            double h = region.getHeight();
            if (w <= 0 || h <= 0) {
                return;
            }
            double c = Math.min(CHAFLAN, Math.min(w, h) / 2);
            poligono.getPoints().setAll(
                    0.0,   0.0,
                    w - c, 0.0,
                    w,     c,
                    w,     h,
                    0.0,   h);
        };
        region.widthProperty().addListener((o, a, b) -> rehacer.run());
        region.heightProperty().addListener((o, a, b) -> rehacer.run());
        rehacer.run();
        return poligono;
    }

    // --- tipografía y escalado -------------------------------------------

    /** Todo lo que depende del tamaño de la ventana se recalcula aquí. */
    private void montarEscalado() {
        InvalidationListener escalar = observable -> aplicarEscalado();
        raiz.widthProperty().addListener(escalar);
        raiz.heightProperty().addListener(escalar);

        // El Button no recibe su tamaño hasta la pasada de layout siguiente a
        // la de la envoltura, asi que la tipografia de cada tarjeta se
        // recalcula cuando el propio Button cambia de tamaño.
        InvalidationListener porTarjeta = observable -> aplicarTipografiaTarjetas();
        for (Button tile : new Button[]{tileClasificacion, tileGestion, tileExplorar,
                                        tileHistorial, tileAjustes}) {
            tile.widthProperty().addListener(porTarjeta);
            tile.heightProperty().addListener(porTarjeta);
        }

        aplicarEscalado();
    }

    private void aplicarEscalado() {
        double h = raiz.getHeight();
        double w = raiz.getWidth();
        if (h <= 0 || w <= 0) {
            return;
        }

        logoF1.setFitHeight(px(35, h));
        estiloFuente(lblAnio, px(48, h));

        rellenarTracking(cajaTemporada, "TEMPORADA 2025", "menu-temporada-glifo", px(11, h), w);
        rellenarTracking(cajaSalir, "SALIR", "menu-salir-glifo", px(13, h), w);
        aplicarStats(DataStore.getInstance());

        fileteSalir.setPrefSize(w * 33 / REF_ANCHO, Math.max(1, px(2, h)));
        fileteSalir.setMaxSize(w * 33 / REF_ANCHO, Math.max(1, px(2, h)));
        fileteClasificacion.setPrefSize(w * 58 / REF_ANCHO, Math.max(2, px(4, h)));
        fileteClasificacion.setMaxSize(w * 58 / REF_ANCHO, Math.max(2, px(4, h)));

        escalaIcono(escalaIconoClasificacion, px(98, h));
        escalaIcono(escalaIconoGestion, px(77, h));
        escalaIcono(escalaIconoExplorar, px(77, h));
        escalaIcono(escalaIconoHistorial, px(77, h));
        escalaIcono(escalaIconoAjustes, px(31, h));

        estiloFuente(subtituloClasificacion, px(15, h));
        estiloFuente(subtituloGestion, px(14, h));
        estiloFuente(subtituloExplorar, px(14, h));
        estiloFuente(subtituloHistorial, px(14, h));

        aplicarTipografiaTarjetas();
    }

    /**
     * Cuerpo y título de cada tarjeta. Va aparte porque depende del ancho ya
     * asignado a la tarjeta, que se resuelve en la pasada de layout de
     * {@code capaTarjetas}, no cuando cambia el tamaño de la ventana.
     */
    private void aplicarTipografiaTarjetas() {
        double h = raiz.getHeight();
        if (h <= 0) {
            return;
        }
        ajustarCuerpo(cuerpoClasificacion, tileClasificacion);
        ajustarCuerpo(cuerpoGestion, tileGestion);
        ajustarCuerpo(cuerpoExplorar, tileExplorar);
        ajustarCuerpo(cuerpoHistorial, tileHistorial);
        ajustarCuerpo(cuerpoAjustes, tileAjustes);

        ajustarTitulo(tituloClasificacion, tileClasificacion, px(97, h), 0.62);
        ajustarTitulo(tituloGestion, tileGestion, px(69, h), 0.72);
        ajustarTitulo(tituloExplorar, tileExplorar, px(69, h), 0.72);
        ajustarTitulo(tituloHistorial, tileHistorial, px(69, h), 0.72);
        // AJUSTES es la tarjeta secundaria: en el mockup su titulo es
        // claramente menor que el de las demas (cap 30 frente a 48).
        ajustarTitulo(tituloAjustes, tileAjustes, px(34, h), 0.88);
    }

    /** Convierte una medida del mockup a píxeles reales de la ventana. */
    private static double px(double medidaEnMockup, double altoActual) {
        return medidaEnMockup * altoActual / REF_ALTO;
    }

    private static void estiloFuente(Label etiqueta, double tamano) {
        etiqueta.setStyle("-fx-font-size: " + redondear(tamano) + "px;");
    }

    private static void escalaIcono(Scale escala, double ladoEnPx) {
        double factor = ladoEnPx / 24.0;   // las rutas viven en una caja 0 0 24 24
        escala.setX(factor);
        escala.setY(factor);
    }

    /** El graphic del Button debe ocupar todo el área útil, no su tamaño preferido. */
    private static void ajustarCuerpo(VBox cuerpo, Button tile) {
        double w = tile.getWidth() - tile.getInsets().getLeft() - tile.getInsets().getRight();
        double h = tile.getHeight() - tile.getInsets().getTop() - tile.getInsets().getBottom();
        if (w <= 0 || h <= 0) {
            return;
        }
        cuerpo.setPrefSize(w, h);
        cuerpo.setMinSize(w, h);
    }

    /**
     * El mockup usa una condensada que Titillium no es. Reproducir su métrica
     * exacta exigiría comprimir al 54 % y deformaría la letra, así que se
     * reproduce el efecto —título de sangrado a sangrado— con una compresión
     * suave acotada por {@code sxMinimo}.
     */
    private void ajustarTitulo(Label titulo, Button tile, double cuerpo, double sxMinimo) {
        double util = tile.getWidth() - tile.getInsets().getLeft() - tile.getInsets().getRight();
        if (util <= 0) {
            return;
        }
        titulo.setWrapText(false);
        // Sin esto el Label se recorta con puntos suspensivos: la Scale
        // comprime lo que se ve, pero no reduce los limites de layout.
        titulo.setMinWidth(Region.USE_PREF_SIZE);
        titulo.setMaxWidth(Double.MAX_VALUE);

        estiloFuente(titulo, cuerpo);
        titulo.applyCss();
        double natural = anchoNatural(titulo);
        if (natural <= 0) {
            return;
        }

        double sx = util / natural;
        if (sx >= 1) {
            sx = 1;
        } else if (sx < sxMinimo) {
            // Ni comprimiendo al maximo cabe, asi que se baja el cuerpo lo
            // justo para que entre sin deformar mas la letra.
            estiloFuente(titulo, cuerpo * (sx / sxMinimo));
            titulo.applyCss();
            sx = sxMinimo;
        }
        // Pivote en el borde izquierdo: setScaleX comprimiría desde el centro
        // y despegaría el título del sangrado.
        titulo.getTransforms().setAll(new Scale(sx, 1, 0, 0));
    }

    private static double anchoNatural(Label etiqueta) {
        Text sonda = new Text(etiqueta.getText());
        sonda.setFont(etiqueta.getFont());
        return sonda.getLayoutBounds().getWidth();
    }

    /**
     * Compone el tracking carácter a carácter: JavaFX 17 no tiene
     * {@code -fx-letter-spacing}, solo {@code -fx-line-spacing}.
     */
    private static void rellenarTracking(HBox destino, String texto, String claseCss,
                                         double tamano, double anchoActual) {
        destino.setSpacing(anchoActual * 4 / REF_ANCHO);
        destino.getChildren().clear();
        for (char c : texto.toCharArray()) {
            Text glifo = new Text(c == ' ' ? " " : String.valueOf(c));
            glifo.getStyleClass().add(claseCss);
            glifo.setStyle("-fx-font-size: " + redondear(tamano) + "px;");
            destino.getChildren().add(glifo);
        }
    }

    private static long redondear(double valor) {
        return Math.max(1, Math.round(valor));
    }

    // --- datos ------------------------------------------------------------

    private void actualizarStats() {
        DataStore datos = DataStore.getInstance();
        if (datos.estaCargado()) {
            aplicarStats(datos);
            return;
        }
        // La carga arrancó en paralelo a la intro; si aún no terminó, se
        // sondea brevemente en vez de dejar los contadores en blanco.
        sondeoDatos = new Timeline(new KeyFrame(Duration.millis(250), e -> {
            if (datos.estaCargado()) {
                aplicarStats(datos);
                sondeoDatos.stop();
            }
        }));
        sondeoDatos.setCycleCount(20);
        sondeoDatos.play();
    }

    private void aplicarStats(DataStore datos) {
        double h = raiz.getHeight();
        double w = raiz.getWidth();
        if (h <= 0 || w <= 0) {
            return;
        }
        String linea = datos.estaCargado()
                ? datos.pilotos().size() + " PILOTOS · "
                        + datos.equipos().size() + " EQUIPOS · "
                        + datos.circuitos().size() + " CIRCUITOS"
                : "— PILOTOS · — EQUIPOS · — CIRCUITOS";
        rellenarTracking(cajaStats, linea, "menu-stat-glifo", px(15, h), w);
    }

    // --- fondo de vídeo ---------------------------------------------------

    private void montarFondo() {
        cargarLogo();
        capaFondo.setClip(recorteDe(capaFondo));

        URL recurso = getClass().getResource("/videos/menu-loop.mp4");
        if (recurso == null) {
            activarRespaldo();
            return;
        }
        try {
            Media media = new Media(recurso.toExternalForm());
            if (media.getError() != null) {
                activarRespaldo();
                return;
            }
            media.setOnError(this::activarRespaldo);

            MediaPlayer reproductor = new MediaPlayer(media);
            if (reproductor.getError() != null) {
                activarRespaldo();
                return;
            }
            reproductor.setOnError(this::activarRespaldo);
            reproductor.setOnHalted(this::activarRespaldo);
            reproductor.statusProperty().addListener((o, previo, estado) -> {
                if (estado == MediaPlayer.Status.HALTED) {
                    activarRespaldo();
                }
            });
            reproductor.setCycleCount(MediaPlayer.INDEFINITE);
            reproductor.setMute(true);
            reproductor.setAutoPlay(true);

            MediaView vista = new MediaView(reproductor);
            vista.setPreserveRatio(false);
            vista.setSmooth(true);
            cubrir(vista.fitWidthProperty(), vista.fitHeightProperty(), VIDEO_ANCHO, VIDEO_ALTO);
            capaFondo.getChildren().setAll(vista);
            reproductorFondo = reproductor;

            // Red de seguridad: en esta máquina los libavplugin de JavaFX 17
            // piden libavcodec.so.54-59 y solo existe la .63, así que el H.264
            // puede no arrancar nunca sin emitir un error explícito.
            PauseTransition vigilante = new PauseTransition(Duration.seconds(2.5));
            vigilante.setOnFinished(e -> {
                if (reproductor.getStatus() != MediaPlayer.Status.PLAYING) {
                    activarRespaldo();
                }
            });
            vigilante.play();
        } catch (RuntimeException | Error sinCodec) {
            // Incluye Error a propósito: si faltan los .so nativos, lo que
            // salta es UnsatisfiedLinkError, no una RuntimeException.
            activarRespaldo();
        }
    }

    /** Idempotente y siempre en el hilo de FX: los avisos de Media pueden no serlo. */
    private void activarRespaldo() {
        if (!respaldoActivo.compareAndSet(false, true)) {
            return;
        }
        Platform.runLater(() -> {
            if (reproductorFondo != null) {
                try {
                    reproductorFondo.stop();
                    reproductorFondo.dispose();
                } catch (RuntimeException ignorado) {
                    // Un reproductor que ya falló no tiene por qué cerrar limpio.
                }
                reproductorFondo = null;
            }
            capaFondo.getChildren().setAll(construirRespaldo());
        });
    }

    /** Fotograma fijo con paneo y zoom lentos, para que el fondo no quede muerto. */
    private Node construirRespaldo() {
        InputStream flujo = getClass().getResourceAsStream("/images/menu-fondo.jpg");
        if (flujo == null) {
            return new Region();      // el scrim ya deja el fondo en negro
        }
        ImageView foto = new ImageView(new Image(flujo, FONDO_ANCHO, 0, true, true));
        foto.setPreserveRatio(false);
        foto.setSmooth(true);
        cubrir(foto.fitWidthProperty(), foto.fitHeightProperty(), FONDO_ANCHO, FONDO_ALTO);

        Scale zoom = new Scale(1, 1, 0, 0);
        Translate paneo = new Translate();
        foto.getTransforms().addAll(zoom, paneo);

        kenBurns = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(zoom.xProperty(), 1.00),
                        new KeyValue(zoom.yProperty(), 1.00),
                        new KeyValue(paneo.xProperty(), 0.0),
                        new KeyValue(paneo.yProperty(), 0.0)),
                new KeyFrame(Duration.seconds(17),
                        new KeyValue(zoom.xProperty(), 1.075, Interpolator.EASE_BOTH),
                        new KeyValue(zoom.yProperty(), 1.075, Interpolator.EASE_BOTH),
                        new KeyValue(paneo.xProperty(), -46.0, Interpolator.EASE_BOTH),
                        new KeyValue(paneo.yProperty(), -22.0, Interpolator.EASE_BOTH)));
        kenBurns.setAutoReverse(true);
        kenBurns.setCycleCount(Animation.INDEFINITE);
        kenBurns.play();
        return foto;
    }

    /**
     * Comportamiento «cover»: escala por el lado que se queda corto para que
     * el nodo desborde y el clip recorte. Con ambas dimensiones fijadas y
     * preserveRatio, JavaFX haría «contain» y dejaría bandas negras.
     */
    private void cubrir(DoubleProperty anchoDestino, DoubleProperty altoDestino,
                        double nativoAncho, double nativoAlto) {
        DoubleBinding factor = Bindings.createDoubleBinding(
                () -> Math.max(capaFondo.getWidth() / nativoAncho,
                               capaFondo.getHeight() / nativoAlto),
                capaFondo.widthProperty(), capaFondo.heightProperty());
        anchoDestino.bind(factor.multiply(nativoAncho));
        altoDestino.bind(factor.multiply(nativoAlto));
    }

    private static Rectangle recorteDe(Region region) {
        Rectangle recorte = new Rectangle();
        recorte.widthProperty().bind(region.widthProperty());
        recorte.heightProperty().bind(region.heightProperty());
        return recorte;
    }

    private void cargarLogo() {
        InputStream flujo = getClass().getResourceAsStream("/images/LogoF1.png");
        if (flujo == null) {
            return;
        }
        logoF1.setImage(new Image(flujo));
        logoF1.setViewport(RECORTE_LOGO);
    }

    /** Suelta el vídeo y las animaciones al abandonar el menú. */
    public void liberar() {
        if (reproductorFondo != null) {
            try {
                reproductorFondo.stop();
                reproductorFondo.dispose();
            } catch (RuntimeException ignorado) {
                // Cerrar el menú nunca debe fallar por el reproductor.
            }
            reproductorFondo = null;
        }
        if (kenBurns != null) {
            kenBurns.stop();
            kenBurns = null;
        }
        if (sondeoDatos != null) {
            sondeoDatos.stop();
            sondeoDatos = null;
        }
    }

    // --- interacción ------------------------------------------------------

    private void aplicarHover(StackPane envoltura) {
        envoltura.setOnMouseEntered(e -> escalar(envoltura, 1.025));
        envoltura.setOnMouseExited(e -> escalar(envoltura, 1.0));
    }

    private void escalar(StackPane envoltura, double destino) {
        ScaleTransition zoom = new ScaleTransition(HOVER, envoltura);
        zoom.setToX(destino);
        zoom.setToY(destino);
        zoom.setInterpolator(Interpolator.EASE_BOTH);
        zoom.play();
    }

    @FXML
    private void onClasificacion() {
        irAlShell(ShellController::irACarrera);
    }

    @FXML
    private void onGestionEquipos() {
        irAlShell(ShellController::irAGestion);
    }

    @FXML
    private void onExplorar() {
        irAlShell(ShellController::irAExplorar);
    }

    @FXML
    private void onHistorial() {
        irAlShell(ShellController::irAHistorial);
    }

    @FXML
    private void onAjustes() {
        AudioManager.reproducirSfx(SFX_SECUNDARIO);
        AjustesDialog.mostrar();
    }

    @FXML
    private void onSalir() {
        AudioManager.reproducirSfx(SFX_SECUNDARIO);
        if (Navigator.confirmar("¿Quieres salir del simulador?")) {
            Platform.exit();
        }
    }

    private void irAlShell(Runnable destinoEnShell) {
        AudioManager.reproducirSfx(SFX_PRINCIPAL);
        alEntrarAlShell.accept(destinoEnShell);
    }
}
