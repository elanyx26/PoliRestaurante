package com.poli.restaurante.exception;

/** Demasiados intentos de inicio de sesión fallidos (HTTP 429). */
public class TooManyAttemptsException extends RuntimeException {

    public TooManyAttemptsException(String mensaje) {
        super(mensaje);
    }
}
