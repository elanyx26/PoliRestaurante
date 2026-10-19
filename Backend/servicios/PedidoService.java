package servicios;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import entidades.DetallePedido;
import entidades.Domicilio;
import entidades.Mesa;
import entidades.Pedido;
import entidades.Producto;
import util.Bd;
import util.CambioEstado;
import util.Constantes;
import util.Fechas;
import util.ReporteVentas;
import util.Validar;

/**
 * Ciclo de vida del pedido (tablas Pedido e HistorialEstado).
 * HU1 a HU4, HU9 a HU11, HU13, HU15 a HU17, HU20 a HU26.
 *
 * Mientras un pedido no se envíe a cocina (enviarACocina) se pueden agregar,
 * editar o quitar sus productos y notas.
 */
public class PedidoService {
    private static final String[] COLS = { "id_mesero", "id_cocina", "id_administrador", "id_mesa",
            "tipo_pedido", "nombre_cliente", "estado", "total", "metodo_pago", "fecha_creacion", "fecha_cierre" };

    private static final String EN_CURSO = "('" + Constantes.PENDIENTE + "','" + Constantes.EN_PREPARACION
            + "','" + Constantes.LISTO + "')";

    private static final List<String> TIPOS = Arrays.asList(
            Constantes.TIPO_MESA, Constantes.TIPO_LLEVAR, Constantes.TIPO_DOMICILIO);

    private static Pedido mapear(ResultSet rs) throws SQLException {
        Pedido p = new Pedido();
        p.setId_pedido(rs.getInt("id_pedido"));
        p.setId_mesero(rs.getInt("id_mesero"));
        p.setId_cocina(rs.getInt("id_cocina"));
        p.setId_administrador(rs.getInt("id_administrador"));
        p.setId_mesa(rs.getInt("id_mesa"));
        p.setTipo_pedido(rs.getString("tipo_pedido"));
        p.setNombre_cliente(rs.getString("nombre_cliente"));
        p.setEstado(rs.getString("estado"));
        p.setTotal(rs.getDouble("total")); // Actualizado a double para DECIMAL
        p.setMetodo_pago(rs.getString("metodo_pago"));
        p.setFecha_creacion(rs.getString("fecha_creacion"));
        p.setFecha_cierre(rs.getString("fecha_cierre"));
        return p;
    }

    private static Object[] valores(Pedido p) {
        return new Object[] { Bd.fk(p.getId_mesero()), Bd.fk(p.getId_cocina()), Bd.fk(p.getId_administrador()),
                Bd.fk(p.getId_mesa()), p.getTipo_pedido(), p.getNombre_cliente(), p.getEstado(), p.getTotal(),
                p.getMetodo_pago(), p.getFecha_creacion(), p.getFecha_cierre() };
    }
    // CRUD

    public Pedido crear(Pedido entidad) {
        validarNuevo(entidad);
        boolean enMesa = Constantes.TIPO_MESA.equals(entidad.getTipo_pedido());
        if (!enMesa) {
            entidad.setId_mesa(0);
        }
        entidad.setId_cocina(0);
        entidad.setEstado(Constantes.PENDIENTE);
        entidad.setTotal(0.00);
        entidad.setMetodo_pago(null);
        entidad.setFecha_creacion(Fechas.ahora());
        entidad.setFecha_cierre(null);

        entidad.setId_pedido(Bd.insertar("Pedido", "id_pedido", entidad.getId_pedido(), COLS, valores(entidad)));
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
        return Bd.consultar("SELECT * FROM Pedido ORDER BY id_pedido", PedidoService::mapear);
    }

    public Pedido actualizar(Pedido entidad) {
        Validar.noNulo(entidad, "Pedido");
        Pedido existente = requerir(entidad.getId_pedido());
        if (estaFinalizado(existente) || Constantes.ENTREGADO.equals(existente.getEstado())) {
            throw new IllegalStateException(
                    "Un pedido " + existente.getEstado().toLowerCase() + " no se puede modificar");
        }
        String nombre = entidad.getNombre_cliente();
        if (!Constantes.TIPO_MESA.equals(existente.getTipo_pedido())) {
            nombre = Validar.texto(nombre, "nombre_cliente");
        }
        Bd.ejecutar("UPDATE Pedido SET nombre_cliente = ? WHERE id_pedido = ?", nombre, existente.getId_pedido());
        return obtenerPorId(existente.getId_pedido());
    }

