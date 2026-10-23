package dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import entidades.Domicilio;
import util.Bd;

/**
 * Capa de acceso a datos (DAO) de la tabla Domicilio (relación 1 a 1 con Pedido).
 * Solo contiene SQL y mapeo de filas a objetos; las reglas de negocio
 * (pedido a domicilio, teléfono válido, envío modificable) viven en DomicilioService.
 */
public class DomicilioDAO {
    private static final String TABLA = "Domicilio";
    private static final String COL_ID = "id_domicilio";
    private static final String[] COLS = {"id_pedido", "direccion", "telefono", "costo_envio"};

    private static Domicilio mapear(ResultSet rs) throws SQLException {
        Domicilio d = new Domicilio();
        d.setId_domicilio(rs.getInt("id_domicilio"));
        d.setId_pedido(rs.getInt("id_pedido"));
        d.setDireccion(rs.getString("direccion"));
        d.setTelefono(rs.getString("telefono"));
        d.setCosto_envio(rs.getInt("costo_envio"));
        return d;
    }

    private static Object[] valores(Domicilio d) {
        return new Object[] {d.getId_pedido(), d.getDireccion(), d.getTelefono(), d.getCosto_envio()};
    }

    /** INSERT. Si la entidad trae id > 0 se usa ese id; si no, lo genera AUTO_INCREMENT. */
    public Domicilio crear(Domicilio entidad) {
        entidad.setId_domicilio(Bd.insertar(TABLA, COL_ID, entidad.getId_domicilio(), COLS, valores(entidad)));
        return entidad;
    }

    /** Devuelve el domicilio o null si no existe. */
    public Domicilio obtenerPorId(int id_domicilio) {
        return Bd.uno("SELECT * FROM Domicilio WHERE id_domicilio = ?", DomicilioDAO::mapear, id_domicilio);
    }

    public List<Domicilio> obtenerTodos() {
        return Bd.consultar("SELECT * FROM Domicilio ORDER BY id_domicilio", DomicilioDAO::mapear);
    }

    public Domicilio actualizar(Domicilio entidad) {
        Bd.actualizar(TABLA, COL_ID, entidad.getId_domicilio(), COLS, valores(entidad));
        return entidad;
    }

    public void eliminar(int id_domicilio) {
        Bd.eliminar(TABLA, COL_ID, id_domicilio);
    }

    // ---------------------------------------------------------------- extras

    /** Domicilio asociado a un pedido, o null si no tiene. */
    public Domicilio obtenerPorPedido(int id_pedido) {
        return Bd.uno("SELECT * FROM Domicilio WHERE id_pedido = ?", DomicilioDAO::mapear, id_pedido);
    }

    public void eliminarPorPedido(int id_pedido) {
        Bd.ejecutar("DELETE FROM Domicilio WHERE id_pedido = ?", id_pedido);
    }

    /** Suma del costo de envío del pedido (0 si no es a domicilio). */
    public int sumarCostoEnvio(int id_pedido) {
        return Bd.entero("SELECT COALESCE(SUM(costo_envio), 0) FROM Domicilio WHERE id_pedido = ?", id_pedido);
    }
}
