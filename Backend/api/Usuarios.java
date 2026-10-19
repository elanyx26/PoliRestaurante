package api;

import entidades.Administrador;
import entidades.Mesero;
import entidades.PersonalCocina;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import servicios.AdministradorService;
import servicios.MeseroService;
import servicios.PersonalCocinaService;
import util.Constantes;

/** Construye los tres CRUD de usuarios (administradores, meseros y personal de cocina). */
public final class Usuarios {
    private Usuarios() { }

    public static UsuarioRecurso administradores() {
        AdministradorService s = new AdministradorService();
        return new UsuarioRecurso("/api/administradores", Constantes.ROL_ADMINISTRADOR,
                () -> {
                    List<Map<String, Object>> r = new ArrayList<>();
                    for (Administrador a : s.listar()) r.add(vista(a, s));
                    return r;
                },
                id -> {
                    Administrador a = s.obtenerPorId(id);
                    return a == null ? null : vista(a, s);
                },
                (id, b) -> {
                    Administrador a = new Administrador();
                    a.setId_administrador(id);
                    a.setNombre(Recurso.texto(b, "nombre"));
                    a.setUsuario(Recurso.texto(b, "usuario"));
                    a.setContrasena(Recurso.texto(b, "contrasena"));
                    Administrador g = id == 0 ? s.crear(a) : s.actualizar(a);
                    return vista(g, s);
                },
                s::eliminar, s::activar, s::desactivar);
    }

    public static UsuarioRecurso meseros() {
        MeseroService s = new MeseroService();
        return new UsuarioRecurso("/api/meseros", Constantes.ROL_MESERO,
                () -> {
                    List<Map<String, Object>> r = new ArrayList<>();
                    for (Mesero m : s.listar()) r.add(vista(m, s));
                    return r;
                },
                id -> {
                    Mesero m = s.obtenerPorId(id);
                    return m == null ? null : vista(m, s);
                },
                (id, b) -> {
                    Mesero m = new Mesero();
                    m.setId_mesero(id);
                    m.setNombre(Recurso.texto(b, "nombre"));
                    m.setUsuario(Recurso.texto(b, "usuario"));
                    m.setContrasena(Recurso.texto(b, "contrasena"));
                    Mesero g = id == 0 ? s.crear(m) : s.actualizar(m);
                    return vista(g, s);
                },
                s::eliminar, s::activar, s::desactivar);
    }

    public static UsuarioRecurso cocina() {
        PersonalCocinaService s = new PersonalCocinaService();
        return new UsuarioRecurso("/api/cocina", Constantes.ROL_COCINA,
                () -> {
                    List<Map<String, Object>> r = new ArrayList<>();
                    for (PersonalCocina c : s.listar()) r.add(vista(c, s));
                    return r;
                },
                id -> {
                    PersonalCocina c = s.obtenerPorId(id);
                    return c == null ? null : vista(c, s);
                },
                (id, b) -> {
                    PersonalCocina c = new PersonalCocina();
                    c.setId_cocina(id);
                    c.setNombre(Recurso.texto(b, "nombre"));
                    c.setUsuario(Recurso.texto(b, "usuario"));
                    c.setContrasena(Recurso.texto(b, "contrasena"));
                    PersonalCocina g = id == 0 ? s.crear(c) : s.actualizar(c);
                    return vista(g, s);
                },
                s::eliminar, s::activar, s::desactivar);
    }

    private static Map<String, Object> vista(Administrador a, AdministradorService s) {
        return UsuarioRecurso.vista(a.getId_administrador(), a.getNombre(), a.getUsuario(),
                Constantes.ROL_ADMINISTRADOR, s.estaActivo(a.getId_administrador()));
    }

    private static Map<String, Object> vista(Mesero m, MeseroService s) {
        return UsuarioRecurso.vista(m.getId_mesero(), m.getNombre(), m.getUsuario(),
                Constantes.ROL_MESERO, s.estaActivo(m.getId_mesero()));
    }

    private static Map<String, Object> vista(PersonalCocina c, PersonalCocinaService s) {
        return UsuarioRecurso.vista(c.getId_cocina(), c.getNombre(), c.getUsuario(),
                Constantes.ROL_COCINA, s.estaActivo(c.getId_cocina()));
    }
}
