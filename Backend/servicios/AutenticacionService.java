package servicios;

import java.util.ArrayList;
import java.util.List;
import util.Bd;
import util.Constantes;
import util.Seguridad;

/**
 * HU14 / RF-01 / RNF-01 / RNF-02: inicio de sesión según el rol.
 * Consulta las tablas Mesero, PersonalCocina y Administrador. Quien inicia
 * sesión recibe una Sesion con su rol; cada servicio de negocio valida además
 * que el actor tenga permiso.
 */
public class AutenticacionService {

    /** Resultado de un inicio de sesión válido. */
    public static class Sesion {
        private final String rol;
        private final int id;
        private final String nombre;
        private final String usuario;
        private final String contrasenaProtegida;
        private final boolean activo;

        Sesion(String rol, int id, String nombre, String usuario, String contrasenaProtegida, boolean activo) {
            this.rol = rol;
            this.id = id;
            this.nombre = nombre;
            this.usuario = usuario;
            this.contrasenaProtegida = contrasenaProtegida;
            this.activo = activo;
        }

        public String getRol() { return rol; }
        public int getId() { return id; }
        public String getNombre() { return nombre; }
        public String getUsuario() { return usuario; }
        /** Hash PBKDF2 (nunca la contraseña real). Solo uso interno entre servicios. */
        String getContrasenaProtegida() { return contrasenaProtegida; }

        @Override
        public String toString() { return nombre + " (" + rol + ")"; }
    }

    /**
     * Con credenciales válidas devuelve la sesión del usuario según su rol.
     * Con credenciales inválidas lanza IllegalArgumentException; un usuario
     * desactivado lanza IllegalStateException.
     */
    public Sesion iniciarSesion(String usuario, String contrasena) {
        if (usuario == null || usuario.trim().isEmpty() || contrasena == null || contrasena.isEmpty()) {
            throw new IllegalArgumentException("Credenciales inválidas");
        }
        boolean coincideInactivo = false;
        for (Sesion s : todos()) {
            if (s.usuario != null && s.usuario.equals(usuario.trim())
                    && Seguridad.verificar(contrasena, s.contrasenaProtegida)) {
                if (s.activo) {
                    return s;
                }
                coincideInactivo = true;
            }
        }
        if (coincideInactivo) {
            throw new IllegalStateException("El usuario está desactivado");
        }
        throw new IllegalArgumentException("Credenciales inválidas");
    }

    // ------------------------------------------------- uso interno de servicios

    /** true si otro usuario ACTIVO (distinto de rol+id indicado) ya usa ese nombre de usuario. */
    static boolean usuarioEnUso(String usuario, String rolPropio, int idPropio) {
        if (usuario == null) {
            return false;
        }
        for (Sesion s : todos()) {
            boolean esElMismo = s.rol.equals(rolPropio) && s.id == idPropio;
            if (!esElMismo && s.activo && usuario.trim().equals(s.usuario)) {
                return true;
            }
        }
        return false;
    }

    static Sesion buscarActivo(String usuario) {
        if (usuario == null) {
            return null;
        }
        for (Sesion s : todos()) {
            if (s.activo && usuario.trim().equals(s.usuario)) {
                return s;
            }
        }
        return null;
    }

    private static List<Sesion> todos() {
        List<Sesion> lista = new ArrayList<>();
        lista.addAll(cargar("Mesero", "id_mesero", Constantes.ROL_MESERO));
        lista.addAll(cargar("PersonalCocina", "id_cocina", Constantes.ROL_COCINA));
        lista.addAll(cargar("Administrador", "id_administrador", Constantes.ROL_ADMINISTRADOR));
        return lista;
    }

    private static List<Sesion> cargar(String tabla, String colId, String rol) {
        return Bd.consultar("SELECT " + colId + ", nombre, usuario, contrasena, activo FROM " + tabla,
                rs -> new Sesion(rol, rs.getInt(1), rs.getString("nombre"), rs.getString("usuario"),
                        rs.getString("contrasena"), rs.getInt("activo") == 1));
    }
}
