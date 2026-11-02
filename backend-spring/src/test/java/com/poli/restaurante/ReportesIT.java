package com.poli.restaurante;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

class ReportesIT extends IntegrationTestBase {

    private final LocalDate hoy = LocalDate.now(ZoneId.of("America/Bogota"));

    @Test
    void losReportesReflejanVentasCancelacionesYTiemposDeLosPedidos() throws Exception {
        Sesion mesero = crearUsuario("MESERO");
        Sesion cocina = crearUsuario("COCINA");
        long producto = crearProducto("10000.00");

        // Pedido entregado: 3 unidades.
        long entregado = crearPedidoSalon(mesero, producto, 3);
        String estado = "/estado";
        esperar(200, HttpMethod.PATCH, "/api/pedidos/" + entregado + estado, cocina.token(), estado("EN_PREPARACION"));
        Thread.sleep(1100);
        esperar(200, HttpMethod.PATCH, "/api/pedidos/" + entregado + estado, cocina.token(), estado("LISTO"));
        esperar(200, HttpMethod.PATCH, "/api/pedidos/" + entregado + estado, mesero.token(), estado("ENTREGADO"));

        // Pedido cancelado desde PENDIENTE.
        long cancelado = crearPedidoSalon(mesero, producto, 1);
        esperar(200, HttpMethod.PATCH, "/api/pedidos/" + cancelado + estado, mesero.token(), estado("CANCELADO"));

        String admin = tokenAdmin();
        String rango = "desde=" + hoy + "&hasta=" + hoy;

        String ventas = esperar(200, HttpMethod.GET, "/api/admin/reportes/ventas?" + rango, admin, null);
        assertTrue(((Number) campo(ventas, "$.pedidos")).longValue() >= 1);
        assertTrue(numero(ventas, "$.totalVentas").compareTo(new BigDecimal("30000")) >= 0);
        assertTrue(numero(ventas, "$.ticketPromedio").signum() > 0);
        List<String> tipos = campo(ventas, "$.porTipo[*].tipo");
        assertTrue(tipos.contains("SALON"));
        assertEquals(hoy.toString(), campo(ventas, "$.porDia[0].fecha"));

        String top = esperar(200, HttpMethod.GET, "/api/admin/reportes/productos-mas-vendidos?limite=100&" + rango, admin, null);
        List<Integer> unidades = campo(top, "$.productos[?(@.productoId==" + producto + ")].unidades");
        assertEquals(List.of(3), unidades);

        String cancelaciones = esperar(200, HttpMethod.GET, "/api/admin/reportes/cancelaciones?" + rango, admin, null);
        assertTrue(((Number) campo(cancelaciones, "$.total")).longValue() >= 1);
        List<String> previos = campo(cancelaciones, "$.porEstadoPrevio[*].estadoPrevio");
        assertTrue(previos.contains("PENDIENTE"));
        List<Integer> meseros = campo(cancelaciones, "$.porMesero[*].meseroId");
        assertTrue(meseros.stream().anyMatch(m -> m.longValue() == mesero.id()));

        String tiempos = esperar(200, HttpMethod.GET, "/api/admin/reportes/tiempo-preparacion?" + rango, admin, null);
        assertTrue(((Number) campo(tiempos, "$.pedidos")).longValue() >= 1);
        assertTrue(((Number) campo(tiempos, "$.maximoSegundos")).doubleValue() >= 1.0);
        assertFalse(((List<?>) campo(tiempos, "$.porTipo")).isEmpty());
    }

    @Test
    void unRangoSinPedidosDevuelveCerosYNoErrores() throws Exception {
        String admin = tokenAdmin();
        String ventas = esperar(200, HttpMethod.GET, "/api/admin/reportes/ventas?desde=2000-01-01&hasta=2000-01-31", admin, null);
        assertEquals(0, ((Number) campo(ventas, "$.pedidos")).intValue());
        assertEquals(0, numero(ventas, "$.ticketPromedio").signum());
        String tiempos = esperar(200, HttpMethod.GET, "/api/admin/reportes/tiempo-preparacion?desde=2000-01-01&hasta=2000-01-31", admin, null);
        assertEquals(0, ((Number) campo(tiempos, "$.pedidos")).intValue());
    }

    @Test
    void sinFechasUsaLosUltimos30DiasYSeValidaElRango() throws Exception {
        String admin = tokenAdmin();
        String ventas = esperar(200, HttpMethod.GET, "/api/admin/reportes/ventas", admin, null);
        assertEquals(hoy.toString(), campo(ventas, "$.hasta"));
        assertEquals(hoy.minusDays(29).toString(), campo(ventas, "$.desde"));

        esperar(422, HttpMethod.GET, "/api/admin/reportes/ventas?desde=2025-02-01&hasta=2025-01-01", admin, null);
        esperar(422, HttpMethod.GET, "/api/admin/reportes/ventas?desde=2020-01-01&hasta=2025-01-01", admin, null);
        esperar(400, HttpMethod.GET, "/api/admin/reportes/ventas?desde=ayer", admin, null);
    }
}
