package com.poli.restaurante.realtime;

import com.poli.restaurante.domain.EstadoPedido;
import com.poli.restaurante.domain.TipoPedido;
import java.time.Instant;

/** Evento de dominio sobre un pedido. Es liviano a propósito: el cliente consulta el detalle por REST. */
public record PedidoEvento(Tipo tipo, Long pedidoId, EstadoPedido estado, EstadoPedido estadoAnterior,
                           TipoPedido tipoPedido, Long meseroId, Integer mesaNumero, Instant fecha) {

    public enum Tipo {
        PEDIDO_CREADO, PEDIDO_ACTUALIZADO, PEDIDO_ESTADO_CAMBIADO
    }
}
