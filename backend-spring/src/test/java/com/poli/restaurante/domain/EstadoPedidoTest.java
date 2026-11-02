package com.poli.restaurante.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class EstadoPedidoTest {

    @Test
    void soloExistenLasTransicionesDelFlujoOperativo() {
        assertTrue(EstadoPedido.PENDIENTE.puedePasarA(EstadoPedido.EN_PREPARACION));
        assertTrue(EstadoPedido.PENDIENTE.puedePasarA(EstadoPedido.CANCELADO));
        assertTrue(EstadoPedido.EN_PREPARACION.puedePasarA(EstadoPedido.LISTO));
        assertTrue(EstadoPedido.EN_PREPARACION.puedePasarA(EstadoPedido.CANCELADO));
        assertTrue(EstadoPedido.LISTO.puedePasarA(EstadoPedido.ENTREGADO));

        assertFalse(EstadoPedido.PENDIENTE.puedePasarA(EstadoPedido.LISTO));
        assertFalse(EstadoPedido.PENDIENTE.puedePasarA(EstadoPedido.ENTREGADO));
        assertFalse(EstadoPedido.LISTO.puedePasarA(EstadoPedido.CANCELADO));
        assertFalse(EstadoPedido.LISTO.puedePasarA(EstadoPedido.EN_PREPARACION));
        assertFalse(EstadoPedido.EN_PREPARACION.puedePasarA(EstadoPedido.PENDIENTE));
    }

    @Test
    void losEstadosFinalesNoTienenSalida() {
        for (EstadoPedido destino : EstadoPedido.values()) {
            assertFalse(EstadoPedido.ENTREGADO.puedePasarA(destino));
            assertFalse(EstadoPedido.CANCELADO.puedePasarA(destino));
        }
    }

    @Test
    void losEstadosActivosSonLosQueOcupanLaMesa() {
        Set<EstadoPedido> activos = Set.of(EstadoPedido.PENDIENTE, EstadoPedido.EN_PREPARACION, EstadoPedido.LISTO);
        for (EstadoPedido e : EstadoPedido.values()) {
            assertTrue(activos.contains(e) == e.esActivo());
        }
    }
}
