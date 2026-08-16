package com.formula1.exception;

public class OpenF1ConnectionException extends RuntimeException {

    public OpenF1ConnectionException(String message) {
        super(message);
    }

    public OpenF1ConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
