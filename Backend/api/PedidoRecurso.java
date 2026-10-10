package api;

import entidades.DetallePedido;
import entidades.Domicilio;
import entidades.Pedido;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import servicios.DetallePedidoService;
import servicios.PedidoService;
import util.CambioEstado;
import util.Fechas;

/**
 * Pedidos del mesero y cola de cocina.
 *   HU1  POST   /api/pedidos                         crear pedido (con productos opcionales)
 *   HU3  PATCH  /api/pedidos/{id}/enviar-cocina      confirmar y enviar a cocina
 *   HU4  PATCH  /api/pedidos/{id}/iniciar-preparacion {"id_cocina":1}   Pendiente -> En preparación
 *   HU4  PATCH  /api/pedidos/{id}/listo               {"id_cocina":1}   En preparación -> Listo
 *   HU4  GET    /api/pedidos/{id}/historial-estados   fecha y hora de cada cambio de estado
 *   HU13 GET    /api/pedidos/cola-cocina              pedidos pendientes, del más antiguo al más reciente
 *   CRUD GET /api/pedidos, GET /api/pedidos/{id}, PUT /api/pedidos/{id} (nombre del cliente),
 *        DELETE /api/pedidos/{id} (solo pedidos que aún no se enviaron a cocina)
 */
public class PedidoRecurso extends Recurso {
    private final PedidoService servicio = new PedidoService();
    private final DetallePedidoService detalles = new DetallePedidoService();

    public PedidoRecurso() {
        super("/api/pedidos");
    }

    static Map<String, Object> plano(Pedido p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id_pedido", p.getId_pedido());
        m.put("id_mesero", p.getId_mesero());
        m.put("id_cocina", p.getId_cocina() > 0 ? p.getId_cocina() : null);
        m.put("id_administrador", p.getId_administrador() > 0 ? p.getId_administrador() : null);
        m.put("id_mesa", p.getId_mesa() > 0 ? p.getId_mesa() : null);
        m.put("tipo_pedido", p.getTipo_pedido());
        m.put("nombre_cliente", p.getNombre_cliente());
        m.put("estado", p.getEstado());
        m.put("total", p.getTotal());
        m.put("metodo_pago", p.getMetodo_pago());
        m.put("fecha_creacion", p.getFecha_creacion());
        m.put("fecha_cierre", p.getFecha_cierre());
        return m;
    }

    /** Pedido con sus productos (para GET por id y al crear). */
    private Map<String, Object> completo(Pedido p) {
        Map<String, Object> m = plano(p);
        List<Object> lista = new ArrayList<>();
        for (DetallePedido d : detalles.listarPorPedido(p.getId_pedido())) {
            lista.add(DetallePedidoRecurso.plano(d));
        }
        m.put("detalles", lista);
        return m;
    }

    private static List<Object> lista(List<Pedido> pedidos) {
        List<Object> r = new ArrayList<>();
        for (Pedido p : pedidos) r.add(plano(p));
        return r;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Respuesta manejar(String metodo, String[] ruta, Map<String, Object> cuerpo, Map<String, String> query) {
        if (ruta.length == 0) {
            if (metodo.equals("GET")) return Respuesta.ok(lista(servicio.listar()));
            if (metodo.equals("POST")) {
                Pedido p = new Pedido();
                p.setId_mesero(entero(cuerpo, "id_mesero", 0));
                p.setId_mesa(entero(cuerpo, "id_mesa", 0));
                p.setId_administrador(entero(cuerpo, "id_administrador", 0));
                p.setTipo_pedido(texto(cuerpo, "tipo_pedido"));
                p.setNombre_cliente(texto(cuerpo, "nombre_cliente"));
                Object det = cuerpo.get("detalles");
                if (det instanceof List && !((List<Object>) det).isEmpty()) {
                    List<DetallePedido> productos = new ArrayList<>();
                    for (Object o : (List<Object>) det) {
                        if (!(o instanceof Map)) {
                            throw new IllegalArgumentException("Cada elemento de 'detalles' debe ser un objeto");
                        }
                        productos.add(DetallePedidoRecurso.desdeMapa((Map<String, Object>) o, new DetallePedido()));
                    }
                    Domicilio dom = null;
                    if (cuerpo.get("domicilio") instanceof Map) {
                        Map<String, Object> dm = (Map<String, Object>) cuerpo.get("domicilio");
                        dom = new Domicilio();
                        dom.setDireccion(texto(dm, "direccion"));
                        dom.setTelefono(texto(dm, "telefono"));
                        dom.setCosto_envio(entero(dm, "costo_envio", 0));
                    }
                    servicio.crearPedido(p, productos, dom);
                } else {
                    servicio.crear(p);   // borrador: los productos se agregan luego en /api/detalles
                }
                return Respuesta.creado(completo(servicio.obtenerPorId(p.getId_pedido())));
            }
        } else if (ruta.length == 1 && metodo.equals("GET") && ruta[0].equals("cola-cocina")) {
            return Respuesta.ok(lista(servicio.colaCocina()));
        } else if (ruta.length == 1) {
            int id = id(ruta[0]);
            Pedido existente = existente(id);
            switch (metodo) {
                case "GET":
                    return Respuesta.ok(completo(existente));
                case "PUT":
                    Pedido p = new Pedido();
                    p.setId_pedido(id);
                    p.setNombre_cliente(texto(cuerpo, "nombre_cliente"));
                    return Respuesta.ok(completo(servicio.actualizar(p)));
                case "DELETE":
                    servicio.eliminar(id);
                    return Respuesta.sinContenido();
                default:
            }
        } else if (ruta.length == 2) {
            int id = id(ruta[0]);
            existente(id);
            if (metodo.equals("GET") && ruta[1].equals("historial-estados")) {
                List<Object> r = new ArrayList<>();
                for (CambioEstado c : servicio.historialDeEstados(id)) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("estado", c.getEstado());
                    m.put("fecha", c.getFecha().format(Fechas.FORMATO));
                    r.add(m);
                }
                return Respuesta.ok(r);
            }
            if (metodo.equals("PATCH")) {
                switch (ruta[1]) {
                    case "enviar-cocina":
                        return Respuesta.ok(completo(servicio.enviarACocina(id)));
                    case "iniciar-preparacion":
                        return Respuesta.ok(completo(servicio.iniciarPreparacion(id, entero(cuerpo, "id_cocina", 0))));
                    case "listo":
                        return Respuesta.ok(completo(servicio.marcarListo(id, entero(cuerpo, "id_cocina", 0))));
                    default:
                }
            }
        }
        return metodoNoPermitido();
    }

    private Pedido existente(int id) {
        Pedido p = servicio.obtenerPorId(id);
        if (p == null) throw new NoEncontrado("No existe el pedido " + id);
        return p;
    }
}
