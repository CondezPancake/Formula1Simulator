package com.formula1.controller;

import com.formula1.data.DataStore;
import com.formula1.util.AudioManager;
import com.formula1.util.Animaciones;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
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
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Text;
import javafx.scene.transform.Scale;
import javafx.util.Duration;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    /** Alto y ancho del mockup del que salen todas las proporciones. */
    private static final double REF_ALTO = 941;
    private static final double REF_ANCHO = 1672;

    /** Corte a 45 grados de la esquina superior derecha, constante en px. */
    private static final double CHAFLAN = 20;

    /**
     * Cota superior de cordura para cualquier medida de layout. En algunos
     * compositores (visto en Hyprland/Wayland) {@code Stage.setMaximized}
     * hace que, durante un único pulso, la ventana informe un ancho o alto
     * disparatado (miles de millones de px) antes de que llegue el tamaño
     * real. Sin este tope, ese valor se multiplica directamente en
     * {@code resizeRelocate} y revienta el cálculo interno de ajuste de
     * texto de JavaFX de forma permanente (no se recupera solo).
     */
    private static final double LADO_MAXIMO_RAZONABLE = 8000;

    /** Ancho al que se piden las fotos del fondo, para acotar memoria. */
    private static final double ANCHO_CARGA_FONDO = 1920;

    /** Fotos del slideshow de fondo, en el orden en que se ciclan. */
    private static final String[] FONDOS = {
        "/images/menu-fondo/fondo-01.jpg",
        "/images/menu-fondo/fondo-02.jpg",
        "/images/menu-fondo/fondo-03.jpg",
    };

    /** Confirmación: elegir una sección principal. Es el sonido protagonista. */
    private static final String SFX_PRINCIPAL = "/audio/sound1.mp3";
    /** Acciones secundarias: ajustes y salir. */
    private static final String SFX_SECUNDARIO = "/audio/sound2.mp3";
    /** Acompañamiento al recorrer las tarjetas, por debajo del de confirmación. */
    private static final String SFX_HOVER = "/audio/sound2.mp3";

    /** Espera mínima entre dos sonidos de hover consecutivos. */
    private static final long ESPERA_SFX_HOVER_MS = 350;
    /** El hover solo acompaña: nunca debe competir con el sonido al elegir. */
    private static final double VOLUMEN_HOVER = 0.35;

    /** Opacidad de las tarjetas que no están bajo el cursor. */
    private static final double ATENUACION_HERMANA = 0.72;
    private static final double ESCALA_HOVER = 1.03;
    private static final double ESCALA_PULSO = 1.06;

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

    private Timeline cicloFondo;
    private Timeline kenBurnsActual;
    private int indiceFondo = 0;
    private Timeline sondeoDatos;
    private long ultimoSfxHoverMs = 0;

    /** Una transición viva por tarjeta: reutilizarlas evita que se pisen entre sí. */
    private final Map<StackPane, FadeTransition> fundidos = new HashMap<>();
    private final Map<StackPane, ScaleTransition> escalados = new HashMap<>();

    @FXML
    public void initialize() {
        montarFondoSlideshow();
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
            if (!medidaValida(w) || !medidaValida(h)) {
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
        if (!medidaValida(w) || !medidaValida(h)) {
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
        if (!medidaValida(tile.getWidth()) || !medidaValida(tile.getHeight())) {
            return;
        }
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
        if (!medidaValida(tile.getWidth())) {
            return;
        }
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

    /** Descarta tanto un tamaño nulo/negativo como uno disparatadamente grande. */
    private static boolean medidaValida(double v) {
        return v > 0 && v <= LADO_MAXIMO_RAZONABLE;
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

    // --- fondo: slideshow de fotos -----------------------------------------

    private void montarFondoSlideshow() {
        cargarLogo();
        capaFondo.setClip(recorteDe(capaFondo));

        List<Image> imagenes = cargarImagenesFondo();
        if (imagenes.isEmpty()) {
            capaFondo.getChildren().setAll(new Region());   // el scrim ya deja el fondo en negro
            return;
        }

        ImageView primera = crearVistaFondo(imagenes.get(0));
        capaFondo.getChildren().setAll(primera);
        kenBurnsActual = iniciarKenBurns(primera);

        if (imagenes.size() > 1) {
            cicloFondo = new Timeline(new KeyFrame(Animaciones.FONDO_HOLD, e -> avanzarFondo(imagenes)));
            cicloFondo.setCycleCount(Animation.INDEFINITE);
            cicloFondo.play();
        }
    }

    /** Cada fichero que falte se salta sin más: no hace falta que estén los tres. */
    private List<Image> cargarImagenesFondo() {
        List<Image> imagenes = new ArrayList<>();
        for (String recurso : FONDOS) {
            InputStream flujo = getClass().getResourceAsStream(recurso);
            if (flujo == null) {
                continue;
            }
            imagenes.add(new Image(flujo, ANCHO_CARGA_FONDO, 0, true, true));
        }
        return imagenes;
    }

    /** Añade la siguiente foto encima con opacidad 0 y la funde, como {@code App.cruzar}. */
    private void avanzarFondo(List<Image> imagenes) {
        indiceFondo = (indiceFondo + 1) % imagenes.size();
        ImageView siguiente = crearVistaFondo(imagenes.get(indiceFondo));
        siguiente.setOpacity(0);
        capaFondo.getChildren().add(siguiente);

        Timeline kenBurnsAnterior = kenBurnsActual;
        kenBurnsActual = iniciarKenBurns(siguiente);

        FadeTransition entrada = new FadeTransition(Animaciones.FONDO_CROSSFADE, siguiente);
        entrada.setToValue(1);
        entrada.setOnFinished(e -> {
            capaFondo.getChildren().remove(0);
            if (kenBurnsAnterior != null) {
                kenBurnsAnterior.stop();
            }
        });
        entrada.play();
    }

    private ImageView crearVistaFondo(Image imagen) {
        ImageView vista = new ImageView(imagen);
        vista.setPreserveRatio(false);
        vista.setSmooth(true);
        cubrir(vista.fitWidthProperty(), vista.fitHeightProperty(), imagen.getWidth(), imagen.getHeight());
        return vista;
    }

    /**
     * Zoom lentísimo de una sola pasada: cada foto solo vive lo que dura su
     * turno (hold + crossfade), así que no hace falta {@code autoReverse} ni
     * ciclo infinito como tenía el respaldo Ken Burns anterior.
     */
    private Timeline iniciarKenBurns(ImageView vista) {
        Scale zoom = new Scale(1, 1, 0, 0);
        vista.getTransforms().add(zoom);

        Timeline kenBurns = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(zoom.xProperty(), 1.00),
                        new KeyValue(zoom.yProperty(), 1.00)),
                new KeyFrame(Animaciones.FONDO_HOLD.add(Animaciones.FONDO_CROSSFADE),
                        new KeyValue(zoom.xProperty(), 1.03, Interpolator.EASE_BOTH),
                        new KeyValue(zoom.yProperty(), 1.03, Interpolator.EASE_BOTH)));
        kenBurns.play();
        return kenBurns;
    }

    /**
     * Comportamiento «cover»: escala por el lado que se queda corto para que
     * el nodo desborde y el clip recorte. Con ambas dimensiones fijadas y
     * preserveRatio, JavaFX haría «contain» y dejaría bandas negras.
     */
    private void cubrir(DoubleProperty anchoDestino, DoubleProperty altoDestino,
                        double nativoAncho, double nativoAlto) {
        DoubleBinding factor = Bindings.createDoubleBinding(
                () -> {
                    double w = capaFondo.getWidth();
                    double h = capaFondo.getHeight();
                    // Mismo guardarraíl que en el layout de las tarjetas: si el
                    // compositor informa un tamaño disparatado durante el
                    // pulso de maximizado, no hay que escalar la imagen a eso
                    // — se congelaría la app intentando rasterizarla.
                    if (!medidaValida(w) || !medidaValida(h)) {
                        return 0.0;
                    }
                    return Math.max(w / nativoAncho, h / nativoAlto);
                },
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

    /** Suelta el slideshow y las animaciones al abandonar el menú. */
    public void liberar() {
        if (cicloFondo != null) {
            cicloFondo.stop();
            cicloFondo = null;
        }
        if (kenBurnsActual != null) {
            kenBurnsActual.stop();
            kenBurnsActual = null;
        }
        if (sondeoDatos != null) {
            sondeoDatos.stop();
            sondeoDatos = null;
        }
        detenerAnimacionesDeHover();
    }

    // --- interacción ------------------------------------------------------

    private StackPane[] todasLasEnvolturas() {
        return new StackPane[]{envolturaClasificacion, envolturaGestion, envolturaExplorar,
                                envolturaHistorial, envolturaAjustes};
    }

    private void aplicarHover(StackPane envoltura) {
        envoltura.setOnMouseEntered(e -> {
            resaltar(envoltura);
            reproducirSfxHover();
        });
        envoltura.setOnMouseExited(e -> resaltar(null));
    }

    /**
     * Única fuente de verdad del estado visual de las tarjetas.
     *
     * Antes el hover se repartía entre dos métodos —uno al entrar y otro al
     * salir—, y al pasar el ratón de una tarjeta a la de al lado JavaFX
     * dispara primero la salida y luego la entrada: las tres tarjetas que no
     * intervienen recibían a la vez un fundido hacia 1.0 (por la salida) y
     * otro hacia 0.72 (por la entrada), peleándose por la misma propiedad y
     * dejando opacidades a medias. Recalculando el estado completo desde la
     * tarjeta activa, cada nodo recibe un único destino coherente.
     *
     * @param activa tarjeta bajo el cursor, o {@code null} si no hay ninguna
     */
    private void resaltar(StackPane activa) {
        for (StackPane envoltura : todasLasEnvolturas()) {
            boolean esActiva = envoltura == activa;
            animarOpacidad(envoltura, activa == null || esActiva ? 1.0 : ATENUACION_HERMANA);
            animarEscala(envoltura, esActiva ? ESCALA_HOVER : 1.0);
        }
    }

    /** Reutiliza una transición por nodo y la detiene antes de redirigirla. */
    private void animarOpacidad(StackPane nodo, double destino) {
        FadeTransition fundido = fundidos.computeIfAbsent(nodo,
                n -> new FadeTransition(Animaciones.HOVER, n));
        if (yaEsta(nodo.getOpacity(), destino, fundido)) {
            return;
        }
        fundido.stop();
        fundido.setFromValue(nodo.getOpacity());
        fundido.setToValue(destino);
        fundido.play();
    }

    private void animarEscala(StackPane nodo, double destino) {
        ScaleTransition zoom = escalados.computeIfAbsent(nodo,
                n -> new ScaleTransition(Animaciones.HOVER, n));
        if (yaEsta(nodo.getScaleX(), destino, zoom)) {
            return;
        }
        zoom.stop();
        zoom.setFromX(nodo.getScaleX());
        zoom.setFromY(nodo.getScaleY());
        zoom.setToX(destino);
        zoom.setToY(destino);
        zoom.setInterpolator(Animaciones.EASE_OUT);
        zoom.play();
    }

    private static boolean yaEsta(double actual, double destino, Animation animacion) {
        return Math.abs(actual - destino) < 0.001
                && animacion.getStatus() != Animation.Status.RUNNING;
    }

    /** Deja las tarjetas quietas antes de una animación que las controla por completo. */
    private void detenerAnimacionesDeHover() {
        fundidos.values().forEach(Animation::stop);
        escalados.values().forEach(Animation::stop);
    }

    /**
     * Acompañamiento del hover, atenuado y con cortafuego: los clips duran
     * ~1,5 s, así que barrer el ratón por las cinco tarjetas apilaría varias
     * copias y taparía el sonido de confirmación al elegir.
     */
    private void reproducirSfxHover() {
        long ahora = System.currentTimeMillis();
        if (ahora - ultimoSfxHoverMs < ESPERA_SFX_HOVER_MS) {
            return;
        }
        ultimoSfxHoverMs = ahora;
        AudioManager.reproducirSfx(SFX_HOVER, VOLUMEN_HOVER);
    }

    @FXML
    private void onClasificacion() {
        irAlShell(ShellController::irACarrera, envolturaClasificacion);
    }

    @FXML
    private void onGestionEquipos() {
        irAlShell(ShellController::irAGestion, envolturaGestion);
    }

    @FXML
    private void onExplorar() {
        irAlShell(ShellController::irAExplorar, envolturaExplorar);
    }

    @FXML
    private void onHistorial() {
        irAlShell(ShellController::irAHistorial, envolturaHistorial);
    }

    @FXML
    private void onAjustes() {
        AudioManager.reproducirSfx(SFX_SECUNDARIO);
        animarEscala(envolturaAjustes, ESCALA_PULSO);
        AjustesDialog.mostrar();
        // El diálogo es modal y bloquea: mientras está abierto el ratón sale
        // de la tarjeta sin que llegue el evento, así que al volver hay que
        // recomponer el estado a mano o AJUSTES se queda agrandada y el resto
        // del menú atenuado para siempre.
        resaltar(null);
    }

    @FXML
    private void onSalir() {
        AudioManager.reproducirSfx(SFX_SECUNDARIO);
        if (Navigator.confirmar("¿Quieres salir del simulador?")) {
            Platform.exit();
            return;
        }
        // Mismo motivo que en AJUSTES: tras un modal el hover queda obsoleto.
        resaltar(null);
    }

    private void irAlShell(Runnable destinoEnShell, StackPane seleccionada) {
        AudioManager.reproducirSfx(SFX_PRINCIPAL);
        animarSalidaHaciaSeccion(seleccionada, () -> alEntrarAlShell.accept(destinoEnShell));
    }

    /**
     * La tarjeta elegida pulsa y el resto del menú se apaga antes de navegar,
     * para que la salida se sienta como parte del propio menú y no como un
     * corte a negro seguido de otra pantalla.
     */
    private void animarSalidaHaciaSeccion(StackPane seleccionada, Runnable alTerminar) {
        // A partir de aquí manda la salida: si quedara viva una transición de
        // hover, las dos escribirían sobre la misma opacidad o escala.
        detenerAnimacionesDeHover();

        // Solo el pulso de la tarjeta elegida. Antes el resto del menú se
        // apagaba a la vez, y encadenado con el cruce hacia el shell daba la
        // sensación de fundido a negro que se quería quitar; la confirmación
        // se lee igual de bien con la escala sola, y así el menú se mantiene
        // nítido hasta que la sección lo tapa.
        ScaleTransition pulso = new ScaleTransition(Animaciones.PULSO_TILE, seleccionada);
        pulso.setToX(ESCALA_PULSO);
        pulso.setToY(ESCALA_PULSO);
        pulso.setInterpolator(Animaciones.EASE_OUT);
        pulso.setOnFinished(e -> alTerminar.run());
        pulso.play();
    }
}
