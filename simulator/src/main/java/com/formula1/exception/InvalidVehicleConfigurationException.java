package com.formula1.exception;

public class InvalidVehicleConfigurationException extends RuntimeException {

    public InvalidVehicleConfigurationException(String message) {
        super(message);
    }

    public InvalidVehicleConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
