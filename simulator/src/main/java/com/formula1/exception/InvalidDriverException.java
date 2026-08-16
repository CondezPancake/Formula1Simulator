package com.formula1.exception;

public class InvalidDriverException extends RuntimeException {

    public InvalidDriverException(String message) {
        super(message);
    }

    public InvalidDriverException(String message, Throwable cause) {
        super(message, cause);
    }
}
