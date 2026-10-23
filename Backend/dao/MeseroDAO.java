package dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import entidades.Mesero;
import util.Bd;

/**
 * Capa de acceso a datos (DAO) de la tabla Mesero.
 * Solo contiene SQL y mapeo de filas a objetos; las reglas de negocio
 * (validaciones, activar/desactivar, contraseñas) viven en MeseroService.
 */
public class MeseroDAO {
    private static final String TABLA = "Mesero";
    private static final String COL_ID = "id_mesero";
    private static final String[] COLS = {"nombre", "usuario", "contrasena"};

    private static Mesero mapear(ResultSet rs) throws SQLException {
        Mesero u = new Mesero();
        u.setId_mesero(rs.getInt("id_mesero"));
        u.setNombre(rs.getString("nombre"));
        u.setUsuario(rs.getString("usuario"));
        u.setContrasena(rs.getString("contrasena"));
        return u;
    }

    private static Object[] valores(Mesero u) {
        return new Object[] {u.getNombre(), u.getUsuario(), u.getContrasena()};
    }

    /** INSERT. Si la entidad trae id > 0 se usa ese id; si no, lo genera AUTO_INCREMENT. */
    public Mesero crear(Mesero entidad) {
        entidad.setId_mesero(Bd.insertar(TABLA, COL_ID, entidad.getId_mesero(), COLS, valores(entidad)));
        return entidad;
    }

    /** Devuelve el registro o null si no existe. */
    public Mesero obtenerPorId(int id) {
        return Bd.uno("SELECT * FROM " + TABLA + " WHERE " + COL_ID + " = ?", MeseroDAO::mapear, id);
    }

    public List<Mesero> obtenerTodos() {
        return Bd.consultar("SELECT * FROM " + TABLA + " ORDER BY " + COL_ID, MeseroDAO::mapear);
    }

    public Mesero actualizar(Mesero entidad) {
        Bd.actualizar(TABLA, COL_ID, entidad.getId_mesero(), COLS, valores(entidad));
        return entidad;
    }

    public void eliminar(int id) {
        Bd.eliminar(TABLA, COL_ID, id);
    }

    // ---------------------------------------------------------------- extras

    /** HU19: marca el usuario como activo (true) o desactivado (false). */
    public void cambiarActivo(int id, boolean activo) {
        Bd.ejecutar("UPDATE " + TABLA + " SET activo = ? WHERE " + COL_ID + " = ?", activo ? 1 : 0, id);
    }

    public boolean estaActivo(int id) {
        return Bd.entero("SELECT COUNT(*) FROM " + TABLA + " WHERE " + COL_ID + " = ? AND activo = 1", id) > 0;
    }
}
