package com.poli.restaurante.exception;

/** La petición es válida pero viola una regla de negocio (HTTP 422). */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String mensaje) {
        super(mensaje);
    }
}
