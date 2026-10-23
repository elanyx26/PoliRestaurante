package servicios;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import dao.DetallePedidoDAO;
import dao.DomicilioDAO;
import dao.HistorialEstadoDAO;
import dao.PedidoDAO;
import entidades.DetallePedido;
import entidades.Domicilio;
import entidades.Mesa;
import entidades.Pedido;
import entidades.Producto;
import util.CambioEstado;
import util.Constantes;
import util.Fechas;
import util.ReporteVentas;
import util.Validar;

/**
 * Ciclo de vida del pedido (tablas Pedido e HistorialEstado).
 * HU1 a HU4, HU9 a HU11, HU13, HU15 a HU17, HU20 a HU26.
 *
 *
 * Mientras un pedido no se envíe a cocina (enviarACocina) se pueden agregar,
 * editar o quitar sus productos y notas.
 */
public class PedidoService {
    private final PedidoDAO dao = new PedidoDAO();
    private final HistorialEstadoDAO historial = new HistorialEstadoDAO();

    private static final List<String> TIPOS = Arrays.asList(
            Constantes.TIPO_MESA, Constantes.TIPO_LLEVAR, Constantes.TIPO_DOMICILIO);

    // ====================================================================
    //  CRUD
    // ====================================================================

    /**
     * Crea el pedido en estado "Pendiente" (borrador editable). Los productos se
     * agregan con DetallePedidoService y los datos de entrega con DomicilioService;
     * para crear todo de una vez (exigiendo al menos un producto) use
     * {@link #crearPedido(Pedido, List, Domicilio)}.
     */
    public Pedido crear(Pedido entidad) {
        validarNuevo(entidad);
        boolean enMesa = Constantes.TIPO_MESA.equals(entidad.getTipo_pedido());
        if (!enMesa) {
            entidad.setId_mesa(0);                 // sin mesa (RF-17, RF-19)
        }
        entidad.setId_cocina(0);
        entidad.setEstado(Constantes.PENDIENTE);
        entidad.setTotal(0);
        entidad.setMetodo_pago(null);
        entidad.setFecha_creacion(Fechas.ahora());
        entidad.setFecha_cierre(null);

        dao.crear(entidad);
        registrar(entidad.getId_pedido(), Constantes.PENDIENTE);
        if (enMesa) {
            new MesaService().cambiarEstado(entidad.getId_mesa(), Constantes.MESA_OCUPADA);
        }
        hidratar(entidad);
        return entidad;
    }

    public Pedido obtenerPorId(int id_pedido) {
        Pedido p = cargar(id_pedido);
        if (p != null) {
            hidratar(p);
        }
        return p;
    }

    public List<Pedido> listar() {
        return dao.obtenerTodos();
    }

    /**
     * Solo permite corregir el nombre del cliente. El estado, total, fechas, mesa y
     * pago no se pueden modificar manualmente por aquí (HU2).
     */
    public Pedido actualizar(Pedido entidad) {
        Validar.noNulo(entidad, "Pedido");
        Pedido existente = requerir(entidad.getId_pedido());
        if (estaFinalizado(existente) || Constantes.ENTREGADO.equals(existente.getEstado())) {
            throw new IllegalStateException("Un pedido " + existente.getEstado().toLowerCase() + " no se puede modificar");
        }
        String nombre = entidad.getNombre_cliente();
        if (!Constantes.TIPO_MESA.equals(existente.getTipo_pedido())) {
            nombre = Validar.texto(nombre, "nombre_cliente");
        }
        dao.actualizarNombreCliente(existente.getId_pedido(), nombre);
        return obtenerPorId(existente.getId_pedido());
    }

    /**
     * Solo se puede eliminar un pedido borrador (aún no enviado a cocina). Para los
     * demás casos se debe cancelar, de modo que quede el historial (RNF-09).
     */
    public void eliminar(int id_pedido) {
        Pedido p = cargar(id_pedido);
        if (p == null) {
            return;
        }
        if (!esEditable(id_pedido)) {
            throw new IllegalStateException("El pedido ya fue enviado a cocina o finalizó; cancélelo en lugar de eliminarlo");
        }
        new DetallePedidoService().eliminarPorPedido(id_pedido);
        new DomicilioService().eliminarPorPedido(id_pedido);
        historial.eliminarPorPedido(id_pedido);
        dao.eliminar(id_pedido);
        liberarMesaSiCorresponde(p.getId_mesa());
    }

