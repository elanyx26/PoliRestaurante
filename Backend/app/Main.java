package app;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import entidades.*;
import servicios.*;
import util.CambioEstado;
import util.Constantes;
// Modificado por Cami
/**
 * Demostración del flujo completo descrito en el documento (HU1 a HU26),
 * ejecutada contra la base de datos MySQL "restaurante".
 *
 * IMPORTANTE: inserta datos de ejemplo en la base. Cada ejecución usa nombres
 * únicos, así que se puede repetir. Para borrar los datos use sql/reiniciar_datos.sql.
 *
 * Ejecutar:  java -cp "out:lib/*" app.Main        (en Windows: "out;lib/*")
 */
public class Main {
    public static void main(String[] args) {
        String sufijo = String.valueOf(System.currentTimeMillis() % 1000000);
        int baseMesa = (int) (System.currentTimeMillis() % 100000) * 10;

        // ---------- Administrador: usuarios, mesas, menú (HU5, HU6, HU8, HU18) ----------
        AdministradorService adminSrv = new AdministradorService();
        Administrador admin = new Administrador();
        admin.setNombre("Ana Admin"); admin.setUsuario("admin" + sufijo); admin.setContrasena("Admin123");
        adminSrv.crear(admin);

        MeseroService meseroSrv = new MeseroService();
        Mesero mesero = new Mesero();
        mesero.setNombre("Mario Mesero"); mesero.setUsuario("mario" + sufijo); mesero.setContrasena("mesero1");
        meseroSrv.crear(mesero);

        PersonalCocinaService cocinaSrv = new PersonalCocinaService();
        PersonalCocina cocina = new PersonalCocina();
        cocina.setNombre("Carla Cocina"); cocina.setUsuario("carla" + sufijo); cocina.setContrasena("cocina1");
        cocinaSrv.crear(cocina);

        MesaService mesaSrv = new MesaService();
        Mesa[] mesas = new Mesa[3];
        for (int i = 0; i < 3; i++) {
            mesas[i] = new Mesa();
            mesas[i].setNumero(baseMesa + i + 1);
            mesas[i].setId_administrador(admin.getId_administrador());
            mesaSrv.crear(mesas[i]);
        }
        CategoriaService catSrv = new CategoriaService();
        Categoria platos = new Categoria();
        platos.setNombre("Platos fuertes " + sufijo); platos.setId_administrador(admin.getId_administrador());
        catSrv.crear(platos);

        ProductoService prodSrv = new ProductoService();
        Producto bandeja = nuevoProducto("Bandeja paisa", "Plato típico", 32000, platos, admin);
        Producto jugo = nuevoProducto("Jugo natural", "Jugo de fruta", 8000, platos, admin);
        prodSrv.crear(bandeja);
        prodSrv.crear(jugo);

        // ---------- Inicio de sesión (HU14) ----------
        AutenticacionService auth = new AutenticacionService();
        System.out.println("Login: " + auth.iniciarSesion(mesero.getUsuario(), "mesero1"));
        try { auth.iniciarSesion(mesero.getUsuario(), "mala"); }
        catch (IllegalArgumentException e) { System.out.println("Login inválido -> " + e.getMessage()); }

        PedidoService pedSrv = new PedidoService();
        int idMesero = mesero.getId_mesero();
        int idCocina = cocina.getId_cocina();
        int idMesa1 = mesas[0].getId_mesa();

        // ---------- Pedido en mesa (HU1, HU2, HU3, HU4, HU9, HU16, HU23) ----------
        Pedido p1 = nuevoPedido(idMesero, Constantes.TIPO_MESA, idMesa1, null);
        pedSrv.crearPedido(p1, Arrays.asList(detalle(bandeja, 2, "sin cebolla"), detalle(jugo, 1, null)));
        System.out.println("\n" + pedSrv.resumenPedido(p1.getId_pedido()));
        System.out.println("Mesa 1: " + mesaSrv.obtenerPorId(idMesa1).getEstado());

        try { pedSrv.crear(nuevoPedido(idMesero, Constantes.TIPO_MESA, idMesa1, null)); }
        catch (IllegalArgumentException e) { System.out.println("Mesa ocupada -> " + e.getMessage()); }
        try { pedSrv.crearPedido(nuevoPedido(idMesero, Constantes.TIPO_MESA, mesas[1].getId_mesa(), null),
                Arrays.<DetallePedido>asList()); }
        catch (IllegalArgumentException e) { System.out.println("Sin productos -> " + e.getMessage()); }

        pedSrv.enviarACocina(p1.getId_pedido());
        try { new DetallePedidoService().crear(detalle(jugo, 1, null, p1.getId_pedido())); }
        catch (IllegalStateException e) { System.out.println("Editar tras enviar -> " + e.getMessage()); }

        // ---------- Pedido para llevar (HU21, HU22) ----------
        Pedido p2 = nuevoPedido(idMesero, Constantes.TIPO_LLEVAR, 0, "Luis Pérez");
        pedSrv.crearPedido(p2, Arrays.asList(detalle(jugo, 3, "sin hielo")));
        pedSrv.enviarACocina(p2.getId_pedido());

        // ---------- Pedido a domicilio (HU24, HU25, HU26) ----------
        Pedido p3 = nuevoPedido(idMesero, Constantes.TIPO_DOMICILIO, 0, "Marta Gómez");
        Domicilio dom = new Domicilio();
        dom.setDireccion("Cra 7 # 45-10"); dom.setTelefono("3001234567"); dom.setCosto_envio(5000);
        pedSrv.crearPedido(p3, Arrays.asList(detalle(bandeja, 1, null)), dom);
        System.out.println("\n" + pedSrv.resumenPedido(p3.getId_pedido()));
        pedSrv.enviarACocina(p3.getId_pedido());

        // ---------- Cola de cocina (HU13) ----------
        System.out.println("\nCola de cocina (solo los pedidos de esta demo van al final):");
        for (Pedido p : pedSrv.colaCocina()) {
            System.out.println("  #" + p.getId_pedido() + " " + pedSrv.descripcionParaCocina(p.getId_pedido()));
        }
        Domicilio visto = new DomicilioService().consultarDatosEntrega(p3.getId_pedido(), Constantes.ROL_COCINA);
        System.out.println("Cocina ve la dirección del domicilio? -> " + visto.getDireccion());

        // ---------- Estados (HU4) ----------
        pedSrv.iniciarPreparacion(p1.getId_pedido(), idCocina);
        try { pedSrv.entregar(p1.getId_pedido(), idMesero, true); }
        catch (IllegalStateException e) { System.out.println("\nEntregar sin estar listo -> " + e.getMessage()); }
        try { Thread.sleep(30); } catch (InterruptedException ignored) { }
        pedSrv.marcarListo(p1.getId_pedido(), idCocina);
        pedSrv.entregar(p1.getId_pedido(), idMesero, true);
        System.out.println("Mesa 1 tras entregar: " + mesaSrv.obtenerPorId(idMesa1).getEstado());
        try { pedSrv.cancelar(p1.getId_pedido(), idMesero); }
        catch (IllegalStateException e) { System.out.println("Cancelar entregado -> " + e.getMessage()); }
        pedSrv.cerrar(p1.getId_pedido(), "Efectivo");

        // ---------- Cancelar (HU15) ----------
        Pedido cancelado = pedSrv.cancelar(p2.getId_pedido(), idMesero);
        System.out.println("Pedido #" + cancelado.getId_pedido() + " -> " + cancelado.getEstado());

        // Domicilio completo y cierre
        pedSrv.iniciarPreparacion(p3.getId_pedido(), idCocina);
        pedSrv.marcarListo(p3.getId_pedido(), idCocina);
        pedSrv.entregar(p3.getId_pedido(), idMesero, false);
        pedSrv.cerrar(p3.getId_pedido(), "Tarjeta");

        // ---------- Historial, activos, reportes (HU7, HU10, HU17, HU20) ----------
        System.out.println("\nHistorial de estados del pedido #" + p1.getId_pedido() + ":");
        for (CambioEstado c : pedSrv.historialDeEstados(p1.getId_pedido())) System.out.println("  " + c);
        System.out.println("Historial mesa 1: " + pedSrv.historialDeMesa(idMesa1).size() + " pedido(s)");
        System.out.println("Activos del mesero: " + pedSrv.consultarActivosDeMesero(idMesero).size());
        System.out.println("Mesas libres/ocupadas (toda la BD): " + mesaSrv.contarLibres() + "/" + mesaSrv.contarOcupadas());
        System.out.println("Reporte de ventas (toda la BD): "
                + pedSrv.reporteVentas(admin.getId_administrador(), null, null));
        Map<Integer, Duration> tiempos = pedSrv.tiemposDePreparacion(admin.getId_administrador(), null, null);
        for (int id : new int[] {p1.getId_pedido(), p3.getId_pedido()}) {
            System.out.println("Preparación pedido #" + id + ": " + tiempos.get(id).toMillis() + " ms");
        }
        try { pedSrv.reporteVentas(idMesero, null, null); }
        catch (SecurityException e) { System.out.println("Reporte como mesero -> " + e.getMessage()); }

        // ---------- Roles y usuarios (HU8, HU19) ----------
        meseroSrv.desactivar(idMesero);
        try { auth.iniciarSesion(mesero.getUsuario(), "mesero1"); }
        catch (IllegalStateException e) { System.out.println("\nUsuario desactivado -> " + e.getMessage()); }
        meseroSrv.activar(idMesero);
        adminSrv.asignarRol(admin.getId_administrador(), cocina.getUsuario(), Constantes.ROL_MESERO);
        System.out.println("Carla ahora inicia sesión como: " + auth.iniciarSesion(cocina.getUsuario(), "cocina1"));
    }

    private static Producto nuevoProducto(String n, String d, int precio, Categoria c, Administrador a) {
        Producto p = new Producto();
        p.setNombre(n); p.setDescripcion(d); p.setPrecio(precio);
        p.setId_categoria(c.getId_categoria()); p.setId_administrador(a.getId_administrador());
        return p;
    }

    private static DetallePedido detalle(Producto prod, int cantidad, String nota) {
        return detalle(prod, cantidad, nota, 0);
    }

    private static DetallePedido detalle(Producto prod, int cantidad, String nota, int idPedido) {
        DetallePedido d = new DetallePedido();
        d.setId_producto(prod.getId_producto()); d.setCantidad(cantidad); d.setNotas(nota); d.setId_pedido(idPedido);
        return d;
    }

    private static Pedido nuevoPedido(int idMesero, String tipo, int idMesa, String cliente) {
        Pedido p = new Pedido();
        p.setId_mesero(idMesero); p.setTipo_pedido(tipo); p.setId_mesa(idMesa); p.setNombre_cliente(cliente);
        return p;
    }
}
