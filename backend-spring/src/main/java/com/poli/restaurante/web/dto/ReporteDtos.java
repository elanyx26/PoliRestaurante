package com.poli.restaurante.web.dto;

import com.poli.restaurante.domain.EstadoPedido;
import com.poli.restaurante.domain.TipoPedido;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class ReporteDtos {

    private ReporteDtos() {
    }

    public record VentasPorTipo(TipoPedido tipo, long pedidos, BigDecimal total) {
    }

    public record VentasPorDia(LocalDate fecha, long pedidos, BigDecimal total) {
    }

    /** Ventas = pedidos ENTREGADOS cuya fecha de entrega cae en el rango. */
    public record VentasResponse(LocalDate desde, LocalDate hasta, long pedidos, BigDecimal totalVentas,
                                 BigDecimal totalEnvios, BigDecimal ticketPromedio, List<VentasPorTipo> porTipo,
                                 List<VentasPorDia> porDia) {
    }

    public record ProductoVendido(Long productoId, String nombre, long unidades, BigDecimal ingresos) {
    }

    public record ProductosMasVendidosResponse(LocalDate desde, LocalDate hasta, List<ProductoVendido> productos) {
    }

    public record CancelacionesPorEstado(EstadoPedido estadoPrevio, long cancelaciones) {
    }

    public record CancelacionesPorMesero(Long meseroId, String mesero, long cancelaciones, BigDecimal valor) {
    }

    public record CancelacionesResponse(LocalDate desde, LocalDate hasta, long total, BigDecimal valorCancelado,
                                        List<CancelacionesPorEstado> porEstadoPrevio,
                                        List<CancelacionesPorMesero> porMesero) {
    }

    public record TiempoPorTipo(TipoPedido tipo, long pedidos, double promedioSegundos) {
    }

    /** Tiempo entre EN_PREPARACION y LISTO, de los pedidos que quedaron listos dentro del rango. */
    public record TiempoPreparacionResponse(LocalDate desde, LocalDate hasta, long pedidos, Double promedioSegundos,
                                            Double minimoSegundos, Double maximoSegundos, Double p90Segundos,
                                            List<TiempoPorTipo> porTipo) {
    }
}
