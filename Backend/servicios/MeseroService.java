package servicios;

import java.util.List;
import dao.MeseroDAO;
import dao.PedidoDAO;
import entidades.Mesero;
import util.Constantes;
import util.Seguridad;
import util.Validar;

/**
 * Gestión de los usuarios con rol mesero (tabla Mesero).
 * Los usuarios desactivados (HU19) conservan su historial pero no pueden ingresar.
 */
public class MeseroService {
    private final MeseroDAO dao = new MeseroDAO();

    public Mesero crear(Mesero entidad) {
        validar(entidad, 0);
        proteger(entidad);
        return dao.crear(entidad);
    }

    public Mesero obtenerPorId(int id_mesero) {
        return dao.obtenerPorId(id_mesero);
    }

    public List<Mesero> listar() {
        return dao.obtenerTodos();
    }

    public Mesero actualizar(Mesero entidad) {
        Validar.noNulo(entidad, "Mesero");
        Mesero existente = obtenerPorId(entidad.getId_mesero());
        if (existente == null) {
            throw new IllegalArgumentException("No existe un mesero con id " + entidad.getId_mesero());
        }
        // Si no se envía contraseña nueva se conserva la actual.
        if (entidad.getContrasena() == null || entidad.getContrasena().trim().isEmpty()) {
            entidad.setContrasena(existente.getContrasena());
        }
        validar(entidad, entidad.getId_mesero());
        proteger(entidad);
        return dao.actualizar(entidad);
    }

    public void eliminar(int id_mesero) {
        if (obtenerPorId(id_mesero) == null) {
            return;
        }
        if (new PedidoDAO().contarPorMesero(id_mesero) > 0) {
            throw new IllegalStateException("El mesero tiene pedidos asociados; desactívelo para conservar su historial");
        }
        dao.eliminar(id_mesero);
    }

    // ---------------------------------------------------------------- extras

    /** HU19: activa un usuario. No puede haber dos usuarios activos con el mismo usuario. */
    public void activar(int id_mesero) {
        Mesero u = obtenerPorId(id_mesero);
        if (u == null) {
            throw new IllegalArgumentException("No existe un mesero con id " + id_mesero);
        }
        if (AutenticacionService.usuarioEnUso(u.getUsuario(), Constantes.ROL_MESERO, id_mesero)) {
            throw new IllegalStateException("Ya existe otro usuario activo con el usuario '" + u.getUsuario() + "'");
        }
        dao.cambiarActivo(id_mesero, true);
    }

    /** HU19: desactiva un usuario conservando su historial de pedidos. */
    public void desactivar(int id_mesero) {
        if (obtenerPorId(id_mesero) == null) {
            throw new IllegalArgumentException("No existe un mesero con id " + id_mesero);
        }
        dao.cambiarActivo(id_mesero, false);
    }

    public boolean estaActivo(int id_mesero) {
        return dao.estaActivo(id_mesero);
    }

    // --------------------------------------------------------------- privados

    private void validar(Mesero u, int idPropio) {
        Validar.noNulo(u, "Mesero");
        u.setNombre(Validar.texto(u.getNombre(), "nombre"));
        u.setUsuario(Validar.texto(u.getUsuario(), "usuario"));
        Validar.texto(u.getContrasena(), "contrasena");
        if (AutenticacionService.usuarioEnUso(u.getUsuario(), Constantes.ROL_MESERO, idPropio)) {
            throw new IllegalArgumentException("El usuario '" + u.getUsuario() + "' ya está en uso");
        }
    }

    /** RNF-06: nunca se almacena la contraseña en texto plano. */
    private void proteger(Mesero u) {
        if (!Seguridad.estaProtegida(u.getContrasena())) {
            u.setContrasena(Seguridad.proteger(u.getContrasena()));
        }
    }
}
