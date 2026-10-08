package servicios;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import entidades.Mesa;
import entidades.Pedido;
import util.Bd;
import util.Constantes;
import util.Validar;

/** HU6, HU10, HU12 / RF-11: gestión de mesas (tabla Mesa). */
public class MesaService {
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

    public Mesa crear(Mesa entidad) {
        validar(entidad, 0);
        entidad.setId_mesa(Bd.insertar("Mesa", "id_mesa", entidad.getId_mesa(), COLS, valores(entidad)));
        hidratar(entidad);
        return entidad;
    }

    public Mesa obtenerPorId(int id_mesa) {
        Mesa m = Bd.uno("SELECT * FROM Mesa WHERE id_mesa = ?", MesaService::mapear, id_mesa);
        if (m != null) {
            hidratar(m);
        }
        return m;
    }

    public List<Mesa> listar() {
        return Bd.consultar("SELECT * FROM Mesa ORDER BY numero", MesaService::mapear);
    }

    public Mesa actualizar(Mesa entidad) {
        Validar.noNulo(entidad, "Mesa");
        Mesa existente = obtenerPorId(entidad.getId_mesa());
        if (existente == null) {
            throw new IllegalArgumentException("No existe una mesa con id " + entidad.getId_mesa());
        }
        validar(entidad, entidad.getId_mesa());
        // Pasar a Libre solo si no hay pedidos en curso en la mesa
        if (Constantes.MESA_LIBRE.equals(entidad.getEstado())
                && Constantes.MESA_OCUPADA.equals(existente.getEstado())) {
            verificarSinPedidosEnCurso(entidad.getId_mesa());
        }
        Bd.actualizar("Mesa", "id_mesa", entidad.getId_mesa(), COLS, valores(entidad));
        hidratar(entidad);
        return entidad;
    }

    public void eliminar(int id_mesa) {
        Mesa m = obtenerPorId(id_mesa);
        if (m == null) {
            return;
        }
        if (Constantes.MESA_OCUPADA.equals(m.getEstado())) {
            throw new IllegalStateException("No se puede eliminar una mesa ocupada");
        }
        if (Bd.entero("SELECT COUNT(*) FROM Pedido WHERE id_mesa = ?", id_mesa) > 0) {
            throw new IllegalStateException("La mesa tiene historial de pedidos y no se puede eliminar");
        }
        Bd.eliminar("Mesa", "id_mesa", id_mesa);
    }

    // ---------------------------------------------------------------- extras

    /** HU6: cambia el estado entre "Libre" y "Ocupada". */
    public Mesa cambiarEstado(int id_mesa, String estado) {
        Mesa m = obtenerPorId(id_mesa);
        if (m == null) {
            throw new IllegalArgumentException("No existe una mesa con id " + id_mesa);
        }
        validarEstado(estado);
        if (Constantes.MESA_LIBRE.equals(estado) && Constantes.MESA_OCUPADA.equals(m.getEstado())) {
            verificarSinPedidosEnCurso(id_mesa);
        }
        Bd.ejecutar("UPDATE Mesa SET estado = ? WHERE id_mesa = ?", estado, id_mesa);
        m.setEstado(estado);
        return m;
    }

    /** Mesas que un mesero puede seleccionar para un nuevo pedido (estado "Libre"). */
    public List<Mesa> listarLibres() { return filtrar(Constantes.MESA_LIBRE); }

    public List<Mesa> listarOcupadas() { return filtrar(Constantes.MESA_OCUPADA); }

    /** HU12: cantidad de mesas libres. */
    public int contarLibres() {
        return Bd.entero("SELECT COUNT(*) FROM Mesa WHERE estado = ?", Constantes.MESA_LIBRE);
    }

    /** HU12: cantidad de mesas ocupadas. */
    public int contarOcupadas() {
        return Bd.entero("SELECT COUNT(*) FROM Mesa WHERE estado = ?", Constantes.MESA_OCUPADA);
    }

    /** HU10: historial de pedidos de una mesa, del más reciente al más antiguo. */
    public List<Pedido> historialDePedidos(int id_mesa) {
        return new PedidoService().historialDeMesa(id_mesa);
    }

    // --------------------------------------------------------------- privados

    private List<Mesa> filtrar(String estado) {
        return Bd.consultar("SELECT * FROM Mesa WHERE estado = ? ORDER BY numero", MesaService::mapear, estado);
    }

    private void verificarSinPedidosEnCurso(int id_mesa) {
        if (new PedidoService().tienePedidoEnCurso(id_mesa)) {
            throw new IllegalStateException("La mesa tiene un pedido en curso; no se puede liberar");
        }
    }

    private void validar(Mesa m, int idPropio) {
        Validar.noNulo(m, "Mesa");
        Validar.positivo(m.getNumero(), "numero");
        if (m.getEstado() == null || m.getEstado().trim().isEmpty()) {
            m.setEstado(Constantes.MESA_LIBRE);
        }
        validarEstado(m.getEstado());
        if (Bd.entero("SELECT COUNT(*) FROM Mesa WHERE numero = ? AND id_mesa <> ?", m.getNumero(), idPropio) > 0) {
            throw new IllegalArgumentException("Ya existe una mesa con el número " + m.getNumero());
        }
        if (new AdministradorService().obtenerPorId(m.getId_administrador()) == null) {
            throw new IllegalArgumentException("El administrador indicado no existe");
        }
    }

    private void validarEstado(String estado) {
        if (!Constantes.MESA_LIBRE.equals(estado) && !Constantes.MESA_OCUPADA.equals(estado)) {
            throw new IllegalArgumentException("El estado de la mesa solo puede ser 'Libre' u 'Ocupada'");
        }
    }

    private void hidratar(Mesa m) {
        m.setAdministrador(new AdministradorService().obtenerPorId(m.getId_administrador()));
    }
}
