package com.poli.restaurante.domain;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Estados de un pedido y su máquina de transiciones:
 * PENDIENTE → EN_PREPARACION → LISTO → ENTREGADO, y PENDIENTE/EN_PREPARACION → CANCELADO.
 */
public enum EstadoPedido {
    PENDIENTE, EN_PREPARACION, LISTO, ENTREGADO, CANCELADO;

    private static final Map<EstadoPedido, Set<EstadoPedido>> TRANSICIONES = new EnumMap<>(EstadoPedido.class);

    static {
        TRANSICIONES.put(PENDIENTE, EnumSet.of(EN_PREPARACION, CANCELADO));
        TRANSICIONES.put(EN_PREPARACION, EnumSet.of(LISTO, CANCELADO));
        TRANSICIONES.put(LISTO, EnumSet.of(ENTREGADO));
        TRANSICIONES.put(ENTREGADO, EnumSet.noneOf(EstadoPedido.class));
        TRANSICIONES.put(CANCELADO, EnumSet.noneOf(EstadoPedido.class));
    }

    public boolean puedePasarA(EstadoPedido destino) {
        return TRANSICIONES.get(this).contains(destino);
    }

    /** Estados en los que el pedido ocupa recursos (p. ej. la mesa). */
    public boolean esActivo() {
        return this == PENDIENTE || this == EN_PREPARACION || this == LISTO;
    }
}
