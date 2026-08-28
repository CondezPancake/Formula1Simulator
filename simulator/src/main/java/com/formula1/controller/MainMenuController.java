package com.formula1.controller;

import com.formula1.data.DataStore;
import com.formula1.util.AudioManager;
import com.formula1.util.ImageCrop;
import com.formula1.util.Imagenes;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.CacheHint;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.effect.PerspectiveTransform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Text;
import javafx.scene.transform.Scale;

import java.io.InputStream;
import java.util.function.Consumer;

/**
 * Menú principal, reproduciendo el del videojuego F1 23.
 *
 * Referencia: {@code docs/assets/f1-23-menu-referencia.jpg}. Pantalla partida:
 * a la izquierda la lista de opciones —la activa dentro de un marco y con
 * galones a su derecha— con su descripción debajo; a la derecha el escenario
 * con el título de la sección.
 *
 * Es un menú <b>estático</b>: cambiar de opción repinta al instante, sin
 * transiciones. Lo único que se mueve en toda la pantalla es el cursor.
 */
public class MainMenuController {

    /** Alto de referencia del que salen todas las proporciones tipográficas. */
    private static final double REF_ALTO = 940;

    /**
     * Cota superior de cordura para cualquier medida de layout. En algunos
     * compositores (visto en Hyprland/Wayland) {@code Stage.setMaximized}
     * hace que, durante un único pulso, la ventana informe un alto
     * disparatado (miles de millones de px) antes de que llegue el real. Sin
     * este tope ese valor se propaga a los cuerpos de letra y revienta el
     * cálculo interno de ajuste de texto de JavaFX de forma permanente.
     */
    private static final double LADO_MAXIMO_RAZONABLE = 8000;

    /** Confirmación: entrar a una sección. */
    private static final String SFX_CONFIRMAR = "/audio/sound1.mp3";
    /** Acompañamiento al recorrer la lista, por debajo del de confirmación. */
    private static final String SFX_MOVER = "/audio/sound2.mp3";
    private static final double VOLUMEN_MOVER = 0.35;
    /** Los clips duran ~1,5 s: sin esto, recorrer la lista los apilaría. */
    private static final long ESPERA_SFX_MOVER_MS = 350;

    /**
     * El wordmark ocupa solo la banda central de LogoF1.png (920x800): sin
     * recortar el margen transparente el logo saldría diminuto.
     */
    private static final Rectangle2D RECORTE_LOGO = new Rectangle2D(85, 295, 750, 190);

    /** Galón de la fila activa, en caja 0 0 12 24. */
    private static final String RUTA_GALON = "M2 3 L10 12 L2 21";

    /** Se decodifica el arte por encima del tamaño de pintado, por nitidez. */
    private static final double FACTOR_NITIDEZ_ARTE = 1.25;
    /** Recorte sesgado hacia arriba: menu-explorar.png es vertical. */
    private static final double SESGO_ARTE = 0.18;
    /** Cuánto se recoge el lado izquierdo del arte, como fracción del alto. */
    private static final double RETRANQUEO_ARTE = 0.035;

    /**
     * Una entrada del menú: lo que se lee, lo que explica y a dónde lleva.
     *
     * El orden del array es el orden en pantalla, y se conserva del menú
     * anterior a propósito.
     */
    private record Opcion(String titulo, String descripcion, String arte, Runnable destino) { }

    private final Opcion[] opciones = {
        new Opcion("CLASIFICACIÓN",
                "Monta una sesión de clasificación: elige circuito, monoplaza y piloto, "
                        + "y pelea por la pole contra el resto de la parrilla.",
                "/images/menu-clasificacion.png",
                () -> entrarAlShell(ShellController::irACarrera)),
        new Opcion("GESTIÓN DE EQUIPOS",
                "Administra la parrilla: da de alta y edita escuderías, pilotos, "
                        + "vehículos y circuitos, y compara monoplazas entre sí.",
                "/images/menu-gestionequipo.png",
                () -> entrarAlShell(ShellController::irAGestion)),
        new Opcion("EXPLORAR",
                "Consulta las fichas de los pilotos, el garaje de monoplazas y "
                        + "los trazados disponibles para la sesión.",
                "/images/menu-explorar.png",
                () -> entrarAlShell(ShellController::irAExplorar)),
        new Opcion("HISTORIAL",
                "Revisa las sesiones ya disputadas, con sus victorias, podios "
                        + "y tiempos por sector.",
                "/images/menu-historial.jpg",
                () -> entrarAlShell(ShellController::irAHistorial)),
        new Opcion("AJUSTES",
                "Ajusta el volumen de la música y de los efectos, o silencia "
                        + "el simulador por completo.",
                "/images/menu-ajustes.jpg",
                AjustesDialog::mostrar),
    };

