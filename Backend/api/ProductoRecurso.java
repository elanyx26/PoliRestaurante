package api;

import entidades.Producto;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import servicios.ProductoService;

/**
 * HU5 / RF-10: CRUD REST de productos del menú + disponibilidad.
 *   GET    /api/productos                      (?disponibles=true solo los "Disponible")
 *   GET    /api/productos/{id}
 *   POST   /api/productos
 *   PUT    /api/productos/{id}
 *   PATCH  /api/productos/{id}/disponibilidad  {"disponibilidad":"No disponible"}
 *   DELETE /api/productos/{id}
 */
public class ProductoRecurso extends Recurso {
    private final ProductoService servicio = new ProductoService();

    public ProductoRecurso() {
        super("/api/productos");
    }

    static Map<String, Object> plano(Producto p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id_producto", p.getId_producto());
        m.put("nombre", p.getNombre());
        m.put("descripcion", p.getDescripcion());
        m.put("precio", p.getPrecio());
        m.put("id_categoria", p.getId_categoria());
        m.put("id_administrador", p.getId_administrador());
        m.put("disponibilidad", p.getDisponibilidad());
        return m;
    }

    private static Map<String, Object> completo(Producto p) {
        Map<String, Object> m = plano(p);
        if (p.getCategoria() != null) m.put("categoria", CategoriaRecurso.plano(p.getCategoria()));
        return m;
    }

    @Override
    protected Respuesta manejar(String metodo, String[] ruta, Map<String, Object> cuerpo, Map<String, String> query) {
        if (ruta.length == 0) {
            if (metodo.equals("GET")) {
                boolean soloDisponibles = "true".equalsIgnoreCase(query.get("disponibles"));
                List<Producto> lista = soloDisponibles ? servicio.listarDisponibles() : servicio.listar();
                List<Object> r = new ArrayList<>();
                for (Producto p : lista) r.add(plano(p));
                return Respuesta.ok(r);
            }
            if (metodo.equals("POST")) {
                Producto p = desdeMapa(cuerpo, new Producto());
                p.setId_producto(0);
                return Respuesta.creado(completo(servicio.crear(p)));
            }
        } else if (ruta.length == 1) {
            int id = id(ruta[0]);
            Producto existente = existente(id);
            switch (metodo) {
                case "GET":
                    return Respuesta.ok(completo(existente));
                case "PUT":
                    Producto p = desdeMapa(cuerpo, existente);
                    p.setId_producto(id);
                    return Respuesta.ok(completo(servicio.actualizar(p)));
                case "DELETE":
                    servicio.eliminar(id);
                    return Respuesta.sinContenido();
                default:
            }
        } else if (ruta.length == 2 && ruta[1].equals("disponibilidad") && metodo.equals("PATCH")) {
            int id = id(ruta[0]);
            Producto p = existente(id);
            p.setDisponibilidad(texto(cuerpo, "disponibilidad"));
            return Respuesta.ok(completo(servicio.actualizar(p)));
        }
        return metodoNoPermitido();
    }

    private Producto existente(int id) {
        Producto p = servicio.obtenerPorId(id);
        if (p == null) throw new NoEncontrado("No existe un producto con id " + id);
        return p;
    }

    /** Copia sobre el producto solo los campos que vienen en el JSON. */
    private static Producto desdeMapa(Map<String, Object> m, Producto p) {
        if (m.containsKey("nombre")) p.setNombre(texto(m, "nombre"));
        if (m.containsKey("descripcion")) p.setDescripcion(texto(m, "descripcion"));
        if (m.containsKey("precio")) p.setPrecio(entero(m, "precio", 0));
        if (m.containsKey("id_categoria")) p.setId_categoria(entero(m, "id_categoria", 0));
        if (m.containsKey("id_administrador")) p.setId_administrador(entero(m, "id_administrador", 0));
        if (m.containsKey("disponibilidad")) p.setDisponibilidad(texto(m, "disponibilidad"));
        return p;
    }
}