    // ====================================================================
    //  Creación completa (HU1, HU21, HU24)
    // ====================================================================

    public Pedido crearPedido(Pedido pedido, List<DetallePedido> detalles) {
        return crearPedido(pedido, detalles, null);
    }

    /**
     * Crea un pedido (en mesa, para llevar o a domicilio) con sus productos y, si es
     * a domicilio, con sus datos de entrega. El pedido no se crea sin al menos un
     * producto. Si algo falla se deshace lo creado.
     */
    public Pedido crearPedido(Pedido pedido, List<DetallePedido> detalles, Domicilio domicilio) {
        Validar.noNulo(pedido, "Pedido");
        if (detalles == null || detalles.isEmpty()) {
            throw new IllegalArgumentException("El pedido no se puede crear sin al menos un producto");
        }
        if (Constantes.TIPO_DOMICILIO.equals(pedido.getTipo_pedido())) {
            if (domicilio == null) {
                throw new IllegalArgumentException("El pedido a domicilio requiere dirección, teléfono y costo de envío");
            }
        } else if (domicilio != null) {
            throw new IllegalArgumentException("Solo los pedidos a domicilio llevan datos de entrega");
        }
        validarNuevo(pedido);

        crear(pedido);
        try {
            DetallePedidoService ds = new DetallePedidoService();
            for (DetallePedido d : detalles) {
                d.setId_pedido(pedido.getId_pedido());
                ds.crear(d);
            }
            if (domicilio != null) {
                domicilio.setId_pedido(pedido.getId_pedido());
                new DomicilioService().crear(domicilio);
            }
        } catch (RuntimeException e) {
            eliminar(pedido.getId_pedido());      // deshacer lo creado
            throw e;
        }
        pedido.setTotal(cargar(pedido.getId_pedido()).getTotal());
        return pedido;
    }

    // ====================================================================
    //  Flujo del pedido
    // ====================================================================

    /** HU3: confirma el pedido y lo envía a la cola de cocina; ya no se pueden editar sus productos. */
    public Pedido enviarACocina(int id_pedido) {
        Pedido p = requerir(id_pedido);
        if (!esEditable(id_pedido)) {
            throw new IllegalStateException("El pedido ya fue enviado a cocina");
        }
        if (new DetallePedidoService().listarPorPedido(id_pedido).isEmpty()) {
            throw new IllegalStateException("El pedido no se puede enviar a cocina sin al menos un producto");
        }
        if (Constantes.TIPO_DOMICILIO.equals(p.getTipo_pedido())
                && new DomicilioService().obtenerPorPedido(id_pedido) == null) {
            throw new IllegalStateException("El pedido a domicilio requiere sus datos de entrega");
        }
        dao.marcarEnviadoACocina(id_pedido);
        return obtenerPorId(id_pedido);
    }

    /** HU4 / RF-06: cocina toma el pedido (Pendiente -> En preparación). */
    public Pedido iniciarPreparacion(int id_pedido, int id_cocina) {
        if (!new PersonalCocinaService().estaActivo(id_cocina)) {
            throw new IllegalArgumentException("El usuario de cocina no existe o está desactivado");
        }
        Pedido p = requerir(id_pedido);
        if (!Constantes.PENDIENTE.equals(p.getEstado()) || !estaEnviado(id_pedido)) {
            throw new IllegalStateException("Solo un pedido Pendiente enviado a cocina puede pasar a En preparación");
        }
        dao.asignarCocina(id_pedido, id_cocina);
        cambiarEstado(id_pedido, Constantes.EN_PREPARACION);
        return obtenerPorId(id_pedido);
    }

    /** HU4 / RF-07: cocina termina (En preparación -> Listo). */
    public Pedido marcarListo(int id_pedido, int id_cocina) {
        if (!new PersonalCocinaService().estaActivo(id_cocina)) {
            throw new IllegalArgumentException("El usuario de cocina no existe o está desactivado");
        }
        Pedido p = requerir(id_pedido);
        if (!Constantes.EN_PREPARACION.equals(p.getEstado())) {
            throw new IllegalStateException("Solo un pedido En preparación puede pasar a Listo");
        }
        cambiarEstado(id_pedido, Constantes.LISTO);
        return obtenerPorId(id_pedido);
    }

