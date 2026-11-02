package com.poli.restaurante.exception;

/** La operación choca con el estado actual de los datos: duplicados, estado inválido, concurrencia (HTTP 409). */
public class ConflictException extends RuntimeException {

    public ConflictException(String mensaje) {
        super(mensaje);
    }
}
