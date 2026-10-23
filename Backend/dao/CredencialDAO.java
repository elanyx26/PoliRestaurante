package dao;

import java.util.ArrayList;
import java.util.List;
import util.Bd;
import util.Constantes;

/**
 * Capa de acceso a datos (DAO) para el inicio de sesión: lee las credenciales de las
 * tres tablas de usuarios (Mesero, PersonalCocina y Administrador) como una sola lista.
 * AutenticacionService decide qué hacer con ellas.
 */
public class CredencialDAO {

    /** Fila de credenciales de un usuario, con el rol de la tabla de donde salió. */
    public static class Credencial {
        private final String rol;
        private final int id;
        private final String nombre;
        private final String usuario;
        private final String contrasena;
        private final boolean activo;

        Credencial(String rol, int id, String nombre, String usuario, String contrasena, boolean activo) {
            this.rol = rol;
            this.id = id;
            this.nombre = nombre;
            this.usuario = usuario;
            this.contrasena = contrasena;
            this.activo = activo;
        }

        public String getRol() { return rol; }
        public int getId() { return id; }
        public String getNombre() { return nombre; }
        public String getUsuario() { return usuario; }
        /** Hash PBKDF2 guardado en la base de datos. */
        public String getContrasena() { return contrasena; }
        public boolean isActivo() { return activo; }
    }

    /** Credenciales de todos los usuarios: meseros, cocina y administradores (en ese orden). */
    public List<Credencial> obtenerTodas() {
        List<Credencial> lista = new ArrayList<>();
        lista.addAll(cargar("Mesero", "id_mesero", Constantes.ROL_MESERO));
        lista.addAll(cargar("PersonalCocina", "id_cocina", Constantes.ROL_COCINA));
        lista.addAll(cargar("Administrador", "id_administrador", Constantes.ROL_ADMINISTRADOR));
        return lista;
    }

    private static List<Credencial> cargar(String tabla, String colId, String rol) {
        return Bd.consultar("SELECT " + colId + ", nombre, usuario, contrasena, activo FROM " + tabla,
                rs -> new Credencial(rol, rs.getInt(1), rs.getString("nombre"), rs.getString("usuario"),
                        rs.getString("contrasena"), rs.getInt("activo") == 1));
    }
}
