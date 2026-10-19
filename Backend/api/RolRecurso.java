package api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import servicios.AdministradorService;
import servicios.MeseroService;
import servicios.PersonalCocinaService;

/**
 * HU8 / RF-12: asignar roles a los usuarios.
 *   GET  /api/roles/usuarios   todos los usuarios del sistema con su rol y si están activos
 *   POST /api/roles/asignar    {"id_administrador":1,"usuario":"jperez","nuevo_rol":"cocina"}
 * Roles válidos: mesero | cocina | administrador. Solo un administrador activo puede asignar.
 */
public class RolRecurso extends Recurso {
    private final AdministradorService admins = new AdministradorService();
    private final MeseroService meseros = new MeseroService();
    private final PersonalCocinaService cocina = new PersonalCocinaService();

    public RolRecurso() {
        super("/api/roles");
    }

    @Override
    protected Respuesta manejar(String metodo, String[] ruta, Map<String, Object> cuerpo, Map<String, String> query) {
        if (ruta.length == 1 && ruta[0].equals("usuarios") && metodo.equals("GET")) {
            List<Object> todos = new ArrayList<>();
            for (entidades.Administrador a : admins.listar()) {
                todos.add(UsuarioRecurso.vista(a.getId_administrador(), a.getNombre(), a.getUsuario(),
                        util.Constantes.ROL_ADMINISTRADOR, admins.estaActivo(a.getId_administrador())));
            }
            for (entidades.Mesero m : meseros.listar()) {
                todos.add(UsuarioRecurso.vista(m.getId_mesero(), m.getNombre(), m.getUsuario(),
                        util.Constantes.ROL_MESERO, meseros.estaActivo(m.getId_mesero())));
            }
            for (entidades.PersonalCocina c : cocina.listar()) {
                todos.add(UsuarioRecurso.vista(c.getId_cocina(), c.getNombre(), c.getUsuario(),
                        util.Constantes.ROL_COCINA, cocina.estaActivo(c.getId_cocina())));
            }
            return Respuesta.ok(todos);
        }
        if (ruta.length == 1 && ruta[0].equals("asignar") && metodo.equals("POST")) {
            int idAdmin = entero(cuerpo, "id_administrador", 0);
            String usuario = texto(cuerpo, "usuario");
            String rol = texto(cuerpo, "nuevo_rol");
            if (usuario == null || usuario.trim().isEmpty()) {
                throw new IllegalArgumentException("El campo 'usuario' es obligatorio");
            }
            if (rol == null || rol.trim().isEmpty()) {
                throw new IllegalArgumentException("El campo 'nuevo_rol' es obligatorio");
            }
            admins.asignarRol(idAdmin, usuario.trim(), rol.trim().toLowerCase());
            Map<String, Object> r = new java.util.LinkedHashMap<>();
            r.put("mensaje", "Rol '" + rol.trim().toLowerCase() + "' asignado a " + usuario.trim());
            return Respuesta.ok(r);
        }
        return metodoNoPermitido();
    }
}
