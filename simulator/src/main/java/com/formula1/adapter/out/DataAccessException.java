package com.formula1.adapter.out;

/** Fallo al leer o escribir en el almacén persistente. */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String mensaje) {
        super(mensaje);
    }

    public DataAccessException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
