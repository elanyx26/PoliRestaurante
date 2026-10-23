package dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import entidades.DetallePedido;
import util.Bd;

/**
 * Capa de acceso a datos (DAO) de la tabla DetallePedido.
 * Solo contiene SQL y mapeo de filas a objetos; las reglas de negocio
 * (pedido editable, producto disponible, cálculo del total) viven en DetallePedidoService.
 */
public class DetallePedidoDAO {
    private static final String TABLA = "DetallePedido";
    private static final String COL_ID = "id_detalle";
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

    /** INSERT. Si la entidad trae id > 0 se usa ese id; si no, lo genera AUTO_INCREMENT. */
    public DetallePedido crear(DetallePedido entidad) {
        entidad.setId_detalle(Bd.insertar(TABLA, COL_ID, entidad.getId_detalle(), COLS, valores(entidad)));
        return entidad;
    }

    /** Devuelve el detalle o null si no existe. */
    public DetallePedido obtenerPorId(int id_detalle) {
        return Bd.uno("SELECT * FROM DetallePedido WHERE id_detalle = ?", DetallePedidoDAO::mapear, id_detalle);
    }

    public List<DetallePedido> obtenerTodos() {
        return Bd.consultar("SELECT * FROM DetallePedido ORDER BY id_detalle", DetallePedidoDAO::mapear);
    }

    public DetallePedido actualizar(DetallePedido entidad) {
        Bd.actualizar(TABLA, COL_ID, entidad.getId_detalle(), COLS, valores(entidad));
        return entidad;
    }

    public void eliminar(int id_detalle) {
        Bd.eliminar(TABLA, COL_ID, id_detalle);
    }

    // ---------------------------------------------------------------- extras

    public List<DetallePedido> obtenerPorPedido(int id_pedido) {
        return Bd.consultar("SELECT * FROM DetallePedido WHERE id_pedido = ? ORDER BY id_detalle",
                DetallePedidoDAO::mapear, id_pedido);
    }

    public void eliminarPorPedido(int id_pedido) {
        Bd.ejecutar("DELETE FROM DetallePedido WHERE id_pedido = ?", id_pedido);
    }

    public int contarPorProducto(int id_producto) {
        return Bd.entero("SELECT COUNT(*) FROM DetallePedido WHERE id_producto = ?", id_producto);
    }

    /** Suma de cantidad x precio unitario de todas las líneas del pedido (0 si no tiene). */
    public int sumarSubtotales(int id_pedido) {
        return Bd.entero("SELECT COALESCE(SUM(cantidad * precio_unitario), 0) FROM DetallePedido WHERE id_pedido = ?",
                id_pedido);
    }
}