    public void eliminar(int id_pedido) {
        Pedido p = cargar(id_pedido);
        if (p == null) {
            return;
        }
        if (!esEditable(id_pedido)) {
            throw new IllegalStateException(
                    "El pedido ya fue enviado a cocina o finalizó; cancélelo en lugar de eliminarlo");
        }
        new DetallePedidoService().eliminarPorPedido(id_pedido);
        new DomicilioService().eliminarPorPedido(id_pedido);
        Bd.ejecutar("DELETE FROM HistorialEstado WHERE id_pedido = ?", id_pedido);
        Bd.eliminar("Pedido", "id_pedido", id_pedido);
        liberarMesaSiCorresponde(p.getId_mesa());
    }

    // Creación completa (HU1, HU21, HU24)

    public Pedido crearPedido(Pedido pedido, List<DetallePedido> detalles) {
        return crearPedido(pedido, detalles, null);
    }

    public Pedido crearPedido(Pedido pedido, List<DetallePedido> detalles, Domicilio domicilio) {
        Validar.noNulo(pedido, "Pedido");
        if (detalles == null || detalles.isEmpty()) {
            throw new IllegalArgumentException("El pedido no se puede crear sin al menos un producto");
        }
        if (Constantes.TIPO_DOMICILIO.equals(pedido.getTipo_pedido())) {
            if (domicilio == null) {
                throw new IllegalArgumentException(
                        "El pedido a domicilio requiere dirección, teléfono y costo de envío");
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
            eliminar(pedido.getId_pedido());
            throw e;
        }
        pedido.setTotal(cargar(pedido.getId_pedido()).getTotal());
        return pedido;
    }

    // Flujo del pedido

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
        Bd.ejecutar("UPDATE Pedido SET enviado_cocina = 1 WHERE id_pedido = ?", id_pedido);
        return obtenerPorId(id_pedido);
    }

    public Pedido iniciarPreparacion(int id_pedido, int id_cocina) {
        if (!new PersonalCocinaService().estaActivo(id_cocina)) {
            throw new IllegalArgumentException("El usuario de cocina no existe o está desactivado");
        }
        Pedido p = requerir(id_pedido);
        if (!Constantes.PENDIENTE.equals(p.getEstado()) || !estaEnviado(id_pedido)) {
            throw new IllegalStateException("Solo un pedido Pendiente enviado a cocina puede pasar a En preparación");
        }
        Bd.ejecutar("UPDATE Pedido SET id_cocina = ? WHERE id_pedido = ?", id_cocina, id_pedido);
        cambiarEstado(id_pedido, Constantes.EN_PREPARACION);
        return obtenerPorId(id_pedido);
    }

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

    public Pedido cerrar(int id_pedido, String metodo_pago) {
        Pedido p = requerir(id_pedido);
        String metodo = Validar.texto(metodo_pago, "metodo_pago");
        if (!Constantes.ENTREGADO.equals(p.getEstado())) {
            throw new IllegalStateException("Solo se puede cerrar un pedido que ya fue entregado");
        }
        Bd.ejecutar("UPDATE Pedido SET metodo_pago = ?, fecha_cierre = ? WHERE id_pedido = ?",
                metodo, Fechas.ahora(), id_pedido);
        cambiarEstado(id_pedido, Constantes.CERRADO);
        liberarMesaSiCorresponde(p.getId_mesa());
        return obtenerPorId(id_pedido);
    }

    // Consultas

    public List<Pedido> colaCocina() {
        return Bd.consultar("SELECT * FROM Pedido WHERE estado = ? AND enviado_cocina = 1 "
                + "ORDER BY fecha_creacion, id_pedido", PedidoService::mapear, Constantes.PENDIENTE);
    }

    public List<Pedido> consultarActivosDeMesero(int id_mesero) {
        return Bd.consultar("SELECT * FROM Pedido WHERE id_mesero = ? AND estado IN " + EN_CURSO
                + " ORDER BY fecha_creacion, id_pedido", PedidoService::mapear, id_mesero);
    }

    public List<Pedido> historialDeMesa(int id_mesa) {
        return Bd.consultar("SELECT * FROM Pedido WHERE id_mesa = ? ORDER BY fecha_creacion DESC, id_pedido DESC",
                PedidoService::mapear, id_mesa);
    }

