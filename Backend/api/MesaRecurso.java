package api;

import entidades.Mesa;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import servicios.MesaService;

/**
 * HU6 y HU12 / RF-11: CRUD REST de mesas, cambio de estado y consulta de disponibilidad.
 *   GET    /api/mesas                 GET   /api/mesas/{id}
 *   POST   /api/mesas                 PUT   /api/mesas/{id}
 *   DELETE /api/mesas/{id}
 *   PATCH  /api/mesas/{id}/estado     {"estado":"Ocupada"}   (Libre | Ocupada)
 *   GET    /api/mesas/libres          mesas libres (lista)
 *   GET    /api/mesas/ocupadas        mesas ocupadas (lista)
 *   GET    /api/mesas/resumen         {"libres":n,"ocupadas":n,"total":n}   (HU12)
 */
public class MesaRecurso extends Recurso {
    private final MesaService servicio = new MesaService();

    public MesaRecurso() {
        super("/api/mesas");
    }

    static Map<String, Object> plano(Mesa m) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id_mesa", m.getId_mesa());
        r.put("numero", m.getNumero());
        r.put("estado", m.getEstado());
        r.put("id_administrador", m.getId_administrador());
        return r;
    }

    private static List<Object> lista(List<Mesa> mesas) {
        List<Object> r = new ArrayList<>();
        for (Mesa m : mesas) r.add(plano(m));
        return r;
    }

    @Override
    protected Respuesta manejar(String metodo, String[] ruta, Map<String, Object> cuerpo, Map<String, String> query) {
        if (ruta.length == 0) {
            if (metodo.equals("GET")) return Respuesta.ok(lista(servicio.listar()));
            if (metodo.equals("POST")) {
                Mesa m = desdeMapa(cuerpo, new Mesa());
                m.setId_mesa(0);
                return Respuesta.creado(plano(servicio.crear(m)));
            }
        } else if (ruta.length == 1 && metodo.equals("GET") && ruta[0].equals("libres")) {
            return Respuesta.ok(lista(servicio.listarLibres()));
        } else if (ruta.length == 1 && metodo.equals("GET") && ruta[0].equals("ocupadas")) {
            return Respuesta.ok(lista(servicio.listarOcupadas()));
        } else if (ruta.length == 1 && metodo.equals("GET") && ruta[0].equals("resumen")) {
            int libres = servicio.contarLibres();
            int ocupadas = servicio.contarOcupadas();
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("libres", libres);
            r.put("ocupadas", ocupadas);
            r.put("total", libres + ocupadas);
            return Respuesta.ok(r);
        } else if (ruta.length == 1) {
            int id = id(ruta[0]);
            Mesa existente = existente(id);
            switch (metodo) {
                case "GET":
                    return Respuesta.ok(plano(existente));
                case "PUT":
                    Mesa m = desdeMapa(cuerpo, existente);
                    m.setId_mesa(id);
                    return Respuesta.ok(plano(servicio.actualizar(m)));
                case "DELETE":
                    servicio.eliminar(id);
                    return Respuesta.sinContenido();
                default:
            }
        } else if (ruta.length == 2 && ruta[1].equals("estado") && metodo.equals("PATCH")) {
            int id = id(ruta[0]);
            existente(id);
            return Respuesta.ok(plano(servicio.cambiarEstado(id, texto(cuerpo, "estado"))));
        }
        return metodoNoPermitido();
    }

    private Mesa existente(int id) {
        Mesa m = servicio.obtenerPorId(id);
        if (m == null) throw new NoEncontrado("No existe una mesa con id " + id);
        return m;
    }

    /** Copia sobre la mesa solo los campos que vienen en el JSON. */
    private static Mesa desdeMapa(Map<String, Object> m, Mesa mesa) {
        if (m.containsKey("numero")) mesa.setNumero(entero(m, "numero", 0));
        if (m.containsKey("estado")) mesa.setEstado(texto(m, "estado"));
        if (m.containsKey("id_administrador")) mesa.setId_administrador(entero(m, "id_administrador", 0));
        return mesa;
    }
}
