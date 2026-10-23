package dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import entidades.Categoria;
import util.Bd;

/**
 * Capa de acceso a datos (DAO) de la tabla Categoria.
 * Solo contiene SQL y mapeo de filas a objetos; las reglas de negocio
 * (nombre único, administrador existente, productos asociados) viven en CategoriaService.
 */
public class CategoriaDAO {
    private static final String TABLA = "Categoria";
    private static final String COL_ID = "id_categoria";
    private static final String[] COLS = {"nombre", "id_administrador"};

    private static Categoria mapear(ResultSet rs) throws SQLException {
        Categoria c = new Categoria();
        c.setId_categoria(rs.getInt("id_categoria"));
        c.setNombre(rs.getString("nombre"));
        c.setId_administrador(rs.getInt("id_administrador"));
        return c;
    }

    private static Object[] valores(Categoria c) {
        return new Object[] {c.getNombre(), Bd.fk(c.getId_administrador())};
    }

    /** INSERT. Si la entidad trae id > 0 se usa ese id; si no, lo genera AUTO_INCREMENT. */
    public Categoria crear(Categoria entidad) {
        entidad.setId_categoria(Bd.insertar(TABLA, COL_ID, entidad.getId_categoria(), COLS, valores(entidad)));
        return entidad;
    }

    /** Devuelve la categoría o null si no existe. */
    public Categoria obtenerPorId(int id_categoria) {
        return Bd.uno("SELECT * FROM Categoria WHERE id_categoria = ?", CategoriaDAO::mapear, id_categoria);
    }

    public List<Categoria> obtenerTodos() {
        return Bd.consultar("SELECT * FROM Categoria ORDER BY nombre", CategoriaDAO::mapear);
    }

    public Categoria actualizar(Categoria entidad) {
        Bd.actualizar(TABLA, COL_ID, entidad.getId_categoria(), COLS, valores(entidad));
        return entidad;
    }

    public void eliminar(int id_categoria) {
        Bd.eliminar(TABLA, COL_ID, id_categoria);
    }

    // ---------------------------------------------------------------- extras

    /** Cantidad de categorías con ese nombre (sin distinguir mayúsculas), sin contar la indicada. */
    public int contarPorNombre(String nombre, int idPropio) {
        return Bd.entero("SELECT COUNT(*) FROM Categoria WHERE LOWER(nombre) = LOWER(?) AND id_categoria <> ?",
                nombre, idPropio);
    }

    public int contarPorAdministrador(int id_administrador) {
        return Bd.entero("SELECT COUNT(*) FROM Categoria WHERE id_administrador = ?", id_administrador);
    }
}