    /** HU9 / RF-08: el mesero entrega el pedido (solo si está Listo); opcionalmente libera la mesa. */
    public Pedido entregar(int id_pedido, int id_mesero, boolean liberarMesa) {
        if (!new MeseroService().estaActivo(id_mesero)) {
            throw new IllegalArgumentException("El mesero no existe o está desactivado");
        }
        Pedido p = requerir(id_pedido);
        if (!Constantes.LISTO.equals(p.getEstado())) {
            throw new IllegalStateException("Solo se puede marcar como Entregado un pedido en estado Listo");
        }
        cambiarEstado(id_pedido, Constantes.ENTREGADO);
        if (liberarMesa) {
            liberarMesaSiCorresponde(p.getId_mesa());
        }
        return obtenerPorId(id_pedido);
    }

    /** HU15 / RF-09: cancela un pedido que aún no ha sido entregado; libera la mesa. */
    public Pedido cancelar(int id_pedido, int id_mesero) {
        if (!new MeseroService().estaActivo(id_mesero)) {
            throw new IllegalArgumentException("El mesero no existe o está desactivado");
        }
        Pedido p = requerir(id_pedido);
        if (Constantes.ENTREGADO.equals(p.getEstado()) || Constantes.CERRADO.equals(p.getEstado())) {
            throw new IllegalStateException("No se puede cancelar un pedido ya entregado");
        }
        if (Constantes.CANCELADO.equals(p.getEstado())) {
            throw new IllegalStateException("El pedido ya está cancelado");
        }
        cambiarEstado(id_pedido, Constantes.CANCELADO);
        liberarMesaSiCorresponde(p.getId_mesa());
        return obtenerPorId(id_pedido);
    }

    /** HU23: registra el método de pago y cierra el pedido entregado; libera la mesa si seguía ocupada. */
    public Pedido cerrar(int id_pedido, String metodo_pago) {
        Pedido p = requerir(id_pedido);
        String metodo = Validar.texto(metodo_pago, "metodo_pago");
        if (!Constantes.ENTREGADO.equals(p.getEstado())) {
            throw new IllegalStateException("Solo se puede cerrar un pedido que ya fue entregado");
        }
        dao.registrarCierre(id_pedido, metodo, Fechas.ahora());
        cambiarEstado(id_pedido, Constantes.CERRADO);
        liberarMesaSiCorresponde(p.getId_mesa());
        return obtenerPorId(id_pedido);
    }

    // ====================================================================
    //  Consultas
    // ====================================================================

    /** HU13 / RF-05: cola de cocina, del más antiguo al más reciente. */
    public List<Pedido> colaCocina() {
        return dao.obtenerColaCocina(Constantes.PENDIENTE);
    }

    /** HU17: pedidos del mesero que no están entregados, cancelados ni cerrados. */
    public List<Pedido> consultarActivosDeMesero(int id_mesero) {
        return dao.obtenerEnCursoDeMesero(id_mesero);
    }

    /** HU10: pedidos anteriores de la mesa, del más reciente al más antiguo. */
    public List<Pedido> historialDeMesa(int id_mesa) {
        return dao.obtenerPorMesa(id_mesa);
    }

    /** RF-15 / RNF-09: fecha y hora de cada cambio de estado del pedido. */
    public List<CambioEstado> historialDeEstados(int id_pedido) {
        return historial.obtenerPorPedido(id_pedido);
    }

    /** HU22 / HU26: cómo ve cocina el pedido (tipo y mesa o nombre del cliente). */
    public String descripcionParaCocina(int id_pedido) {
        Pedido p = requerir(id_pedido);
        if (Constantes.TIPO_MESA.equals(p.getTipo_pedido())) {
            Mesa m = new MesaService().obtenerPorId(p.getId_mesa());
            return "[En mesa] Mesa " + (m == null ? p.getId_mesa() : m.getNumero());
        }
        return "[" + p.getTipo_pedido() + "] Cliente: " + p.getNombre_cliente();
    }

