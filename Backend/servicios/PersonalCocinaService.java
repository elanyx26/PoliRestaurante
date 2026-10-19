package servicios;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import entidades.PersonalCocina;
import util.Bd;
import util.Constantes;
import util.Seguridad;
import util.Validar;

/**
 * Gestión de los usuarios con rol cocina (tabla PersonalCocina).
 * Los usuarios desactivados (HU19) conservan su historial pero no pueden ingresar.
 */
public class PersonalCocinaService {
    private static final String TABLA = "PersonalCocina";
    private static final String COL_ID = "id_cocina";
    
    //  Añadimos todas las columnas reales de la base de datos aquí:
    private static final String[] COLS = {
        "nombre", "usuario", "contrasena", 
        "tipo_documento", "numero_documento", "correo", "telefono", "direccion", "activo"
    };

    private static PersonalCocina mapear(ResultSet rs) throws SQLException {
        PersonalCocina u = new PersonalCocina();
        u.setId_cocina(rs.getInt("id_cocina"));
        u.setNombre(rs.getString("nombre"));
        u.setUsuario(rs.getString("usuario"));
        u.setContrasena(rs.getString("contrasena"));
        
        // 2. Mapeamos también estos campos para que no salgan en null:
        u.setTipo_documento(rs.getString("tipo_documento"));
        u.setNumero_documento(rs.getString("numero_documento"));
        u.setCorreo(rs.getString("correo"));
        u.setTelefono(rs.getString("telefono"));
        u.setDireccion(rs.getString("direccion"));
        u.setEstado(rs.getBoolean("activo")); 
        
        return u;
    }

    private static Object[] valores(PersonalCocina u) {
        // 3. Devolvemos los valores en el mismo orden exacto del arreglo COLS:
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

    public PersonalCocina crear(PersonalCocina entidad) {
        validar(entidad, 0);
        proteger(entidad);
        entidad.setId_cocina(Bd.insertar(TABLA, COL_ID, entidad.getId_cocina(), COLS, valores(entidad)));
        return entidad;
    }

    public PersonalCocina obtenerPorId(int id_cocina) {
        return Bd.uno("SELECT * FROM " + TABLA + " WHERE " + COL_ID + " = ?", PersonalCocinaService::mapear, id_cocina);
    }

    public List<PersonalCocina> listar() {
        return Bd.consultar("SELECT * FROM " + TABLA + " ORDER BY " + COL_ID, PersonalCocinaService::mapear);
    }

    public PersonalCocina actualizar(PersonalCocina entidad) {
        Validar.noNulo(entidad, "PersonalCocina");
        PersonalCocina existente = obtenerPorId(entidad.getId_cocina());
        if (existente == null) {
            throw new IllegalArgumentException("No existe un usuario de cocina con id " + entidad.getId_cocina());
        }
        if (entidad.getContrasena() == null || entidad.getContrasena().trim().isEmpty()) {
            entidad.setContrasena(existente.getContrasena());
        }
        validar(entidad, entidad.getId_cocina());
        proteger(entidad);
        Bd.actualizar(TABLA, COL_ID, entidad.getId_cocina(), COLS, valores(entidad));
        return entidad;
    }

    public void eliminar(int id_cocina) {
        if (obtenerPorId(id_cocina) == null) {
            return;
        }
        if (Bd.entero("SELECT COUNT(*) FROM Pedido WHERE id_cocina = ?", id_cocina) > 0) {
            throw new IllegalStateException("El usuario de cocina tiene pedidos asociados; desactívelo para conservar su historial");
        }
        Bd.eliminar(TABLA, COL_ID, id_cocina);
    }



    public void activar(int id_cocina) {
        PersonalCocina u = obtenerPorId(id_cocina);
        if (u == null) {
            throw new IllegalArgumentException("No existe un usuario de cocina con id " + id_cocina);
        }
        if (AutenticacionService.usuarioEnUso(u.getUsuario(), Constantes.ROL_COCINA, id_cocina)) {
            throw new IllegalStateException("Ya existe otro usuario activo con el usuario '" + u.getUsuario() + "'");
        }
        Bd.ejecutar("UPDATE " + TABLA + " SET activo = 1 WHERE " + COL_ID + " = ?", id_cocina);
    }

    public void desactivar(int id_cocina) {
        if (obtenerPorId(id_cocina) == null) {
            throw new IllegalArgumentException("No existe un usuario de cocina con id " + id_cocina);
        }
        Bd.ejecutar("UPDATE " + TABLA + " SET activo = 0 WHERE " + COL_ID + " = ?", id_cocina);
    }

    public boolean estaActivo(int id_cocina) {
        return Bd.entero("SELECT COUNT(*) FROM " + TABLA + " WHERE " + COL_ID + " = ? AND activo = 1", id_cocina) > 0;
    }


    private void validar(PersonalCocina u, int idPropio) {
        Validar.noNulo(u, "PersonalCocina");
        u.setNombre(Validar.texto(u.getNombre(), "nombre"));
        u.setUsuario(Validar.texto(u.getUsuario(), "usuario"));
        Validar.texto(u.getContrasena(), "contrasena");
        if (AutenticacionService.usuarioEnUso(u.getUsuario(), Constantes.ROL_COCINA, idPropio)) {
            throw new IllegalArgumentException("El usuario '" + u.getUsuario() + "' ya está en uso");
        }
    }

    private void proteger(PersonalCocina u) {
        if (!Seguridad.estaProtegida(u.getContrasena())) {
            u.setContrasena(Seguridad.proteger(u.getContrasena()));
        }
    }
}