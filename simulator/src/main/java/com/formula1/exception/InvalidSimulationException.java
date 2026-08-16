package com.formula1.exception;

public class InvalidSimulationException extends RuntimeException {

    public InvalidSimulationException(String message) {
        super(message);
    }

    public InvalidSimulationException(String message, Throwable cause) {
        super(message, cause);
    }
}
