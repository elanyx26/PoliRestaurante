package servicios;

import java.util.List;
import dao.DetallePedidoDAO;
import entidades.DetallePedido;
import entidades.Pedido;
import entidades.Producto;
import util.Validar;

/**
 * HU2, HU11, HU16 / RF-03: productos de un pedido (tabla DetallePedido).
 * Solo se pueden agregar, editar o quitar productos (y notas) antes de enviar
 * el pedido a cocina. El precio unitario siempre se toma del producto y el
 * total del pedido se recalcula automáticamente.
 */
public class DetallePedidoService {
    private final DetallePedidoDAO dao = new DetallePedidoDAO();

    public DetallePedido crear(DetallePedido entidad) {
        Validar.noNulo(entidad, "DetallePedido");
        Pedido pedido = pedidoEditable(entidad.getId_pedido());
        Producto producto = productoValido(entidad.getId_producto());
        Validar.positivo(entidad.getCantidad(), "cantidad");

        entidad.setPrecio_unitario(producto.getPrecio());   // el usuario no define el precio
        dao.crear(entidad);
        entidad.setPedido(pedido);
        entidad.setProducto(producto);
        new PedidoService().recalcularTotal(entidad.getId_pedido());
        return entidad;
    }

    public DetallePedido obtenerPorId(int id_detalle) {
        DetallePedido d = dao.obtenerPorId(id_detalle);
        if (d != null) {
            d.setPedido(new PedidoService().obtenerPorId(d.getId_pedido()));
            d.setProducto(new ProductoService().obtenerPorId(d.getId_producto()));
        }
        return d;
    }

    public List<DetallePedido> listar() {
        return dao.obtenerTodos();
    }

    /** Permite cambiar cantidad, producto y notas mientras el pedido no se haya enviado a cocina. */
    public DetallePedido actualizar(DetallePedido entidad) {
        Validar.noNulo(entidad, "DetallePedido");
        DetallePedido existente = obtenerPorId(entidad.getId_detalle());
        if (existente == null) {
            throw new IllegalArgumentException("No existe un detalle con id " + entidad.getId_detalle());
        }
        // el detalle no se puede mover a otro pedido
        entidad.setId_pedido(existente.getId_pedido());
        Pedido pedido = pedidoEditable(existente.getId_pedido());
        Producto producto = productoValido(entidad.getId_producto());
        Validar.positivo(entidad.getCantidad(), "cantidad");

        if (entidad.getId_producto() == existente.getId_producto()) {
            entidad.setPrecio_unitario(existente.getPrecio_unitario());
        } else {
            entidad.setPrecio_unitario(producto.getPrecio());
        }
        dao.actualizar(entidad);
        entidad.setPedido(pedido);
        entidad.setProducto(producto);
        new PedidoService().recalcularTotal(entidad.getId_pedido());
        return entidad;
    }

    public void eliminar(int id_detalle) {
        DetallePedido d = dao.obtenerPorId(id_detalle);
        if (d == null) {
            return;
        }
        pedidoEditable(d.getId_pedido());
        dao.eliminar(id_detalle);
        new PedidoService().recalcularTotal(d.getId_pedido());
    }

    // ---------------------------------------------------------------- extras

    /** HU11: productos de un pedido (cantidad, precio unitario y notas). */
    public List<DetallePedido> listarPorPedido(int id_pedido) {
        return dao.obtenerPorPedido(id_pedido);
    }

    /** Uso interno: al borrar un pedido borrador se eliminan sus productos. */
    void eliminarPorPedido(int id_pedido) {
        dao.eliminarPorPedido(id_pedido);
    }

    // --------------------------------------------------------------- privados

    private Pedido pedidoEditable(int id_pedido) {
        PedidoService ps = new PedidoService();
        Pedido p = ps.obtenerPorId(id_pedido);
        if (p == null) {
            throw new IllegalArgumentException("No existe el pedido " + id_pedido);
        }
        if (!ps.esEditable(id_pedido)) {
            throw new IllegalStateException(
                    "El pedido ya fue enviado a cocina o finalizó; no se pueden editar sus productos");
        }
        return p;
    }

    private Producto productoValido(int id_producto) {
        ProductoService prs = new ProductoService();
        Producto producto = prs.obtenerPorId(id_producto);
        if (producto == null) {
            throw new IllegalArgumentException("No existe el producto " + id_producto);
        }
        if (!prs.estaDisponible(producto)) {
            throw new IllegalStateException("El producto '" + producto.getNombre() + "' no está disponible");
        }
        return producto;
    }
}
