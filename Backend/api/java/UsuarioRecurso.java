package api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * CRUD REST de un tipo de usuario (Administrador, Mesero o PersonalCocina).
 * La contraseña nunca se devuelve en las respuestas.
 *   GET    {base}                 GET    {base}/{id}
 *   POST   {base}                 PUT    {base}/{id}   (sin "contrasena" conserva la actual)
 *   DELETE {base}/{id}
 *   PATCH  {base}/{id}/activar    PATCH  {base}/{id}/desactivar
 * Cuerpo para crear/editar: {"nombre":"..","usuario":"..","contrasena":".."}
 */
public class UsuarioRecurso extends Recurso {
    private final Supplier<List<Map<String, Object>>> listar;
    private final Function<Integer, Map<String, Object>> obtener;
    /** (id, cuerpo) -> usuario; id = 0 crea, id > 0 actualiza. */
    private final BiFunction<Integer, Map<String, Object>, Map<String, Object>> guardar;
    private final Consumer<Integer> eliminar;
    private final Consumer<Integer> activar;
    private final Consumer<Integer> desactivar;
    private final String nombreRol;

    public UsuarioRecurso(String base, String nombreRol,
                          Supplier<List<Map<String, Object>>> listar,
                          Function<Integer, Map<String, Object>> obtener,
                          BiFunction<Integer, Map<String, Object>, Map<String, Object>> guardar,
                          Consumer<Integer> eliminar,
                          Consumer<Integer> activar,
                          Consumer<Integer> desactivar) {
        super(base);
        this.nombreRol = nombreRol;
        this.listar = listar;
        this.obtener = obtener;
        this.guardar = guardar;
        this.eliminar = eliminar;
        this.activar = activar;
        this.desactivar = desactivar;
    }

    /** Vista pública de un usuario (sin contraseña). */
    public static Map<String, Object> vista(int id, String nombre, String usuario, String rol, boolean activo) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("nombre", nombre);
        m.put("usuario", usuario);
        m.put("rol", rol);
        m.put("activo", activo);
        return m;
    }

    @Override
    protected Respuesta manejar(String metodo, String[] ruta, Map<String, Object> cuerpo, Map<String, String> query) {
        if (ruta.length == 0) {
            if (metodo.equals("GET")) return Respuesta.ok(listar.get());
            if (metodo.equals("POST")) return Respuesta.creado(guardar.apply(0, cuerpo));
        } else if (ruta.length == 1) {
            int id = id(ruta[0]);
            Map<String, Object> existente = existente(id);
            switch (metodo) {
                case "GET":
                    return Respuesta.ok(existente);
                case "PUT":
                    return Respuesta.ok(guardar.apply(id, cuerpo));
                case "DELETE":
                    eliminar.accept(id);
                    return Respuesta.sinContenido();
                default:
            }
        } else if (ruta.length == 2 && metodo.equals("PATCH")) {
            int id = id(ruta[0]);
            existente(id);
            if (ruta[1].equals("activar")) {
                activar.accept(id);
                return Respuesta.ok(obtener.apply(id));
            }
            if (ruta[1].equals("desactivar")) {
                desactivar.accept(id);
                return Respuesta.ok(obtener.apply(id));
            }
        }
        return metodoNoPermitido();
    }

    private Map<String, Object> existente(int id) {
        Map<String, Object> u = obtener.apply(id);
        if (u == null) throw new NoEncontrado("No existe un usuario con rol " + nombreRol + " y id " + id);
        return u;
    }
}
