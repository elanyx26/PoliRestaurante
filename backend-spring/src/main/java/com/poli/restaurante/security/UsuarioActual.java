package com.poli.restaurante.security;

import com.poli.restaurante.domain.Rol;
import java.security.Principal;

/** Usuario autenticado de la petición actual. Su nombre de principal es el id (se usa para mensajes WebSocket dirigidos). */
public record UsuarioActual(Long id, String email, String nombre, Rol rol) implements Principal {

    @Override
    public String getName() {
        return String.valueOf(id);
    }
}
