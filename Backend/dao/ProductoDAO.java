package dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import entidades.Producto;
import util.Bd;

/**
 * Capa de acceso a datos (DAO) de la tabla Producto.
 * Solo contiene SQL y mapeo de filas a objetos; las reglas de negocio
 * (precio, categoría, disponibilidad) viven en ProductoService.
 */
public class ProductoDAO {
    private static final String TABLA = "Producto";
    private static final String COL_ID = "id_producto";
    private static final String[] COLS =
            {"nombre", "descripcion", "precio", "id_categoria", "id_administrador", "disponibilidad"};

    private static Producto mapear(ResultSet rs) throws SQLException {
        Producto p = new Producto();
        p.setId_producto(rs.getInt("id_producto"));
        p.setNombre(rs.getString("nombre"));
        p.setDescripcion(rs.getString("descripcion"));
        p.setPrecio(rs.getInt("precio"));
        p.setId_categoria(rs.getInt("id_categoria"));
        p.setId_administrador(rs.getInt("id_administrador"));
        p.setDisponibilidad(rs.getString("disponibilidad"));
        return p;
    }

    private static Object[] valores(Producto p) {
        return new Object[] {p.getNombre(), p.getDescripcion(), p.getPrecio(),
                Bd.fk(p.getId_categoria()), Bd.fk(p.getId_administrador()), p.getDisponibilidad()};
    }

    /** INSERT. Si la entidad trae id > 0 se usa ese id; si no, lo genera AUTO_INCREMENT. */
    public Producto crear(Producto entidad) {
        entidad.setId_producto(Bd.insertar(TABLA, COL_ID, entidad.getId_producto(), COLS, valores(entidad)));
        return entidad;
    }

    /** Devuelve el producto o null si no existe. */
    public Producto obtenerPorId(int id_producto) {
        return Bd.uno("SELECT * FROM Producto WHERE id_producto = ?", ProductoDAO::mapear, id_producto);
    }

    public List<Producto> obtenerTodos() {
        return Bd.consultar("SELECT * FROM Producto ORDER BY id_producto", ProductoDAO::mapear);
    }

    public Producto actualizar(Producto entidad) {
        Bd.actualizar(TABLA, COL_ID, entidad.getId_producto(), COLS, valores(entidad));
        return entidad;
    }

    public void eliminar(int id_producto) {
        Bd.eliminar(TABLA, COL_ID, id_producto);
    }

    // ---------------------------------------------------------------- extras

    /** Productos con esa disponibilidad (sin distinguir mayúsculas), ordenados por nombre. */
    public List<Producto> obtenerPorDisponibilidad(String disponibilidad) {
        return Bd.consultar("SELECT * FROM Producto WHERE LOWER(disponibilidad) = LOWER(?) ORDER BY nombre",
                ProductoDAO::mapear, disponibilidad);
    }

    public void actualizarDisponibilidad(int id_producto, String disponibilidad) {
        Bd.ejecutar("UPDATE Producto SET disponibilidad = ? WHERE id_producto = ?", disponibilidad, id_producto);
    }

    public int contarPorCategoria(int id_categoria) {
        return Bd.entero("SELECT COUNT(*) FROM Producto WHERE id_categoria = ?", id_categoria);
    }

    public int contarPorAdministrador(int id_administrador) {
        return Bd.entero("SELECT COUNT(*) FROM Producto WHERE id_administrador = ?", id_administrador);
    }
}