    /** HU11: detalle completo del pedido (productos, cantidades, precios, notas y total). */
    public String resumenPedido(int id_pedido) {
        Pedido p = requerir(id_pedido);
        StringBuilder sb = new StringBuilder();
        sb.append("Pedido #").append(p.getId_pedido()).append(" - ").append(descripcionParaCocina(id_pedido))
          .append(" - Estado: ").append(p.getEstado()).append('\n');
        ProductoService prs = new ProductoService();
        for (DetallePedido d : new DetallePedidoService().listarPorPedido(id_pedido)) {
            Producto prod = prs.obtenerPorId(d.getId_producto());
            sb.append("  ").append(d.getCantidad()).append(" x ").append(prod == null ? "?" : prod.getNombre())
              .append(" @ ").append(d.getPrecio_unitario())
              .append(" = ").append(d.getCantidad() * d.getPrecio_unitario());
            if (d.getNotas() != null && !d.getNotas().trim().isEmpty()) {
                sb.append("  (nota: ").append(d.getNotas()).append(')');
            }
            sb.append('\n');
        }
        Domicilio dom = new DomicilioService().obtenerPorPedido(id_pedido);
        if (dom != null) {
            sb.append("  Envío: ").append(dom.getCosto_envio()).append('\n');
        }
        sb.append("  TOTAL: ").append(p.getTotal());
        return sb.toString();
    }

    // ====================================================================
    //  Reportes (solo administrador) - HU7, HU20
    // ====================================================================

    /** HU7: ventas de los pedidos cerrados, filtradas por fecha de cierre (rango inclusivo; null = sin límite). */
    public ReporteVentas reporteVentas(int id_administrador, LocalDate desde, LocalDate hasta) {
        exigirAdministrador(id_administrador);
        int cantidad = 0, ventas = 0, envios = 0;
        DomicilioService ds = new DomicilioService();
        List<Pedido> cerrados = dao.obtenerPorEstado(Constantes.CERRADO);
        for (Pedido p : cerrados) {
            if (enRango(Fechas.parsear(p.getFecha_cierre()), desde, hasta)) {
                cantidad++;
                ventas += p.getTotal();
                Domicilio d = ds.obtenerPorPedido(p.getId_pedido());
                if (d != null) {
                    envios += d.getCosto_envio();
                }
            }
        }
        return new ReporteVentas(cantidad, ventas, envios);
    }

    /** HU20: tiempo entre "En preparación" y "Listo" por pedido, filtrado por fecha de creación. */
    public Map<Integer, Duration> tiemposDePreparacion(int id_administrador, LocalDate desde, LocalDate hasta) {
        exigirAdministrador(id_administrador);
        Map<Integer, Duration> r = new LinkedHashMap<>();
        for (Pedido p : listar()) {
            if (!enRango(Fechas.parsear(p.getFecha_creacion()), desde, hasta)) {
                continue;
            }
            LocalDateTime ini = null, fin = null;
            for (CambioEstado c : historialDeEstados(p.getId_pedido())) {
                if (Constantes.EN_PREPARACION.equals(c.getEstado())) ini = c.getFecha();
                if (Constantes.LISTO.equals(c.getEstado())) fin = c.getFecha();
            }
            if (ini != null && fin != null) {
                r.put(p.getId_pedido(), Duration.between(ini, fin));
            }
        }
        return r;
    }

    // ====================================================================
    //  Apoyo para otros servicios
    // ====================================================================

    /** true si se pueden editar productos/notas: pedido Pendiente y aún no enviado a cocina. */
    public boolean esEditable(int id_pedido) {
        Pedido p = cargar(id_pedido);
        return p != null && Constantes.PENDIENTE.equals(p.getEstado()) && !estaEnviado(id_pedido);
    }

    /** HU25: el costo de envío no se modifica en pedidos entregados, cancelados o cerrados. */
    public boolean permiteCambiarEnvio(int id_pedido) {
        Pedido p = cargar(id_pedido);
        return p != null && !Constantes.ENTREGADO.equals(p.getEstado()) && !estaFinalizado(p);
    }

    /** true si la mesa tiene un pedido que aún no se ha entregado ni cancelado. */
    public boolean tienePedidoEnCurso(int id_mesa) {
        return id_mesa > 0 && dao.contarEnCursoPorMesa(id_mesa) > 0;
    }

