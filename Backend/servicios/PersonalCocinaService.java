package servicios;

import java.util.List;
import dao.PedidoDAO;
import dao.PersonalCocinaDAO;
import entidades.PersonalCocina;
import util.Constantes;
import util.Seguridad;
import util.Validar;

/**
 * Gestión de los usuarios con rol cocina (tabla PersonalCocina).
 * Los usuarios desactivados (HU19) conservan su historial pero no pueden ingresar.
 */
public class PersonalCocinaService {
    private final PersonalCocinaDAO dao = new PersonalCocinaDAO();

    public PersonalCocina crear(PersonalCocina entidad) {
        validar(entidad, 0);
        proteger(entidad);
        return dao.crear(entidad);
    }

    public PersonalCocina obtenerPorId(int id_cocina) {
        return dao.obtenerPorId(id_cocina);
    }

    public List<PersonalCocina> listar() {
        return dao.obtenerTodos();
    }

    public PersonalCocina actualizar(PersonalCocina entidad) {
        Validar.noNulo(entidad, "PersonalCocina");
        PersonalCocina existente = obtenerPorId(entidad.getId_cocina());
        if (existente == null) {
            throw new IllegalArgumentException("No existe un usuario de cocina con id " + entidad.getId_cocina());
        }
        // Si no se envía contraseña nueva se conserva la actual.
        if (entidad.getContrasena() == null || entidad.getContrasena().trim().isEmpty()) {
            entidad.setContrasena(existente.getContrasena());
        }
        validar(entidad, entidad.getId_cocina());
        proteger(entidad);
        return dao.actualizar(entidad);
    }

    public void eliminar(int id_cocina) {
        if (obtenerPorId(id_cocina) == null) {
            return;
        }
        if (new PedidoDAO().contarPorCocina(id_cocina) > 0) {
            throw new IllegalStateException("El usuario de cocina tiene pedidos asociados; desactívelo para conservar su historial");
        }
        dao.eliminar(id_cocina);
    }

    // ---------------------------------------------------------------- extras

    /** HU19: activa un usuario. No puede haber dos usuarios activos con el mismo usuario. */
    public void activar(int id_cocina) {
        PersonalCocina u = obtenerPorId(id_cocina);
        if (u == null) {
            throw new IllegalArgumentException("No existe un usuario de cocina con id " + id_cocina);
        }
        if (AutenticacionService.usuarioEnUso(u.getUsuario(), Constantes.ROL_COCINA, id_cocina)) {
            throw new IllegalStateException("Ya existe otro usuario activo con el usuario '" + u.getUsuario() + "'");
        }
        dao.cambiarActivo(id_cocina, true);
    }

    /** HU19: desactiva un usuario conservando su historial de pedidos. */
    public void desactivar(int id_cocina) {
        if (obtenerPorId(id_cocina) == null) {
            throw new IllegalArgumentException("No existe un usuario de cocina con id " + id_cocina);
        }
        dao.cambiarActivo(id_cocina, false);
    }

    public boolean estaActivo(int id_cocina) {
        return dao.estaActivo(id_cocina);
    }

    // --------------------------------------------------------------- privados

    private void validar(PersonalCocina u, int idPropio) {
        Validar.noNulo(u, "PersonalCocina");
        u.setNombre(Validar.texto(u.getNombre(), "nombre"));
        u.setUsuario(Validar.texto(u.getUsuario(), "usuario"));
        Validar.texto(u.getContrasena(), "contrasena");
        if (AutenticacionService.usuarioEnUso(u.getUsuario(), Constantes.ROL_COCINA, idPropio)) {
            throw new IllegalArgumentException("El usuario '" + u.getUsuario() + "' ya está en uso");
        }
    }

    /** RNF-06: nunca se almacena la contraseña en texto plano. */
    private void proteger(PersonalCocina u) {
        if (!Seguridad.estaProtegida(u.getContrasena())) {
            u.setContrasena(Seguridad.proteger(u.getContrasena()));
        }
    }
}
