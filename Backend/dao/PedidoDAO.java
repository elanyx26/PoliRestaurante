package dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import entidades.Pedido;
import util.Bd;
import util.Constantes;

/**
 * Capa de acceso a datos (DAO) de la tabla Pedido.
 * Solo contiene SQL y mapeo de filas a objetos; el ciclo de vida del pedido
 * (estados, validaciones, mesa, total) vive en PedidoService.
 */
public class PedidoDAO {
    private static final String TABLA = "Pedido";
    private static final String COL_ID = "id_pedido";
    private static final String[] COLS = {"id_mesero", "id_cocina", "id_administrador", "id_mesa",
            "tipo_pedido", "nombre_cliente", "estado", "total", "metodo_pago", "fecha_creacion", "fecha_cierre"};

    /** Estados en los que el pedido todavía está en curso (no entregado, cancelado ni cerrado). */
    private static final String EN_CURSO = "('" + Constantes.PENDIENTE + "','" + Constantes.EN_PREPARACION
            + "','" + Constantes.LISTO + "')";

    private static Pedido mapear(ResultSet rs) throws SQLException {
        Pedido p = new Pedido();
        p.setId_pedido(rs.getInt("id_pedido"));
        p.setId_mesero(rs.getInt("id_mesero"));
        p.setId_cocina(rs.getInt("id_cocina"));              // NULL -> 0
        p.setId_administrador(rs.getInt("id_administrador"));
        p.setId_mesa(rs.getInt("id_mesa"));
        p.setTipo_pedido(rs.getString("tipo_pedido"));
        p.setNombre_cliente(rs.getString("nombre_cliente"));
        p.setEstado(rs.getString("estado"));
        p.setTotal(rs.getInt("total"));
        p.setMetodo_pago(rs.getString("metodo_pago"));
        p.setFecha_creacion(rs.getString("fecha_creacion"));
        p.setFecha_cierre(rs.getString("fecha_cierre"));
        return p;
    }

    private static Object[] valores(Pedido p) {
        return new Object[] {Bd.fk(p.getId_mesero()), Bd.fk(p.getId_cocina()), Bd.fk(p.getId_administrador()),
                Bd.fk(p.getId_mesa()), p.getTipo_pedido(), p.getNombre_cliente(), p.getEstado(), p.getTotal(),
                p.getMetodo_pago(), p.getFecha_creacion(), p.getFecha_cierre()};
    }

    /** INSERT. Si la entidad trae id > 0 se usa ese id; si no, lo genera AUTO_INCREMENT. */
    public Pedido crear(Pedido entidad) {
        entidad.setId_pedido(Bd.insertar(TABLA, COL_ID, entidad.getId_pedido(), COLS, valores(entidad)));
        return entidad;
    }

    /** Lee el pedido sin cargar sus objetos relacionados; null si no existe. */
    public Pedido obtenerPorId(int id_pedido) {
        return Bd.uno("SELECT * FROM Pedido WHERE id_pedido = ?", PedidoDAO::mapear, id_pedido);
    }

    public List<Pedido> obtenerTodos() {
        return Bd.consultar("SELECT * FROM Pedido ORDER BY id_pedido", PedidoDAO::mapear);
    }

    public Pedido actualizar(Pedido entidad) {
        Bd.actualizar(TABLA, COL_ID, entidad.getId_pedido(), COLS, valores(entidad));
        return entidad;
    }

    public void eliminar(int id_pedido) {
        Bd.eliminar(TABLA, COL_ID, id_pedido);
    }

    // ---------------------------------------------------- actualizaciones puntuales

    public void actualizarNombreCliente(int id_pedido, String nombre) {
        Bd.ejecutar("UPDATE Pedido SET nombre_cliente = ? WHERE id_pedido = ?", nombre, id_pedido);
    }

    public void actualizarEstado(int id_pedido, String estado) {
        Bd.ejecutar("UPDATE Pedido SET estado = ? WHERE id_pedido = ?", estado, id_pedido);
    }

    public void actualizarTotal(int id_pedido, int total) {
        Bd.ejecutar("UPDATE Pedido SET total = ? WHERE id_pedido = ?", total, id_pedido);
    }

    public void asignarCocina(int id_pedido, int id_cocina) {
        Bd.ejecutar("UPDATE Pedido SET id_cocina = ? WHERE id_pedido = ?", id_cocina, id_pedido);
    }

    public void marcarEnviadoACocina(int id_pedido) {
        Bd.ejecutar("UPDATE Pedido SET enviado_cocina = 1 WHERE id_pedido = ?", id_pedido);
    }

    public boolean estaEnviadoACocina(int id_pedido) {
        return Bd.entero("SELECT enviado_cocina FROM Pedido WHERE id_pedido = ?", id_pedido) == 1;
    }

    public void registrarCierre(int id_pedido, String metodo_pago, String fecha_cierre) {
        Bd.ejecutar("UPDATE Pedido SET metodo_pago = ?, fecha_cierre = ? WHERE id_pedido = ?",
                metodo_pago, fecha_cierre, id_pedido);
    }

    // ---------------------------------------------------------------- consultas

    /** Cola de cocina: pedidos en ese estado y ya enviados a cocina, del más antiguo al más reciente. */
    public List<Pedido> obtenerColaCocina(String estado) {
        return Bd.consultar("SELECT * FROM Pedido WHERE estado = ? AND enviado_cocina = 1 "
                + "ORDER BY fecha_creacion, id_pedido", PedidoDAO::mapear, estado);
    }

    /** Pedidos del mesero que siguen en curso. */
    public List<Pedido> obtenerEnCursoDeMesero(int id_mesero) {
        return Bd.consultar("SELECT * FROM Pedido WHERE id_mesero = ? AND estado IN " + EN_CURSO
                + " ORDER BY fecha_creacion, id_pedido", PedidoDAO::mapear, id_mesero);
    }

    /** Pedidos de la mesa, del más reciente al más antiguo. */
    public List<Pedido> obtenerPorMesa(int id_mesa) {
        return Bd.consultar("SELECT * FROM Pedido WHERE id_mesa = ? ORDER BY fecha_creacion DESC, id_pedido DESC",
                PedidoDAO::mapear, id_mesa);
    }

    public List<Pedido> obtenerPorEstado(String estado) {
        return Bd.consultar("SELECT * FROM Pedido WHERE estado = ?", PedidoDAO::mapear, estado);
    }

    /** Cantidad de pedidos de la mesa que siguen en curso. */
    public int contarEnCursoPorMesa(int id_mesa) {
        return Bd.entero("SELECT COUNT(*) FROM Pedido WHERE id_mesa = ? AND estado IN " + EN_CURSO, id_mesa);
    }

    public int contarPorMesero(int id_mesero) {
        return Bd.entero("SELECT COUNT(*) FROM Pedido WHERE id_mesero = ?", id_mesero);
    }

    public int contarPorCocina(int id_cocina) {
        return Bd.entero("SELECT COUNT(*) FROM Pedido WHERE id_cocina = ?", id_cocina);
    }

    public int contarPorMesa(int id_mesa) {
        return Bd.entero("SELECT COUNT(*) FROM Pedido WHERE id_mesa = ?", id_mesa);
    }

    public int contarPorAdministrador(int id_administrador) {
        return Bd.entero("SELECT COUNT(*) FROM Pedido WHERE id_administrador = ?", id_administrador);
    }
}