    @FXML private GridPane raiz;
    @FXML private VBox panelIzquierdo;
    @FXML private HBox cajaMarca;
    @FXML private ImageView logoF1;
    @FXML private HBox cajaTemporada;
    @FXML private VBox listaOpciones;
    @FXML private Label lblDescripcion;
    @FXML private VBox cajaPie;
    @FXML private Button btnSalir;
    @FXML private Label lblPistas;

    @FXML private StackPane escenario;
    @FXML private Group capaArte;
    @FXML private Region veloArte;
    @FXML private HBox cajaHud;
    @FXML private VBox cajaTitulo;
    @FXML private Region fileteTitulo;
    @FXML private Label lblTituloSeccion;
    @FXML private Label lblPieSeccion;

    /** App inyecta aquí cómo pasar de este menú al shell, para no depender de él. */
    private Consumer<Runnable> alEntrarAlShell = irA -> { };

    /** Una fila por opción, en el mismo orden que {@link #opciones}. */
    private final HBox[] filas = new HBox[5];
    private final StackPane[] marcos = new StackPane[5];
    private final Label[] etiquetas = new Label[5];
    private final HBox[] galones = new HBox[5];
    private final Scale[] escalasGalon = new Scale[5];

    private int seleccionada = 0;
    private long ultimoSfxMoverMs = 0;

    /** Se guarda la referencia para poder retirar el filtro de la escena. */
    private final EventHandler<KeyEvent> filtroTeclado = this::alPulsarTecla;

    @FXML
    public void initialize() {
        cargarLogo();
        construirFilas();
        rellenarHud();
        montarTeclado();
        montarEscalado();
        seleccionar(0);
    }

    public void setAlEntrarAlShell(Consumer<Runnable> callback) {
        this.alEntrarAlShell = callback;
    }

    // --- construcción -----------------------------------------------------

    private void construirFilas() {
        for (int i = 0; i < opciones.length; i++) {
            Label texto = new Label(opciones[i].titulo());
            texto.getStyleClass().add("menu-opcion-texto");

            // El marco ciñe al texto, no ocupa la columna entera: en la
            // referencia el recuadro termina justo después de la palabra.
            StackPane marco = new StackPane(texto);
            marco.getStyleClass().add("menu-opcion-marco");
            marco.setAlignment(Pos.CENTER_LEFT);
            marco.setMaxWidth(Region.USE_PREF_SIZE);

            HBox galon = construirGalones(i);

            // La fila sí ocupa todo el ancho, para que el ratón la coja
            // entera y no solo encima de la palabra.
            HBox fila = new HBox(marco, galon);
            fila.getStyleClass().add("menu-opcion");
            fila.setAlignment(Pos.CENTER_LEFT);

            final int indice = i;
            // El ratón solo mueve el resaltado; entrar exige clic, como en un
            // menú de consola donde el foco y la confirmación son distintos.
            fila.setOnMouseEntered(e -> seleccionar(indice));
            fila.setOnMouseClicked(e -> activar(indice));

            filas[i] = fila;
            marcos[i] = marco;
            etiquetas[i] = texto;
            galones[i] = galon;
            listaOpciones.getChildren().add(fila);
        }
    }