    public List<CambioEstado> historialDeEstados(int id_pedido) {
        return Bd.consultar("SELECT * FROM HistorialEstado WHERE id_pedido = ? ORDER BY id_historial",
                rs -> new CambioEstado(rs.getInt("id_pedido"), rs.getString("estado"),
                        Fechas.parsear(rs.getString("fecha"))),
                id_pedido);
    }

    public String descripcionParaCocina(int id_pedido) {
        Pedido p = requerir(id_pedido);
        if (Constantes.TIPO_MESA.equals(p.getTipo_pedido())) {
            Mesa m = new MesaService().obtenerPorId(p.getId_mesa());
            return "[En mesa] Mesa " + (m == null ? p.getId_mesa() : m.getNumero());
        }
        return "[" + p.getTipo_pedido() + "] Cliente: " + p.getNombre_cliente();
    }

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

    // Reportes (solo administrador)

    public ReporteVentas reporteVentas(int id_administrador, LocalDate desde, LocalDate hasta) {
        exigirAdministrador(id_administrador);
        int cantidad = 0;
        double ventas = 0.0, envios = 0.0;
        DomicilioService ds = new DomicilioService();
        List<Pedido> cerrados = Bd.consultar("SELECT * FROM Pedido WHERE estado = ?", PedidoService::mapear,
                Constantes.CERRADO);
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

    public Map<Integer, Duration> tiemposDePreparacion(int id_administrador, LocalDate desde, LocalDate hasta) {
        exigirAdministrador(id_administrador);
        Map<Integer, Duration> r = new LinkedHashMap<>();
        for (Pedido p : listar()) {
            if (!enRango(Fechas.parsear(p.getFecha_creacion()), desde, hasta)) {
                continue;
            }
            LocalDateTime ini = null, fin = null;
            for (CambioEstado c : historialDeEstados(p.getId_pedido())) {
                if (Constantes.EN_PREPARACION.equals(c.getEstado()))
                    ini = c.getFecha();
                if (Constantes.LISTO.equals(c.getEstado()))
                    fin = c.getFecha();
            }
            if (ini != null && fin != null) {
                r.put(p.getId_pedido(), Duration.between(ini, fin));
            }
        }
        return r;
    }

    public boolean esEditable(int id_pedido) {
        Pedido p = cargar(id_pedido);
        return p != null && Constantes.PENDIENTE.equals(p.getEstado()) && !estaEnviado(id_pedido);
    }

    public boolean permiteCambiarEnvio(int id_pedido) {
        Pedido p = cargar(id_pedido);
        return p != null && !Constantes.ENTREGADO.equals(p.getEstado()) && !estaFinalizado(p);
    }

    public boolean tienePedidoEnCurso(int id_mesa) {
        return id_mesa > 0 && Bd.entero("SELECT COUNT(*) FROM Pedido WHERE id_mesa = ? AND estado IN " + EN_CURSO,
                id_mesa) > 0;
    }

    public void recalcularTotal(int id_pedido) {
        requerir(id_pedido);
        double total = Bd.real(
                "SELECT COALESCE(SUM(cantidad * precio_unitario), 0) FROM DetallePedido WHERE id_pedido = ?",
                id_pedido);
        total += Bd.real("SELECT COALESCE(SUM(costo_envio), 0) FROM Domicilio WHERE id_pedido = ?", id_pedido);
        Bd.ejecutar("UPDATE Pedido SET total = ? WHERE id_pedido = ?", total, id_pedido);
    }

    private Pedido cargar(int id_pedido) {
        return Bd.uno("SELECT * FROM Pedido WHERE id_pedido = ?", PedidoService::mapear, id_pedido);
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
                ? new AdministradorService().obtenerPorId(p.getId_administrador())
                : null);
        p.setMesa(p.getId_mesa() > 0 ? new MesaService().obtenerPorId(p.getId_mesa()) : null);
    }

    private boolean estaEnviado(int id_pedido) {
        return Bd.entero("SELECT enviado_cocina FROM Pedido WHERE id_pedido = ?", id_pedido) == 1;
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

    private void cambiarEstado(int id_pedido, String nuevo) {
        Bd.ejecutar("UPDATE Pedido SET estado = ? WHERE id_pedido = ?", nuevo, id_pedido);
        registrar(id_pedido, nuevo);
    }

    private void registrar(int id_pedido, String estado) {
        Bd.ejecutar("INSERT INTO HistorialEstado (id_pedido, estado, fecha) VALUES (?, ?, ?)",
                id_pedido, estado, Fechas.ahora());
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