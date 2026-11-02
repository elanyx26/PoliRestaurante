package com.poli.restaurante.service;

import com.poli.restaurante.domain.DetallePedido;
import com.poli.restaurante.domain.EstadoMesa;
import com.poli.restaurante.domain.EstadoPedido;
import com.poli.restaurante.domain.HistorialEstado;
import com.poli.restaurante.domain.Mesa;
import com.poli.restaurante.domain.MetodoPago;
import com.poli.restaurante.domain.Pedido;
import com.poli.restaurante.domain.Producto;
import com.poli.restaurante.domain.Rol;
import com.poli.restaurante.domain.TipoPedido;
import com.poli.restaurante.domain.Usuario;
import com.poli.restaurante.exception.BusinessRuleException;
import com.poli.restaurante.exception.ConflictException;
import com.poli.restaurante.exception.ResourceNotFoundException;
import com.poli.restaurante.realtime.MesaEvento;
import com.poli.restaurante.realtime.PedidoEvento;
import com.poli.restaurante.repository.HistorialEstadoRepository;
import com.poli.restaurante.repository.MesaRepository;
import com.poli.restaurante.repository.PedidoRepository;
import com.poli.restaurante.repository.ProductoRepository;
import com.poli.restaurante.repository.UsuarioRepository;
import com.poli.restaurante.security.UsuarioActual;
import com.poli.restaurante.web.dto.PedidoDtos.ActualizarDetalleRequest;
import com.poli.restaurante.web.dto.PedidoDtos.CambiarEstadoPedidoRequest;
import com.poli.restaurante.web.dto.PedidoDtos.CrearPedidoRequest;
import com.poli.restaurante.web.dto.PedidoDtos.EntregaRequest;
import com.poli.restaurante.web.dto.PedidoDtos.ItemPedidoRequest;
import com.poli.restaurante.web.dto.PedidoDtos.PedidoResponse;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Comandos sobre pedidos (crear, editar, cambiar de estado). Cada operación bloquea la fila del pedido
 * (y la mesa cuando aplica) para que ediciones y transiciones concurrentes se serialicen.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PedidoService {

    private final PedidoRepository pedidos;
    private final MesaRepository mesas;
    private final ProductoRepository productos;
    private final UsuarioRepository usuarios;
    private final HistorialEstadoRepository historial;
    private final ConfiguracionDomiciliosService configuracion;
    private final ApplicationEventPublisher eventos;
    private final Clock clock;

    // ------------------------------------------------------------ creación

    public PedidoResponse crear(CrearPedidoRequest req, UsuarioActual actor) {
        Usuario mesero = usuarios.getReferenceById(actor.id());
        Pedido pedido = new Pedido();
        pedido.setTipo(req.tipo());
        pedido.setMesero(mesero);
        pedido.setEstado(EstadoPedido.PENDIENTE);
        configurarSegunTipo(pedido, req);

        Map<Long, Producto> catalogo = cargarProductos(req.items().stream().map(ItemPedidoRequest::productoId).toList());
        for (ItemPedidoRequest item : req.items()) {
            pedido.agregarDetalle(nuevoDetalle(catalogo.get(item.productoId()), item.cantidad(), item.notas()));
        }
        pedido.recalcularTotal();
        validarMetodoDePago(pedido);

        pedidos.saveAndFlush(pedido);
        registrarHistorial(pedido, null, EstadoPedido.PENDIENTE, mesero, null);
        publicar(PedidoEvento.Tipo.PEDIDO_CREADO, pedido, null);
        return PedidoResponse.de(pedido);
    }

    // --------------------------------------------------- edición (PENDIENTE)

    public PedidoResponse agregarDetalle(Long pedidoId, ItemPedidoRequest item, UsuarioActual actor) {
        Pedido pedido = pedidoEditable(pedidoId, actor);
        Producto producto = productos.findById(item.productoId())
                .orElseThrow(() -> ResourceNotFoundException.de("Producto", item.productoId()));
        pedido.agregarDetalle(nuevoDetalle(producto, item.cantidad(), item.notas()));
        return guardarEdicion(pedido);
    }

    public PedidoResponse actualizarDetalle(Long pedidoId, Long detalleId, ActualizarDetalleRequest req,
                                            UsuarioActual actor) {
        Pedido pedido = pedidoEditable(pedidoId, actor);
        DetallePedido detalle = buscarDetalle(pedido, detalleId);
        if (req.productoId() != null && !req.productoId().equals(detalle.getProducto().getId())) {
            Producto nuevo = productos.findById(req.productoId())
                    .orElseThrow(() -> ResourceNotFoundException.de("Producto", req.productoId()));
            exigirVendible(nuevo);
            detalle.setProducto(nuevo);
            detalle.setPrecioUnitario(nuevo.getPrecio());
        }
        // Sin cambio de producto se conserva el precio guardado en el detalle.
        detalle.setCantidad(req.cantidad());
        detalle.setNotas(limpiar(req.notas()));
        return guardarEdicion(pedido);
    }

    public PedidoResponse eliminarDetalle(Long pedidoId, Long detalleId, UsuarioActual actor) {
        Pedido pedido = pedidoEditable(pedidoId, actor);
        DetallePedido detalle = buscarDetalle(pedido, detalleId);
        if (pedido.getDetalles().size() <= 1) {
            throw new BusinessRuleException("No se puede eliminar el último producto del pedido");
        }
        pedido.quitarDetalle(detalle);
        return guardarEdicion(pedido);
    }

    // --------------------------------------------------- cambios de estado

    public PedidoResponse cambiarEstado(Long pedidoId, CambiarEstadoPedidoRequest req, UsuarioActual actor) {
        Pedido pedido = bloquear(pedidoId);
        EstadoPedido destino = req.estado();
        exigirPermisoDeTransicion(pedido, destino, actor);

        EstadoPedido anterior = pedido.getEstado();
        if (!anterior.puedePasarA(destino)) {
            throw new ConflictException("No se puede pasar un pedido de " + anterior + " a " + destino);
        }
        pedido.setEstado(destino);
        if (destino == EstadoPedido.ENTREGADO) {
            pedido.setEntregadoEn(clock.instant());
        }
        if ((destino == EstadoPedido.ENTREGADO || destino == EstadoPedido.CANCELADO)
                && pedido.getTipo() == TipoPedido.SALON) {
            liberarMesa(pedido.getMesa());
        }
        pedidos.saveAndFlush(pedido);
        registrarHistorial(pedido, anterior, destino, usuarios.getReferenceById(actor.id()), limpiar(req.comentario()));
        publicar(PedidoEvento.Tipo.PEDIDO_ESTADO_CAMBIADO, pedido, anterior);
        return PedidoResponse.de(pedido);
    }

    /**
     * Cocina: PENDIENTE → EN_PREPARACION → LISTO. Mesero propietario: LISTO → ENTREGADO y cancelar.
     * El administrador nunca cambia el estado operativo.
     */
    private void exigirPermisoDeTransicion(Pedido pedido, EstadoPedido destino, UsuarioActual actor) {
        if (actor.rol() == Rol.ADMINISTRADOR) {
            throw new AccessDeniedException("El administrador no puede cambiar el estado de un pedido");
        }
        switch (destino) {
            case EN_PREPARACION, LISTO -> {
                if (actor.rol() != Rol.COCINA) {
                    throw new AccessDeniedException("Solo cocina puede pasar un pedido a " + destino);
                }
            }
            case ENTREGADO, CANCELADO -> {
                if (actor.rol() != Rol.MESERO) {
                    throw new AccessDeniedException("Solo el mesero puede pasar un pedido a " + destino);
                }
                exigirPropietario(pedido, actor);
            }
            default -> {
                // PENDIENTE no es destino válido: lo rechaza la máquina de estados.
            }
        }
    }

    // ----------------------------------------------------------- reglas de tipo

    private void configurarSegunTipo(Pedido pedido, CrearPedidoRequest req) {
        String cliente = limpiar(req.nombreCliente());
        switch (req.tipo()) {
            case SALON -> {
                if (req.mesaId() == null) {
                    throw new BusinessRuleException("Un pedido de salón requiere mesaId");
                }
                exigirSinEntrega(req);
                Mesa mesa = mesas.findByIdForUpdate(req.mesaId())
                        .orElseThrow(() -> ResourceNotFoundException.de("Mesa", req.mesaId()));
                if (mesa.getEstado() != EstadoMesa.LIBRE) {
                    throw new ConflictException("La mesa " + mesa.getNumero() + " no está libre");
                }
                mesa.setEstado(EstadoMesa.OCUPADA);
                pedido.setMesa(mesa);
                pedido.setNombreCliente(cliente);
                eventos.publishEvent(new MesaEvento(mesa.getId(), mesa.getNumero(), mesa.getEstado(), clock.instant()));
            }
            case PARA_LLEVAR -> {
                exigirSinMesa(req);
                exigirSinEntrega(req);
                pedido.setNombreCliente(exigirCliente(cliente));
            }
            case DOMICILIO -> {
                exigirSinMesa(req);
                EntregaRequest entrega = req.entrega();
                if (entrega == null) {
                    throw new BusinessRuleException("Un pedido a domicilio requiere dirección, teléfono y método de pago");
                }
                pedido.setNombreCliente(exigirCliente(cliente));
                pedido.setDireccionEntrega(entrega.direccion().trim());
                pedido.setTelefonoEntrega(entrega.telefono().trim());
                pedido.setCostoEnvio(entrega.costoEnvio() != null ? entrega.costoEnvio() : BigDecimal.ZERO);
                pedido.setMetodoPago(entrega.metodoPago());
            }
        }
    }

    private static void exigirSinMesa(CrearPedidoRequest req) {
        if (req.mesaId() != null) {
            throw new BusinessRuleException("Solo los pedidos de salón llevan mesa");
        }
    }

    private static void exigirSinEntrega(CrearPedidoRequest req) {
        if (req.entrega() != null) {
            throw new BusinessRuleException("Solo los pedidos a domicilio llevan datos de entrega");
        }
    }

    private static String exigirCliente(String cliente) {
        if (cliente == null) {
            throw new BusinessRuleException("El nombre del cliente es obligatorio para pedidos para llevar y a domicilio");
        }
        return cliente;
    }

    /** Contraentrega solo hasta el límite configurado; por encima se debe usar pago virtual. */
    private void validarMetodoDePago(Pedido pedido) {
        if (pedido.getTipo() == TipoPedido.DOMICILIO && pedido.getMetodoPago() == MetodoPago.EFECTIVO_CONTRAENTREGA) {
            BigDecimal limite = configuracion.limiteContraentrega();
            if (pedido.getTotal().compareTo(limite) > 0) {
                throw new BusinessRuleException("El total " + pedido.getTotal().toPlainString()
                        + " supera el límite de contraentrega (" + limite.toPlainString()
                        + "); use PAGO_VIRTUAL");
            }
        }
    }

    // ----------------------------------------------------------------- apoyo

    private Pedido bloquear(Long id) {
        return pedidos.findByIdForUpdate(id).orElseThrow(() -> ResourceNotFoundException.de("Pedido", id));
    }

    private Pedido pedidoEditable(Long id, UsuarioActual actor) {
        Pedido pedido = bloquear(id);
        exigirPropietario(pedido, actor);
        if (pedido.getEstado() != EstadoPedido.PENDIENTE) {
            throw new ConflictException("Solo se puede modificar un pedido en estado PENDIENTE");
        }
        return pedido;
    }

    private static void exigirPropietario(Pedido pedido, UsuarioActual actor) {
        if (!pedido.getMesero().getId().equals(actor.id())) {
            throw new AccessDeniedException("Solo el mesero propietario puede operar este pedido");
        }
    }

    private PedidoResponse guardarEdicion(Pedido pedido) {
        pedido.recalcularTotal();
        validarMetodoDePago(pedido);
        pedidos.saveAndFlush(pedido);
        publicar(PedidoEvento.Tipo.PEDIDO_ACTUALIZADO, pedido, null);
        return PedidoResponse.de(pedido);
    }

    private static DetallePedido buscarDetalle(Pedido pedido, Long detalleId) {
        return pedido.getDetalles().stream()
                .filter(d -> d.getId().equals(detalleId))
                .findFirst()
                .orElseThrow(() -> ResourceNotFoundException.de("Detalle", detalleId));
    }

    private Map<Long, Producto> cargarProductos(List<Long> ids) {
        Set<Long> unicos = Set.copyOf(ids);
        Map<Long, Producto> encontrados = new HashMap<>();
        productos.findAllById(unicos).forEach(p -> encontrados.put(p.getId(), p));
        Set<Long> faltantes = unicos.stream().filter(id -> !encontrados.containsKey(id)).collect(Collectors.toSet());
        if (!faltantes.isEmpty()) {
            throw new ResourceNotFoundException("No existen los productos con id " + faltantes);
        }
        return encontrados;
    }

    private DetallePedido nuevoDetalle(Producto producto, int cantidad, String notas) {
        exigirVendible(producto);
        DetallePedido d = new DetallePedido();
        d.setProducto(producto);
        d.setCantidad(cantidad);
        d.setPrecioUnitario(producto.getPrecio());
        d.setNotas(limpiar(notas));
        return d;
    }

    private static void exigirVendible(Producto producto) {
        if (!producto.isDisponible() || !producto.getCategoria().isActiva()) {
            throw new BusinessRuleException("El producto '" + producto.getNombre() + "' no está disponible");
        }
    }

    private void liberarMesa(Mesa mesa) {
        Mesa bloqueada = mesas.findByIdForUpdate(mesa.getId()).orElseThrow();
        bloqueada.setEstado(EstadoMesa.LIBRE);
        eventos.publishEvent(new MesaEvento(bloqueada.getId(), bloqueada.getNumero(), bloqueada.getEstado(),
                clock.instant()));
    }

    private void registrarHistorial(Pedido pedido, EstadoPedido anterior, EstadoPedido nuevo, Usuario usuario,
                                    String comentario) {
        HistorialEstado h = new HistorialEstado();
        h.setPedido(pedido);
        h.setEstadoAnterior(anterior);
        h.setEstadoNuevo(nuevo);
        h.setUsuario(usuario);
        h.setComentario(comentario);
        historial.save(h);
    }

    private void publicar(PedidoEvento.Tipo tipo, Pedido pedido, EstadoPedido anterior) {
        eventos.publishEvent(new PedidoEvento(tipo, pedido.getId(), pedido.getEstado(), anterior, pedido.getTipo(),
                pedido.getMesero().getId(), pedido.getMesa() == null ? null : pedido.getMesa().getNumero(),
                clock.instant()));
    }

    private static String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
