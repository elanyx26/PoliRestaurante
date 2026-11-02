package com.poli.restaurante.web.dto;

import com.poli.restaurante.domain.DetallePedido;
import com.poli.restaurante.domain.EstadoPedido;
import com.poli.restaurante.domain.HistorialEstado;
import com.poli.restaurante.domain.MetodoPago;
import com.poli.restaurante.domain.Pedido;
import com.poli.restaurante.domain.TipoPedido;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class PedidoDtos {

    private PedidoDtos() {
    }

    // ------------------------------------------------------------ solicitudes

    public record EntregaRequest(
            @NotBlank @Size(max = 255) String direccion,
            @NotBlank @Size(max = 30) String telefono,
            @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal costoEnvio,
            @NotNull MetodoPago metodoPago) {
    }

    public record ItemPedidoRequest(
            @NotNull Long productoId,
            @NotNull @Min(1) @Max(100) Integer cantidad,
            @Size(max = 255) String notas) {
    }

    public record CrearPedidoRequest(
            @NotNull TipoPedido tipo,
            Long mesaId,
            @Size(max = 120) String nombreCliente,
            @Valid EntregaRequest entrega,
            @NotEmpty List<@Valid ItemPedidoRequest> items) {
    }

    /** Si productoId viene, el detalle cambia de producto y toma el precio vigente de ese producto. */
    public record ActualizarDetalleRequest(
            Long productoId,
            @NotNull @Min(1) @Max(100) Integer cantidad,
            @Size(max = 255) String notas) {
    }

    public record CambiarEstadoPedidoRequest(
            @NotNull EstadoPedido estado,
            @Size(max = 255) String comentario) {
    }

    // ------------------------------------------------------------- respuestas

    public record DetallePedidoResponse(Long id, Long productoId, String productoNombre, int cantidad,
                                        BigDecimal precioUnitario, BigDecimal subtotal, String notas) {

        public static DetallePedidoResponse de(DetallePedido d) {
            return new DetallePedidoResponse(d.getId(), d.getProducto().getId(), d.getProducto().getNombre(),
                    d.getCantidad(), d.getPrecioUnitario(), d.subtotal(), d.getNotas());
        }
    }

    public record EntregaResponse(String direccion, String telefono, BigDecimal costoEnvio, MetodoPago metodoPago) {
    }

    public record MesaRef(Long id, Integer numero) {
    }

    public record UsuarioRef(Long id, String nombre) {
    }

    public record PedidoResponse(Long id, TipoPedido tipo, EstadoPedido estado, MesaRef mesa, UsuarioRef mesero,
                                 String nombreCliente, EntregaResponse entrega, BigDecimal total,
                                 List<DetallePedidoResponse> detalles, Instant createdAt, Instant updatedAt,
                                 Instant entregadoEn) {

        public static PedidoResponse de(Pedido p) {
            EntregaResponse entrega = p.getTipo() == TipoPedido.DOMICILIO
                    ? new EntregaResponse(p.getDireccionEntrega(), p.getTelefonoEntrega(), p.getCostoEnvio(),
                    p.getMetodoPago())
                    : null;
            MesaRef mesa = p.getMesa() == null ? null : new MesaRef(p.getMesa().getId(), p.getMesa().getNumero());
            return new PedidoResponse(p.getId(), p.getTipo(), p.getEstado(), mesa,
                    new UsuarioRef(p.getMesero().getId(), p.getMesero().getNombre()), p.getNombreCliente(), entrega,
                    p.getTotal(), p.getDetalles().stream().map(DetallePedidoResponse::de).toList(),
                    p.getCreatedAt(), p.getUpdatedAt(), p.getEntregadoEn());
        }
    }

    /** Versión liviana para listados paginados (sin detalles). */
    public record PedidoResumenResponse(Long id, TipoPedido tipo, EstadoPedido estado, Integer mesaNumero,
                                        String nombreCliente, UsuarioRef mesero, BigDecimal total,
                                        Instant createdAt) {

        public static PedidoResumenResponse de(Pedido p) {
            return new PedidoResumenResponse(p.getId(), p.getTipo(), p.getEstado(),
                    p.getMesa() == null ? null : p.getMesa().getNumero(), p.getNombreCliente(),
                    new UsuarioRef(p.getMesero().getId(), p.getMesero().getNombre()), p.getTotal(),
                    p.getCreatedAt());
        }
    }

    public record HistorialResponse(Long id, EstadoPedido estadoAnterior, EstadoPedido estadoNuevo, UsuarioRef usuario,
                                    String comentario, Instant fecha) {

        public static HistorialResponse de(HistorialEstado h) {
            return new HistorialResponse(h.getId(), h.getEstadoAnterior(), h.getEstadoNuevo(),
                    new UsuarioRef(h.getUsuario().getId(), h.getUsuario().getNombre()), h.getComentario(),
                    h.getCreatedAt());
        }
    }
}
