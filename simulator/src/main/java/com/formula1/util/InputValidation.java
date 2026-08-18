package com.formula1.util;

import com.formula1.service.ValidationException;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputControl;

import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

/** Filtros y conversiones estrictas reutilizables para los inputs JavaFX. */
public final class InputValidation {

    private static final Pattern SOLO_TEXTO = Pattern.compile("[\\p{L} .''’\\-]*");
    private static final Pattern IDENTIFICADOR = Pattern.compile("[\\p{L}\\p{N} .,'’&()/_+\\-]*");
    private static final Pattern BUSQUEDA = Pattern.compile("[\\p{L}\\p{N} .,'’&()/_+\\-]*");
    private static final Pattern DESCRIPCION = Pattern.compile("[^\\p{Cntrl}]*");
    private static final Pattern VUELTA_PARCIAL = Pattern.compile("\\d{0,2}(:\\d{0,2}([.,]\\d{0,3})?)?");
    private static final Pattern VUELTA_COMPLETA = Pattern.compile("\\d{1,2}:[0-5]\\d[.,]\\d{3}");

    private InputValidation() { }

    public static void texto(TextInputControl control, int maximo) {
        filtrar(control, SOLO_TEXTO, maximo);
    }

    public static void identificador(TextInputControl control, int maximo) {
        filtrar(control, IDENTIFICADOR, maximo);
    }

    public static void descripcion(TextInputControl control, int maximo) {
        filtrar(control, DESCRIPCION, maximo);
    }

    public static void busqueda(TextField control) {
        filtrar(control, BUSQUEDA, 80);
    }

    public static void decimal(TextField control, int enteros, int decimales) {
        Pattern patron = Pattern.compile("\\d{0," + enteros + "}([.,]\\d{0," + decimales + "})?");
        filtrar(control, patron, enteros + decimales + 1);
    }

    public static void tiempoVuelta(TextField control) {
        filtrar(control, VUELTA_PARCIAL, 9);
    }

    public static void entero(Spinner<Integer> control, int min, int max) {
        control.setEditable(true);
        Pattern patron = Pattern.compile("\\d{0," + String.valueOf(max).length() + "}");
        control.getEditor().setTextFormatter(new TextFormatter<String>(c ->
                patron.matcher(c.getControlNewText()).matches() ? c : null));
    }

    public static String requerido(TextInputControl control, String campo, int maximo) {
        String valor = control.getText() == null ? "" : control.getText().trim();
        if (valor.isEmpty()) {
            throw invalido(control, campo + " es obligatorio.");
        }
        if (valor.length() > maximo) {
            throw invalido(control, campo + " no puede superar " + maximo + " caracteres.");
        }
        control.getStyleClass().remove("input-invalid");
        return valor;
    }

    public static double valorDecimal(TextField control, String campo, double min, double max) {
        String texto = requerido(control, campo, 20).replace(',', '.');
        final double valor;
        try {
            valor = Double.parseDouble(texto);
        } catch (NumberFormatException error) {
            throw invalido(control, campo + " debe ser un número válido.");
        }
        if (!Double.isFinite(valor) || valor < min || valor > max) {
            throw invalido(control, campo + " debe estar entre " + min + " y " + max + ".");
        }
        return valor;
    }

    public static int valorEntero(Spinner<Integer> control, String campo, int min, int max) {
        String texto = control.getEditor().getText();
        final int valor;
        try {
            valor = Integer.parseInt(texto);
        } catch (RuntimeException error) {
            marcarInvalido(control);
            throw new ValidationException(campo + " debe ser un número entero.");
        }
        if (valor < min || valor > max) {
            marcarInvalido(control);
            throw new ValidationException(campo + " debe estar entre " + min + " y " + max + ".");
        }
        SpinnerValueFactory<Integer> fabrica = control.getValueFactory();
        if (fabrica != null) fabrica.setValue(valor);
        control.getStyleClass().remove("input-invalid");
        return valor;
    }

    public static double valorTiempoVuelta(TextField control, String campo) {
        String texto = requerido(control, campo, 9);
        if (!VUELTA_COMPLETA.matcher(texto).matches()) {
            throw invalido(control, campo + " debe usar el formato m:ss.mmm; por ejemplo, 1:10.166.");
        }
        String[] partes = texto.replace(',', '.').split(":");
        return Integer.parseInt(partes[0]) * 60.0 + Double.parseDouble(partes[1]);
    }

    public static void seleccionar(boolean condicion, javafx.scene.control.Control control, String mensaje) {
        if (!condicion) {
            marcarInvalido(control);
            control.requestFocus();
            throw new ValidationException(mensaje);
        }
        control.getStyleClass().remove("input-invalid");
    }

    private static void filtrar(TextInputControl control, Pattern patron, int maximo) {
        UnaryOperator<TextFormatter.Change> filtro = cambio -> {
            String nuevo = cambio.getControlNewText();
            return nuevo.length() <= maximo && patron.matcher(nuevo).matches() ? cambio : null;
        };
        control.setTextFormatter(new TextFormatter<String>(filtro));
    }

    private static ValidationException invalido(TextInputControl control, String mensaje) {
        marcarInvalido(control);
        control.requestFocus();
        return new ValidationException(mensaje);
    }

    private static void marcarInvalido(javafx.scene.control.Control control) {
        if (!control.getStyleClass().contains("input-invalid")) {
            control.getStyleClass().add("input-invalid");
        }
    }
}
