package dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import util.Bd;

/**
 * Capa de acceso a datos (DAO) de consulta: qué registros están enlazados (por llave
 * foránea) a otro registro. Cada método devuelve una línea de texto por registro
 * enlazado; DependenciasService arma el informe con ellas.
 */
public class DependenciasDAO {

    // ---- Administrador
    public List<String> mesasDeAdministrador(int id) {
        return Bd.consultar("SELECT id_mesa, numero, estado FROM Mesa WHERE id_administrador = ?",
                rs -> "mesa id " + rs.getInt("id_mesa") + " (#" + rs.getInt("numero")
                        + ", " + rs.getString("estado") + ")", id);
    }

    public List<String> categoriasDeAdministrador(int id) {
        return Bd.consultar("SELECT id_categoria, nombre FROM Categoria WHERE id_administrador = ?",
                rs -> "categoría id " + rs.getInt("id_categoria") + " (" + rs.getString("nombre") + ")", id);
    }

    public List<String> productosDeAdministrador(int id) {
        return Bd.consultar("SELECT id_producto, nombre FROM Producto WHERE id_administrador = ?",
                rs -> "producto id " + rs.getInt("id_producto") + " (" + rs.getString("nombre") + ")", id);
    }

    public List<String> pedidosDeAdministrador(int id) {
        return Bd.consultar("SELECT id_pedido, tipo_pedido, estado FROM Pedido WHERE id_administrador = ?",
                DependenciasDAO::pedido, id);
    }

    // ---- Mesero, cocina y mesa
    public List<String> pedidosDeMesero(int id) {
        return Bd.consultar("SELECT id_pedido, tipo_pedido, estado FROM Pedido WHERE id_mesero = ?",
                DependenciasDAO::pedido, id);
    }

    public List<String> pedidosDeCocina(int id) {
        return Bd.consultar("SELECT id_pedido, tipo_pedido, estado FROM Pedido WHERE id_cocina = ?",
                DependenciasDAO::pedido, id);
    }

    public List<String> pedidosDeMesa(int id) {
        return Bd.consultar("SELECT id_pedido, tipo_pedido, estado FROM Pedido WHERE id_mesa = ?",
                DependenciasDAO::pedido, id);
    }

    // ---- Categoría y producto
    public List<String> productosDeCategoria(int id) {
        return Bd.consultar("SELECT id_producto, nombre FROM Producto WHERE id_categoria = ?",
                rs -> "producto id " + rs.getInt("id_producto") + " (" + rs.getString("nombre") + ")", id);
    }

    public List<String> detallesDeProducto(int id) {
        return Bd.consultar("SELECT id_detalle, id_pedido, cantidad FROM DetallePedido WHERE id_producto = ?",
                rs -> "detalle id " + rs.getInt("id_detalle") + " (pedido " + rs.getInt("id_pedido")
                        + ", cantidad " + rs.getInt("cantidad") + ")", id);
    }

    private static String pedido(ResultSet rs) throws SQLException {
        return "pedido id " + rs.getInt("id_pedido") + " (" + rs.getString("tipo_pedido")
                + ", " + rs.getString("estado") + ")";
    }
}
