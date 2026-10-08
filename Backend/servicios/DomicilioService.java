package servicios;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import entidades.Domicilio;
import entidades.Pedido;
import util.Bd;
import util.Constantes;
import util.Validar;

/**
 * HU24, HU25, HU26 / RF-19, RF-20, RF-21, RNF-11: datos de entrega de un
 * pedido a domicilio (tabla Domicilio, relación 1 a 1 con Pedido). El costo
 * de envío se suma automáticamente al total del pedido.
 */
public class DomicilioService {
    private static final String[] COLS = {"id_pedido", "direccion", "telefono", "costo_envio"};

    private static Domicilio mapear(ResultSet rs) throws SQLException {
        Domicilio d = new Domicilio();
        d.setId_domicilio(rs.getInt("id_domicilio"));
        d.setId_pedido(rs.getInt("id_pedido"));
        d.setDireccion(rs.getString("direccion"));
        d.setTelefono(rs.getString("telefono"));
        d.setCosto_envio(rs.getInt("costo_envio"));
        return d;
    }

    private static Object[] valores(Domicilio d) {
        return new Object[] {d.getId_pedido(), d.getDireccion(), d.getTelefono(), d.getCosto_envio()};
    }

    public Domicilio crear(Domicilio entidad) {
        Validar.noNulo(entidad, "Domicilio");
        Pedido pedido = pedidoDomicilio(entidad.getId_pedido());
        validarCampos(entidad);
        if (obtenerPorPedido(entidad.getId_pedido()) != null) {
            throw new IllegalArgumentException("El pedido ya tiene datos de entrega (relación única)");
        }
        entidad.setId_domicilio(Bd.insertar("Domicilio", "id_domicilio", entidad.getId_domicilio(), COLS, valores(entidad)));
        entidad.setPedido(pedido);
        new PedidoService().recalcularTotal(entidad.getId_pedido());
        return entidad;
    }

    public Domicilio obtenerPorId(int id_domicilio) {
        Domicilio d = Bd.uno("SELECT * FROM Domicilio WHERE id_domicilio = ?", DomicilioService::mapear, id_domicilio);
        if (d != null) {
            d.setPedido(new PedidoService().obtenerPorId(d.getId_pedido()));
        }
        return d;
    }

    public List<Domicilio> listar() {
        return Bd.consultar("SELECT * FROM Domicilio ORDER BY id_domicilio", DomicilioService::mapear);
    }

    /** No se puede modificar el envío de un pedido entregado, cancelado o cerrado (HU25). */
    public Domicilio actualizar(Domicilio entidad) {
        Validar.noNulo(entidad, "Domicilio");
        Domicilio existente = Bd.uno("SELECT * FROM Domicilio WHERE id_domicilio = ?",
                DomicilioService::mapear, entidad.getId_domicilio());
        if (existente == null) {
            throw new IllegalArgumentException("No existe un domicilio con id " + entidad.getId_domicilio());
        }
        entidad.setId_pedido(existente.getId_pedido());   // el pedido asociado no cambia
        Pedido pedido = pedidoDomicilio(existente.getId_pedido());
        validarCampos(entidad);
        Bd.actualizar("Domicilio", "id_domicilio", entidad.getId_domicilio(), COLS, valores(entidad));
        entidad.setPedido(pedido);
        new PedidoService().recalcularTotal(entidad.getId_pedido());
        return entidad;
    }

    /** Solo se pueden quitar los datos de entrega de un pedido que aún no se envió a cocina. */
    public void eliminar(int id_domicilio) {
        Domicilio d = Bd.uno("SELECT * FROM Domicilio WHERE id_domicilio = ?", DomicilioService::mapear, id_domicilio);
        if (d == null) {
            return;
        }
        if (!new PedidoService().esEditable(d.getId_pedido())) {
            throw new IllegalStateException("El pedido ya fue enviado a cocina; no se pueden quitar los datos de entrega");
        }
        Bd.eliminar("Domicilio", "id_domicilio", id_domicilio);
        new PedidoService().recalcularTotal(d.getId_pedido());
    }

    // ---------------------------------------------------------------- extras

    /** Domicilio asociado a un pedido, o null si no tiene. */
    public Domicilio obtenerPorPedido(int id_pedido) {
        return Bd.uno("SELECT * FROM Domicilio WHERE id_pedido = ?", DomicilioService::mapear, id_pedido);
    }

    /**
     * RNF-11: dirección y teléfono son datos personales. El mesero y el
     * administrador ven los datos completos; cocina solo recibe el costo de
     * envío (para identificar el pedido a domicilio no necesita dirección ni teléfono).
     */
    public Domicilio consultarDatosEntrega(int id_pedido, String rol) {
        Domicilio d = obtenerPorPedido(id_pedido);
        if (d == null) {
            return null;
        }
        if (Constantes.ROL_COCINA.equals(rol)) {
            Domicilio parcial = new Domicilio();
            parcial.setId_domicilio(d.getId_domicilio());
            parcial.setId_pedido(d.getId_pedido());
            parcial.setCosto_envio(d.getCosto_envio());
            return parcial;
        }
        return d;
    }

    /** Uso interno: al borrar un pedido borrador se eliminan sus datos de entrega. */
    void eliminarPorPedido(int id_pedido) {
        Bd.ejecutar("DELETE FROM Domicilio WHERE id_pedido = ?", id_pedido);
    }

    // --------------------------------------------------------------- privados

    private Pedido pedidoDomicilio(int id_pedido) {
        PedidoService ps = new PedidoService();
        Pedido p = ps.obtenerPorId(id_pedido);
        if (p == null) {
            throw new IllegalArgumentException("No existe el pedido " + id_pedido);
        }
        if (!Constantes.TIPO_DOMICILIO.equals(p.getTipo_pedido())) {
            throw new IllegalArgumentException("Solo los pedidos de tipo 'A domicilio' tienen datos de entrega");
        }
        if (!ps.permiteCambiarEnvio(id_pedido)) {
            throw new IllegalStateException("No se puede modificar el envío de un pedido entregado, cancelado o cerrado");
        }
        return p;
    }

    private void validarCampos(Domicilio d) {
        d.setDireccion(Validar.texto(d.getDireccion(), "direccion"));
        d.setTelefono(Validar.texto(d.getTelefono(), "telefono"));
        if (!d.getTelefono().matches("^\\+?[0-9 ()-]{7,20}$")) {
            throw new IllegalArgumentException("El teléfono de contacto no es válido");
        }
        Validar.noNegativo(d.getCosto_envio(), "costo_envio");
    }
}