    /** Tres galones de opacidad decreciente, el rasgo del menú de F1 23. */
    private HBox construirGalones(int indice) {
        HBox caja = new HBox();
        caja.setAlignment(Pos.CENTER_LEFT);
        Scale escala = new Scale(1, 1);
        escalasGalon[indice] = escala;

        double[] opacidades = {0.85, 0.5, 0.25};
        for (double opacidad : opacidades) {
            SVGPath galon = new SVGPath();
            galon.setContent(RUTA_GALON);
            galon.getStyleClass().add("menu-galon");
            galon.setOpacity(opacidad);
            Group envoltura = new Group(galon);
            envoltura.getTransforms().add(escala);
            caja.getChildren().add(envoltura);
        }
        return caja;
    }

    /** Contadores reales de la parrilla, como la tira de estado de la referencia. */
    private void rellenarHud() {
        DataStore datos = DataStore.getInstance();
        // Se pinta una sola vez: la carga arrancó en paralelo a la intro, que
        // dura ~6 s, así que a esta altura ya terminó. Si no, quedan guiones
        // en vez de montar un temporizador para un dato decorativo.
        boolean listo = datos.estaCargado();
        String[] valores = {
            (listo ? String.valueOf(datos.pilotos().size()) : "—") + " PILOTOS",
            (listo ? String.valueOf(datos.equipos().size()) : "—") + " EQUIPOS",
            (listo ? String.valueOf(datos.circuitos().size()) : "—") + " CIRCUITOS",
            "TEMPORADA 2025",
        };
        cajaHud.getChildren().clear();
        for (int i = 0; i < valores.length; i++) {
            if (i > 0) {
                Text separador = new Text("  ·  ");
                separador.getStyleClass().add("menu-hud-separador");
                cajaHud.getChildren().add(separador);
            }
            Text glifo = new Text(valores[i]);
            glifo.getStyleClass().add("menu-hud-glifo");
            cajaHud.getChildren().add(glifo);
        }
    }

    /**
     * El teclado se engancha a la escena, no a la raíz.
     *
     * Un manejador en la raíz solo dispara si el foco está justamente ahí, y
     * basta que lo tenga el botón SALIR para que las flechas dejen de
     * responder. Un filtro en la escena recoge la tecla venga de donde venga,
     * que es como se comporta un menú de consola.
     *
     * Como la aplicación reutiliza una única {@code Scene} para todas las
     * pantallas, el filtro <b>hay que quitarlo</b> al salir del menú: si no,
     * seguiría interceptando las flechas dentro del shell.
     */
    private void montarTeclado() {
        raiz.setFocusTraversable(true);
        raiz.sceneProperty().addListener((obs, antigua, nueva) -> {
            if (antigua != null) {
                antigua.removeEventFilter(KeyEvent.KEY_PRESSED, filtroTeclado);
            }
            if (nueva != null) {
                nueva.addEventFilter(KeyEvent.KEY_PRESSED, filtroTeclado);
                // El foco no se puede pedir hasta estar en una escena.
                Platform.runLater(raiz::requestFocus);
            }
        });
    }

    private void alPulsarTecla(KeyEvent e) {
        switch (e.getCode()) {
            case UP, W -> { mover(-1); e.consume(); }
            case DOWN, S -> { mover(1); e.consume(); }
            case ENTER, SPACE -> { activar(seleccionada); e.consume(); }
            case ESCAPE -> { onSalir(); e.consume(); }
            default -> { }
        }
    }

    // --- selección --------------------------------------------------------

    /** Envolvente: bajar desde la última lleva a la primera y al revés. */
    private void mover(int delta) {
        int destino = Math.floorMod(seleccionada + delta, opciones.length);
        if (destino != seleccionada) {
            reproducirSfxMover();
        }
        seleccionar(destino);
    }

    /**
     * Única fuente de verdad del estado del menú: repinta las cinco filas, la
     * descripción y el escenario a partir del índice activo.
     */
    private void seleccionar(int indice) {
        seleccionada = indice;
        for (int i = 0; i < filas.length; i++) {
            boolean activa = i == indice;
            marcos[i].getStyleClass().remove("menu-opcion-activa");
            if (activa) {
                marcos[i].getStyleClass().add("menu-opcion-activa");
            }
            // Los galones solo acompañan a la fila activa.
            galones[i].setVisible(activa);
            galones[i].setManaged(activa);
        }
        Opcion opcion = opciones[indice];
        lblDescripcion.setText(opcion.descripcion());
        lblTituloSeccion.setText(opcion.titulo());
        lblPieSeccion.setText("SIMULADOR DE FÓRMULA 1");
        pintarArte(opcion);
        aplicarEscalado();
    }