    /**
     * HU2 / HU25: total = suma(cantidad x precio unitario) + costo de envío (si es
     * a domicilio). Nadie puede fijarlo manualmente.
     */
    public void recalcularTotal(int id_pedido) {
        requerir(id_pedido);
        int total = new DetallePedidoDAO().sumarSubtotales(id_pedido);
        total += new DomicilioDAO().sumarCostoEnvio(id_pedido);
        dao.actualizarTotal(id_pedido, total);
    }

    // ====================================================================
    //  Privados
    // ====================================================================

    /** Lee el pedido sin cargar sus objetos relacionados. */
    private Pedido cargar(int id_pedido) {
        return dao.obtenerPorId(id_pedido);
    }

    private Pedido requerir(int id_pedido) {
        Pedido p = cargar(id_pedido);
        if (p == null) {
            throw new IllegalArgumentException("No existe el pedido " + id_pedido);
        }
        return p;
    }

    private void hidratar(Pedido p) {
        p.setMesero(new MeseroService().obtenerPorId(p.getId_mesero()));
        p.setCocina(p.getId_cocina() > 0 ? new PersonalCocinaService().obtenerPorId(p.getId_cocina()) : null);
        p.setAdministrador(p.getId_administrador() > 0
                ? new AdministradorService().obtenerPorId(p.getId_administrador()) : null);
        p.setMesa(p.getId_mesa() > 0 ? new MesaService().obtenerPorId(p.getId_mesa()) : null);
    }

    private boolean estaEnviado(int id_pedido) {
        return dao.estaEnviadoACocina(id_pedido);
    }

    private void validarNuevo(Pedido p) {
        Validar.noNulo(p, "Pedido");
        if (!TIPOS.contains(p.getTipo_pedido())) {
            throw new IllegalArgumentException("El tipo de pedido debe ser: " + TIPOS);
        }
        if (!new MeseroService().estaActivo(p.getId_mesero())) {
            throw new IllegalArgumentException("El mesero no existe o está desactivado");
        }
        if (p.getId_administrador() > 0
                && new AdministradorService().obtenerPorId(p.getId_administrador()) == null) {
            throw new IllegalArgumentException("El administrador indicado no existe");
        }
        if (Constantes.TIPO_MESA.equals(p.getTipo_pedido())) {
            Mesa m = new MesaService().obtenerPorId(p.getId_mesa());
            if (m == null) {
                throw new IllegalArgumentException("Debe seleccionar una mesa existente");
            }
            if (!Constantes.MESA_LIBRE.equals(m.getEstado())) {
                throw new IllegalArgumentException("Solo se pueden seleccionar mesas con estado 'Libre'");
            }
        } else {
            p.setNombre_cliente(Validar.texto(p.getNombre_cliente(), "nombre_cliente"));
        }
    }

    /** Cambia el estado y deja constancia de fecha y hora (RF-15). */
    private void cambiarEstado(int id_pedido, String nuevo) {
        dao.actualizarEstado(id_pedido, nuevo);
        registrar(id_pedido, nuevo);
    }

    private void registrar(int id_pedido, String estado) {
        historial.registrar(id_pedido, estado, Fechas.ahora());
    }

    private void liberarMesaSiCorresponde(int id_mesa) {
        if (id_mesa > 0 && !tienePedidoEnCurso(id_mesa)) {
            MesaService ms = new MesaService();
            if (ms.obtenerPorId(id_mesa) != null) {
                ms.cambiarEstado(id_mesa, Constantes.MESA_LIBRE);
            }
        }
    }

    private boolean estaFinalizado(Pedido p) {
        return Constantes.CANCELADO.equals(p.getEstado()) || Constantes.CERRADO.equals(p.getEstado());
    }

    private void exigirAdministrador(int id_administrador) {
        if (!new AdministradorService().estaActivo(id_administrador)) {
            throw new SecurityException("Solo el administrador puede consultar reportes");
        }
    }

    private boolean enRango(LocalDateTime fecha, LocalDate desde, LocalDate hasta) {
        if (fecha == null) {
            return false;
        }
        LocalDate d = fecha.toLocalDate();
        return (desde == null || !d.isBefore(desde)) && (hasta == null || !d.isAfter(hasta));
    }
}
