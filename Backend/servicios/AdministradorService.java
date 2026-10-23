package servicios;

import java.util.List;
import dao.AdministradorDAO;
import dao.CategoriaDAO;
import dao.MesaDAO;
import dao.PedidoDAO;
import dao.ProductoDAO;
import entidades.Administrador;
import util.Constantes;
import util.Seguridad;
import util.Validar;

/**
 * Gestión de los usuarios con rol administrador (tabla Administrador).
 * Los usuarios desactivados (HU19) conservan su historial pero no pueden ingresar.
 */
public class AdministradorService {
    private final AdministradorDAO dao = new AdministradorDAO();

    public Administrador crear(Administrador entidad) {
        validar(entidad, 0);
        proteger(entidad);
        return dao.crear(entidad);
    }

    public Administrador obtenerPorId(int id_administrador) {
        return dao.obtenerPorId(id_administrador);
    }

    public List<Administrador> listar() {
        return dao.obtenerTodos();
    }

    public Administrador actualizar(Administrador entidad) {
        Validar.noNulo(entidad, "Administrador");
        Administrador existente = obtenerPorId(entidad.getId_administrador());
        if (existente == null) {
            throw new IllegalArgumentException("No existe un administrador con id " + entidad.getId_administrador());
        }
        // Si no se envía contraseña nueva se conserva la actual.
        if (entidad.getContrasena() == null || entidad.getContrasena().trim().isEmpty()) {
            entidad.setContrasena(existente.getContrasena());
        }
        validar(entidad, entidad.getId_administrador());
        proteger(entidad);
        return dao.actualizar(entidad);
    }

    public void eliminar(int id_administrador) {
        if (obtenerPorId(id_administrador) == null) {
            return;
        }
        int referencias = new MesaDAO().contarPorAdministrador(id_administrador)
                + new CategoriaDAO().contarPorAdministrador(id_administrador)
                + new ProductoDAO().contarPorAdministrador(id_administrador)
                + new PedidoDAO().contarPorAdministrador(id_administrador);
        if (referencias > 0) {
            throw new IllegalStateException("El administrador tiene registros asociados (mesas, categorías, productos o pedidos); desactívelo en lugar de eliminarlo");
        }
        dao.eliminar(id_administrador);
    }

    // ---------------------------------------------------------------- extras

    /** HU19: activa un usuario. No puede haber dos usuarios activos con el mismo usuario. */
    public void activar(int id_administrador) {
        Administrador u = obtenerPorId(id_administrador);
        if (u == null) {
            throw new IllegalArgumentException("No existe un administrador con id " + id_administrador);
        }
        if (AutenticacionService.usuarioEnUso(u.getUsuario(), Constantes.ROL_ADMINISTRADOR, id_administrador)) {
            throw new IllegalStateException("Ya existe otro usuario activo con el usuario '" + u.getUsuario() + "'");
        }
        dao.cambiarActivo(id_administrador, true);
    }

    /** HU19: desactiva un usuario conservando su historial de pedidos. */
    public void desactivar(int id_administrador) {
        if (obtenerPorId(id_administrador) == null) {
            throw new IllegalArgumentException("No existe un administrador con id " + id_administrador);
        }
        dao.cambiarActivo(id_administrador, false);
    }

    public boolean estaActivo(int id_administrador) {
        return dao.estaActivo(id_administrador);
    }

    /**
     * HU8: asigna un único rol (mesero, cocina o administrador) a un usuario.
     * El registro anterior queda desactivado (conserva su historial) y se crea
     * el usuario en el nuevo rol con las mismas credenciales.
     */
    public void asignarRol(int id_administradorQueEjecuta, String usuario, String nuevoRol) {
        if (!estaActivo(id_administradorQueEjecuta)) {
            throw new IllegalArgumentException("Solo un administrador activo puede asignar roles");
        }
        AutenticacionService.Sesion actual = AutenticacionService.buscarActivo(usuario);
        if (actual == null) {
            throw new IllegalArgumentException("No existe un usuario activo '" + usuario + "'");
        }
        if (actual.getRol().equals(nuevoRol)) {
            throw new IllegalArgumentException("El usuario ya tiene el rol " + nuevoRol);
        }
        String nombre = actual.getNombre();
        String hash = actual.getContrasenaProtegida();

        // Desactivar el registro anterior
        switch (actual.getRol()) {
            case Constantes.ROL_MESERO: new MeseroService().desactivar(actual.getId()); break;
            case Constantes.ROL_COCINA: new PersonalCocinaService().desactivar(actual.getId()); break;
            default: new AdministradorService().desactivar(actual.getId());
        }
        try {
            switch (nuevoRol) {
                case Constantes.ROL_MESERO: {
                    entidades.Mesero m = new entidades.Mesero();
                    m.setNombre(nombre); m.setUsuario(usuario); m.setContrasena(hash);
                    new MeseroService().crear(m); break;
                }
                case Constantes.ROL_COCINA: {
                    entidades.PersonalCocina c = new entidades.PersonalCocina();
                    c.setNombre(nombre); c.setUsuario(usuario); c.setContrasena(hash);
                    new PersonalCocinaService().crear(c); break;
                }
                case Constantes.ROL_ADMINISTRADOR: {
                    entidades.Administrador a = new entidades.Administrador();
                    a.setNombre(nombre); a.setUsuario(usuario); a.setContrasena(hash);
                    new AdministradorService().crear(a); break;
                }
                default: throw new IllegalArgumentException("Rol inválido: " + nuevoRol);
            }
        } catch (RuntimeException e) {
            // revertir
            switch (actual.getRol()) {
                case Constantes.ROL_MESERO: new MeseroService().activar(actual.getId()); break;
                case Constantes.ROL_COCINA: new PersonalCocinaService().activar(actual.getId()); break;
                default: new AdministradorService().activar(actual.getId());
            }
            throw e;
        }
    }

    // --------------------------------------------------------------- privados

    private void validar(Administrador u, int idPropio) {
        Validar.noNulo(u, "Administrador");
        u.setNombre(Validar.texto(u.getNombre(), "nombre"));
        u.setUsuario(Validar.texto(u.getUsuario(), "usuario"));
        Validar.texto(u.getContrasena(), "contrasena");
        if (AutenticacionService.usuarioEnUso(u.getUsuario(), Constantes.ROL_ADMINISTRADOR, idPropio)) {
            throw new IllegalArgumentException("El usuario '" + u.getUsuario() + "' ya está en uso");
        }
    }

    /** RNF-06: nunca se almacena la contraseña en texto plano. */
    private void proteger(Administrador u) {
        if (!Seguridad.estaProtegida(u.getContrasena())) {
            u.setContrasena(Seguridad.proteger(u.getContrasena()));
        }
    }
}
