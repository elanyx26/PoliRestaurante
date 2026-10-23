package dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import entidades.Mesa;
import util.Bd;

/**
 * Capa de acceso a datos (DAO) de la tabla Mesa.
 * Solo contiene SQL y mapeo de filas a objetos; las reglas de negocio
 * (estados válidos, número único, pedidos en curso) viven en MesaService.
 */
public class MesaDAO {
    private static final String TABLA = "Mesa";
    private static final String COL_ID = "id_mesa";
    private static final String[] COLS = {"numero", "estado", "id_administrador"};

    private static Mesa mapear(ResultSet rs) throws SQLException {
        Mesa m = new Mesa();
        m.setId_mesa(rs.getInt("id_mesa"));
        m.setNumero(rs.getInt("numero"));
        m.setEstado(rs.getString("estado"));
        m.setId_administrador(rs.getInt("id_administrador"));
        return m;
    }

    private static Object[] valores(Mesa m) {
        return new Object[] {m.getNumero(), m.getEstado(), Bd.fk(m.getId_administrador())};
    }

    /** INSERT. Si la entidad trae id > 0 se usa ese id; si no, lo genera AUTO_INCREMENT. */
    public Mesa crear(Mesa entidad) {
        entidad.setId_mesa(Bd.insertar(TABLA, COL_ID, entidad.getId_mesa(), COLS, valores(entidad)));
        return entidad;
    }

    /** Devuelve la mesa o null si no existe. */
    public Mesa obtenerPorId(int id_mesa) {
        return Bd.uno("SELECT * FROM Mesa WHERE id_mesa = ?", MesaDAO::mapear, id_mesa);
    }

    public List<Mesa> obtenerTodos() {
        return Bd.consultar("SELECT * FROM Mesa ORDER BY numero", MesaDAO::mapear);
    }

    public Mesa actualizar(Mesa entidad) {
        Bd.actualizar(TABLA, COL_ID, entidad.getId_mesa(), COLS, valores(entidad));
        return entidad;
    }

    public void eliminar(int id_mesa) {
        Bd.eliminar(TABLA, COL_ID, id_mesa);
    }

    // ---------------------------------------------------------------- extras

    public void actualizarEstado(int id_mesa, String estado) {
        Bd.ejecutar("UPDATE Mesa SET estado = ? WHERE id_mesa = ?", estado, id_mesa);
    }

    public List<Mesa> obtenerPorEstado(String estado) {
        return Bd.consultar("SELECT * FROM Mesa WHERE estado = ? ORDER BY numero", MesaDAO::mapear, estado);
    }

    public int contarPorEstado(String estado) {
        return Bd.entero("SELECT COUNT(*) FROM Mesa WHERE estado = ?", estado);
    }

    /** Cantidad de mesas con ese número, sin contar la mesa indicada (para validar que sea único). */
    public int contarPorNumero(int numero, int idPropio) {
        return Bd.entero("SELECT COUNT(*) FROM Mesa WHERE numero = ? AND id_mesa <> ?", numero, idPropio);
    }

    public int contarPorAdministrador(int id_administrador) {
        return Bd.entero("SELECT COUNT(*) FROM Mesa WHERE id_administrador = ?", id_administrador);
    }
}
