package dao;

import java.util.List;
import util.Bd;
import util.CambioEstado;
import util.Fechas;

/**
 * Capa de acceso a datos (DAO) de la tabla HistorialEstado (fecha y hora de cada
 * cambio de estado de un pedido, RF-15 / RNF-09).
 */
public class HistorialEstadoDAO {

    public void registrar(int id_pedido, String estado, String fecha) {
        Bd.ejecutar("INSERT INTO HistorialEstado (id_pedido, estado, fecha) VALUES (?, ?, ?)",
                id_pedido, estado, fecha);
    }

    public List<CambioEstado> obtenerPorPedido(int id_pedido) {
        return Bd.consultar("SELECT * FROM HistorialEstado WHERE id_pedido = ? ORDER BY id_historial",
                rs -> new CambioEstado(rs.getInt("id_pedido"), rs.getString("estado"),
                        Fechas.parsear(rs.getString("fecha"))), id_pedido);
    }

    public void eliminarPorPedido(int id_pedido) {
        Bd.ejecutar("DELETE FROM HistorialEstado WHERE id_pedido = ?", id_pedido);
    }
}
