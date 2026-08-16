package com.formula1.service;

/**
 * Datos de entrada que no cumplen las reglas del dominio.
 *
 * Una sola excepción para todas las entidades: el mensaje ya distingue el
 * caso concreto, y tener una clase por entidad solo añadía archivos.
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String mensaje) {
        super(mensaje);
    }
}
