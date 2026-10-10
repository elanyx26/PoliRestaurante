package api;

import entidades.DetallePedido;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import servicios.DetallePedidoService;

/**
 * HU2: productos de un pedido (agregar, cambiar cantidad o notas, quitar).
 * Solo se pueden modificar mientras el pedido no se haya enviado a cocina.
 * El precio unitario lo toma el sistema del producto y el total del pedido se recalcula solo.
 *   GET    /api/detalles                (?id_pedido=3 solo los de ese pedido)
 *   GET    /api/detalles/{id}
 *   POST   /api/detalles                {"id_pedido":1,"id_producto":2,"cantidad":2,"notas":"sin cebolla"}
 *   PUT    /api/detalles/{id}
 *   DELETE /api/detalles/{id}
 */
public class DetallePedidoRecurso extends Recurso {
    private final DetallePedidoService servicio = new DetallePedidoService();

    public DetallePedidoRecurso() {
        super("/api/detalles");
    }

    static Map<String, Object> plano(DetallePedido d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id_detalle", d.getId_detalle());
        m.put("id_pedido", d.getId_pedido());
        m.put("id_producto", d.getId_producto());
        m.put("cantidad", d.getCantidad());
        m.put("precio_unitario", d.getPrecio_unitario());
        m.put("subtotal", d.getCantidad() * d.getPrecio_unitario());
        m.put("notas", d.getNotas());
        return m;
    }

    /** Copia sobre el detalle solo los campos que vienen en el JSON. */
    static DetallePedido desdeMapa(Map<String, Object> m, DetallePedido d) {
        if (m.containsKey("id_pedido")) d.setId_pedido(entero(m, "id_pedido", 0));
        if (m.containsKey("id_producto")) d.setId_producto(entero(m, "id_producto", 0));
        if (m.containsKey("cantidad")) d.setCantidad(entero(m, "cantidad", 0));
        if (m.containsKey("notas")) d.setNotas(texto(m, "notas"));
        return d;
    }

    @Override
    protected Respuesta manejar(String metodo, String[] ruta, Map<String, Object> cuerpo, Map<String, String> query) {
        if (ruta.length == 0) {
            if (metodo.equals("GET")) {
                List<DetallePedido> datos = query.containsKey("id_pedido")
                        ? servicio.listarPorPedido(id(query.get("id_pedido"))) : servicio.listar();
                List<Object> r = new ArrayList<>();
                for (DetallePedido d : datos) r.add(plano(d));
                return Respuesta.ok(r);
            }
            if (metodo.equals("POST")) {
                DetallePedido d = desdeMapa(cuerpo, new DetallePedido());
                d.setId_detalle(0);
                return Respuesta.creado(plano(servicio.crear(d)));
            }
        } else if (ruta.length == 1) {
            int id = id(ruta[0]);
            DetallePedido existente = servicio.obtenerPorId(id);
            if (existente == null) throw new NoEncontrado("No existe un detalle con id " + id);
            switch (metodo) {
                case "GET":
                    return Respuesta.ok(plano(existente));
                case "PUT":
                    DetallePedido d = desdeMapa(cuerpo, existente);
                    d.setId_detalle(id);
                    return Respuesta.ok(plano(servicio.actualizar(d)));
                case "DELETE":
                    servicio.eliminar(id);
                    return Respuesta.sinContenido();
                default:
            }
        }
        return metodoNoPermitido();
    }
}