    // --- arte del panel derecho -------------------------------------------

    /**
     * Coloca la imagen de la opción activa en el panel derecho.
     *
     * El encaje es «cover»: se recorta a la proporción del panel en vez de
     * deformarse. El sesgo del recorte va hacia arriba porque
     * {@code menu-explorar.png} es vertical y centrarlo dejaría fuera lo que
     * interesa.
     */
    private void pintarArte(Opcion opcion) {
        double w = escenario.getWidth();
        double h = escenario.getHeight();
        if (!medidaValida(w) || !medidaValida(h)) {
            return;   // aún sin tamaño; el listener del panel repetirá
        }

        Image imagen = Imagenes.cargar(opcion.arte(), w * FACTOR_NITIDEZ_ARTE, 0);
        if (imagen == null) {
            capaArte.getChildren().clear();
            return;
        }

        ImageView vista = ImageCrop.encajar(imagen, w, h, SESGO_ARTE);
        // El Group no lo coloca el StackPane (va sin gestionar), así que la
        // vista se ancla a mano en el origen del panel.
        vista.setLayoutX(0);
        vista.setLayoutY(0);
        vista.setEffect(relieve(w, h));
        // El arte no cambia salvo al elegir otra opción: se rasteriza una vez
        // en vez de recalcular la perspectiva en cada fotograma.
        vista.setCache(true);
        vista.setCacheHint(CacheHint.SPEED);
        capaArte.getChildren().setAll(vista);
    }

    /**
     * Da volumen a la imagen para que no parezca una lámina pegada.
     *
     * Es un {@link PerspectiveTransform} con las dos esquinas del lado
     * izquierdo recogidas hacia dentro: ese lado queda «más lejos» y el plano
     * se lee inclinado, hundiéndose hacia la mitad negra del menú. Sus
     * coordenadas son absolutas en píxeles, así que hay que rehacerlo cada vez
     * que cambia el tamaño del panel.
     *
     * No lleva animación a propósito: el menú es estático.
     */
    private static PerspectiveTransform relieve(double w, double h) {
        double retranqueo = h * RETRANQUEO_ARTE;
        PerspectiveTransform perspectiva = new PerspectiveTransform();
        perspectiva.setUlx(0);      perspectiva.setUly(retranqueo);
        perspectiva.setUrx(w);      perspectiva.setUry(0);
        perspectiva.setLrx(w);      perspectiva.setLry(h);
        perspectiva.setLlx(0);      perspectiva.setLly(h - retranqueo);
        return perspectiva;
    }

    private void activar(int indice) {
        seleccionar(indice);
        AudioManager.reproducirSfx(SFX_CONFIRMAR);
        opciones[indice].destino().run();
    }

    private void entrarAlShell(Runnable destinoEnShell) {
        alEntrarAlShell.accept(destinoEnShell);
    }

    private void reproducirSfxMover() {
        long ahora = System.currentTimeMillis();
        if (ahora - ultimoSfxMoverMs < ESPERA_SFX_MOVER_MS) {
            return;
        }
        ultimoSfxMoverMs = ahora;
        AudioManager.reproducirSfx(SFX_MOVER, VOLUMEN_MOVER);
    }

    @FXML
    private void onSalir() {
        AudioManager.reproducirSfx(SFX_MOVER, VOLUMEN_MOVER);
        if (Navigator.confirmar("¿Quieres salir del simulador?")) {
            Platform.exit();
        }
    }

    // --- escalado ---------------------------------------------------------

