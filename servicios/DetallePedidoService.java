package servicios;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import entidades.DetallePedido;
import entidades.Pedido;
import entidades.Producto;
import util.Bd;
import util.Validar;

/**
 * HU2, HU11, HU16 / RF-03: productos de un pedido (tabla DetallePedido).
 * Solo se pueden agregar, editar o quitar productos (y notas) antes de enviar
 * el pedido a cocina. El precio unitario siempre se toma del producto y el
 * total del pedido se recalcula automáticamente.
 */
public class DetallePedidoService {
    private static final String[] COLS = {"id_pedido", "id_producto", "cantidad", "precio_unitario", "notas"};

    private static DetallePedido mapear(ResultSet rs) throws SQLException {
        DetallePedido d = new DetallePedido();
        d.setId_detalle(rs.getInt("id_detalle"));
        d.setId_pedido(rs.getInt("id_pedido"));
        d.setId_producto(rs.getInt("id_producto"));
        d.setCantidad(rs.getInt("cantidad"));
        d.setPrecio_unitario(rs.getInt("precio_unitario"));
        d.setNotas(rs.getString("notas"));
        return d;
    }

    private static Object[] valores(DetallePedido d) {
        return new Object[] {d.getId_pedido(), d.getId_producto(), d.getCantidad(), d.getPrecio_unitario(), d.getNotas()};
    }

    public DetallePedido crear(DetallePedido entidad) {
        Validar.noNulo(entidad, "DetallePedido");
        Pedido pedido = pedidoEditable(entidad.getId_pedido());
        Producto producto = productoValido(entidad.getId_producto());
        Validar.positivo(entidad.getCantidad(), "cantidad");

        entidad.setPrecio_unitario(producto.getPrecio());   // el usuario no define el precio
        entidad.setId_detalle(Bd.insertar("DetallePedido", "id_detalle", entidad.getId_detalle(), COLS, valores(entidad)));
        entidad.setPedido(pedido);
        entidad.setProducto(producto);
        new PedidoService().recalcularTotal(entidad.getId_pedido());
        return entidad;
    }

    public DetallePedido obtenerPorId(int id_detalle) {
        DetallePedido d = Bd.uno("SELECT * FROM DetallePedido WHERE id_detalle = ?", DetallePedidoService::mapear, id_detalle);
        if (d != null) {
            d.setPedido(new PedidoService().obtenerPorId(d.getId_pedido()));
            d.setProducto(new ProductoService().obtenerPorId(d.getId_producto()));
        }
        return d;
    }

    public List<DetallePedido> listar() {
        return Bd.consultar("SELECT * FROM DetallePedido ORDER BY id_detalle", DetallePedidoService::mapear);
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
        Bd.actualizar("DetallePedido", "id_detalle", entidad.getId_detalle(), COLS, valores(entidad));
        entidad.setPedido(pedido);
        entidad.setProducto(producto);
        new PedidoService().recalcularTotal(entidad.getId_pedido());
        return entidad;
    }

    public void eliminar(int id_detalle) {
        DetallePedido d = Bd.uno("SELECT * FROM DetallePedido WHERE id_detalle = ?", DetallePedidoService::mapear, id_detalle);
        if (d == null) {
            return;
        }
        pedidoEditable(d.getId_pedido());
        Bd.eliminar("DetallePedido", "id_detalle", id_detalle);
        new PedidoService().recalcularTotal(d.getId_pedido());
    }

    // ---------------------------------------------------------------- extras

    /** HU11: productos de un pedido (cantidad, precio unitario y notas). */
    public List<DetallePedido> listarPorPedido(int id_pedido) {
        return Bd.consultar("SELECT * FROM DetallePedido WHERE id_pedido = ? ORDER BY id_detalle",
                DetallePedidoService::mapear, id_pedido);
    }

    /** Uso interno: al borrar un pedido borrador se eliminan sus productos. */
    void eliminarPorPedido(int id_pedido) {
        Bd.ejecutar("DELETE FROM DetallePedido WHERE id_pedido = ?", id_pedido);
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
