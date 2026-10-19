package servicios;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import entidades.Mesero;
import util.Bd;
import util.Constantes;
import util.Seguridad;
import util.Validar;

/**
 * Gestión de los usuarios con rol mesero (tabla Mesero).
 * Los usuarios desactivados (HU19) conservan su historial pero no pueden ingresar.
 */
public class MeseroService {
    private static final String TABLA = "Mesero";
    private static final String COL_ID = "id_mesero";
    private static final String[] COLS = {
        "nombre", "usuario", "contrasena", "tipo_documento", 
        "numero_documento", "correo", "telefono", "direccion", "estado"
    };

    private static Mesero mapear(ResultSet rs) throws SQLException {
        Mesero u = new Mesero();
        u.setId_mesero(rs.getInt("id_mesero"));
        u.setNombre(rs.getString("nombre"));
        u.setUsuario(rs.getString("usuario"));
        u.setContrasena(rs.getString("contrasena"));
        u.setTipo_documento(rs.getString("tipo_documento"));
        u.setNumero_documento(rs.getString("numero_documento"));
        u.setCorreo(rs.getString("correo"));
        u.setTelefono(rs.getString("telefono"));
        u.setDireccion(rs.getString("direccion"));
        u.setEstado(rs.getBoolean("estado"));
        return u;
    }

    private static Object[] valores(Mesero u) {
        return new Object[] {
            u.getNombre(), 
            u.getUsuario(), 
            u.getContrasena(),
            u.getTipo_documento(),
            u.getNumero_documento(),
            u.getCorreo(),
            u.getTelefono(),
            u.getDireccion(),
            u.isEstado()
        };
    }

    public Mesero crear(Mesero entidad) {
        validar(entidad, 0);
        proteger(entidad);
        entidad.setId_mesero(Bd.insertar(TABLA, COL_ID, entidad.getId_mesero(), COLS, valores(entidad)));
        return entidad;
    }

    public Mesero obtenerPorId(int id_mesero) {
        return Bd.uno("SELECT * FROM " + TABLA + " WHERE " + COL_ID + " = ?", MeseroService::mapear, id_mesero);
    }

    public List<Mesero> listar() {
        return Bd.consultar("SELECT * FROM " + TABLA + " ORDER BY " + COL_ID, MeseroService::mapear);
    }

    public Mesero actualizar(Mesero entidad) {
        Validar.noNulo(entidad, "Mesero");
        Mesero existente = obtenerPorId(entidad.getId_mesero());
        if (existente == null) {
            throw new IllegalArgumentException("No existe un mesero con id " + entidad.getId_mesero());
        }
        if (entidad.getContrasena() == null || entidad.getContrasena().trim().isEmpty()) {
            entidad.setContrasena(existente.getContrasena());
        }
        validar(entidad, entidad.getId_mesero());
        proteger(entidad);
        Bd.actualizar(TABLA, COL_ID, entidad.getId_mesero(), COLS, valores(entidad));
        return entidad;
    }

    public void eliminar(int id_mesero) {
        if (obtenerPorId(id_mesero) == null) {
            return;
        }
        if (Bd.entero("SELECT COUNT(*) FROM Pedido WHERE id_mesero = ?", id_mesero) > 0) {
            throw new IllegalStateException("El mesero tiene pedidos asociados; desactívelo para conservar su historial");
        }
        Bd.eliminar(TABLA, COL_ID, id_mesero);
    }



    /** HU19: activa un usuario. No puede haber dos usuarios activos con el mismo usuario. */
    public void activar(int id_mesero) {
        Mesero u = obtenerPorId(id_mesero);
        if (u == null) {
            throw new IllegalArgumentException("No existe un mesero con id " + id_mesero);
        }
        if (AutenticacionService.usuarioEnUso(u.getUsuario(), Constantes.ROL_MESERO, id_mesero)) {
            throw new IllegalStateException("Ya existe otro usuario activo con el usuario '" + u.getUsuario() + "'");
        }
        Bd.ejecutar("UPDATE " + TABLA + " SET estado = 1 WHERE " + COL_ID + " = ?", id_mesero);
    }

    /** HU19: desactiva un usuario conservando su historial de pedidos. */
    public void desactivar(int id_mesero) {
        if (obtenerPorId(id_mesero) == null) {
            throw new IllegalArgumentException("No existe un mesero con id " + id_mesero);
        }
        Bd.ejecutar("UPDATE " + TABLA + " SET estado = 0 WHERE " + COL_ID + " = ?", id_mesero);
    }

    public boolean estaActivo(int id_mesero) {
        return Bd.entero("SELECT COUNT(*) FROM " + TABLA + " WHERE " + COL_ID + " = ? AND estado = 1", id_mesero) > 0;
    }

    
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