    /** El menú escala con la ventana, así que la tipografía se recalcula. */
    private void montarEscalado() {
        InvalidationListener escalar = observable -> aplicarEscalado();
        raiz.widthProperty().addListener(escalar);
        raiz.heightProperty().addListener(escalar);

        // El arte necesita el tamaño del panel derecho, no el de la raíz, y
        // el GridPane reparte el ancho a sus columnas en una pasada posterior:
        // cuando la raíz ya mide, `escenario` todavía está a cero. Sin este
        // listener el panel se quedaba vacío hasta que algo volvía a disparar
        // el escalado —pasar el ratón por la lista—, así que recién arrancado
        // no se veía ninguna imagen.
        InvalidationListener repintarArte = observable -> pintarArte(opciones[seleccionada]);
        escenario.widthProperty().addListener(repintarArte);
        escenario.heightProperty().addListener(repintarArte);
    }

    private void aplicarEscalado() {
        double h = raiz.getHeight();
        if (!medidaValida(h)) {
            return;
        }

        double margen = px(64, h);
        panelIzquierdo.setPadding(new Insets(px(58, h), margen, px(44, h), margen));
        panelIzquierdo.setSpacing(px(30, h));
        // El margen va en el contenido, no en el panel: si lo lleva el panel,
        // el arte y su velo se quedan dentro del hueco y aparece un marco del
        // fondo alrededor en vez de llegar a los bordes.
        StackPane.setMargin(cajaHud, new Insets(px(26, h), px(34, h), 0, 0));
        StackPane.setMargin(cajaTitulo, new Insets(0, 0, px(46, h), px(46, h)));

        logoF1.setFitHeight(px(38, h));
        rellenarTracking(cajaTemporada, "TEMPORADA 2025", px(11, h));

        listaOpciones.setSpacing(px(6, h));
        for (int i = 0; i < etiquetas.length; i++) {
            estiloFuente(etiquetas[i], px(i == seleccionada ? 31 : 25, h));
            filas[i].setSpacing(px(22, h));
            galones[i].setSpacing(px(3, h));
            double factor = px(26, h) / 24.0;   // las rutas viven en caja 0 0 12 24
            escalasGalon[i].setX(factor);
            escalasGalon[i].setY(factor);
        }

        estiloFuente(lblDescripcion, px(14, h));
        lblDescripcion.setMaxWidth(Region.USE_PREF_SIZE);
        lblDescripcion.setPrefWidth(px(430, h));

        estiloFuente(btnSalir, px(14, h));
        estiloFuente(lblPistas, px(11, h));
        cajaPie.setSpacing(px(10, h));

        for (var nodo : cajaHud.getChildren()) {
            if (nodo instanceof Text glifo) {
                glifo.setStyle("-fx-font-size: " + redondear(px(11, h)) + "px;");
            }
        }

        pintarArte(opciones[seleccionada]);

        fileteTitulo.setPrefSize(px(74, h), Math.max(2, px(5, h)));
        fileteTitulo.setMaxSize(px(74, h), Math.max(2, px(5, h)));
        VBox.setMargin(lblTituloSeccion, new Insets(px(16, h), 0, 0, 0));
        estiloFuente(lblTituloSeccion, px(66, h));
        estiloFuente(lblPieSeccion, px(13, h));
    }

    /** Convierte una medida de la referencia a píxeles reales de la ventana. */
    private static double px(double medida, double altoActual) {
        return medida * altoActual / REF_ALTO;
    }

    private static void estiloFuente(Labeled etiqueta, double tamano) {
        etiqueta.setStyle("-fx-font-size: " + redondear(tamano) + "px;");
    }

    /**
     * Compone el tracking carácter a carácter: JavaFX 17 no tiene
     * {@code -fx-letter-spacing}, solo {@code -fx-line-spacing}.
     */
    private static void rellenarTracking(HBox destino, String texto, double tamano) {
        destino.setSpacing(Math.max(1, tamano * 0.34));
        destino.getChildren().clear();
        for (char c : texto.toCharArray()) {
            Text glifo = new Text(c == ' ' ? " " : String.valueOf(c));
            glifo.getStyleClass().add("menu-marca");
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

    private void cargarLogo() {
        Image logo = Imagenes.cargar("/images/LogoF1.png", 840, 0);
        if (logo == null) {
            return;
        }
        logoF1.setImage(logo);
        logoF1.setViewport(RECORTE_LOGO);
    }
}
