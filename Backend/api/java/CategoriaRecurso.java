package api;

import entidades.Categoria;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import servicios.CategoriaService;

/**
 * HU18 / RF-10: CRUD REST de categorías del menú.
 *   GET    /api/categorias        GET    /api/categorias/{id}
 *   POST   /api/categorias        PUT    /api/categorias/{id}
 *   DELETE /api/categorias/{id}
 */
public class CategoriaRecurso extends Recurso {
    private final CategoriaService servicio = new CategoriaService();

    public CategoriaRecurso() {
        super("/api/categorias");
    }

    static Map<String, Object> plano(Categoria c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id_categoria", c.getId_categoria());
        m.put("nombre", c.getNombre());
        m.put("id_administrador", c.getId_administrador());
        return m;
    }

    @Override
    protected Respuesta manejar(String metodo, String[] ruta, Map<String, Object> cuerpo, Map<String, String> query) {
        if (ruta.length == 0) {
            if (metodo.equals("GET")) {
                List<Object> r = new ArrayList<>();
                for (Categoria c : servicio.listar()) r.add(plano(c));
                return Respuesta.ok(r);
            }
            if (metodo.equals("POST")) {
                Categoria c = desdeMapa(cuerpo, new Categoria());
                c.setId_categoria(0);
                return Respuesta.creado(plano(servicio.crear(c)));
            }
        } else if (ruta.length == 1) {
            int id = id(ruta[0]);
            Categoria existente = servicio.obtenerPorId(id);
            if (existente == null) throw new NoEncontrado("No existe una categoría con id " + id);
            switch (metodo) {
                case "GET":
                    return Respuesta.ok(plano(existente));
                case "PUT":
                    Categoria c = desdeMapa(cuerpo, existente);
                    c.setId_categoria(id);
                    return Respuesta.ok(plano(servicio.actualizar(c)));
                case "DELETE":
                    servicio.eliminar(id);
                    return Respuesta.sinContenido();
                default:
            }
        }
        return metodoNoPermitido();
    }

    /** Copia sobre la categoría solo los campos que vienen en el JSON. */
    private static Categoria desdeMapa(Map<String, Object> m, Categoria c) {
        if (m.containsKey("nombre")) c.setNombre(texto(m, "nombre"));
        if (m.containsKey("id_administrador")) c.setId_administrador(entero(m, "id_administrador", 0));
        return c;
    }
}
