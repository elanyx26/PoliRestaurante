package app;

import entidades.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import servicios.*;
import util.Constantes;

/**
 * CRUD por consola: crear, listar, actualizar y eliminar registros
 * usando los servicios que ya existen (con todas sus validaciones).
 *
 * Ejecutar:  java -cp "out:lib/*" app.MenuConsola      (Windows: "out;lib/*")
 */
public class MenuConsola {

    private static final Scanner in = new Scanner(System.in);

    private static final AdministradorService adminSrv = new AdministradorService();
    private static final MeseroService meseroSrv = new MeseroService();
    private static final PersonalCocinaService cocinaSrv = new PersonalCocinaService();
    private static final MesaService mesaSrv = new MesaService();
    private static final CategoriaService catSrv = new CategoriaService();
    private static final ProductoService prodSrv = new ProductoService();
    private static final PedidoService pedSrv = new PedidoService();

    public static void main(String[] args) {
        while (true) {
            System.out.println("\n===== RESTAURANTE - MENÚ PRINCIPAL =====");
            System.out.println("1. Administradores");
            System.out.println("2. Meseros");
            System.out.println("3. Personal de cocina");
            System.out.println("4. Mesas");
            System.out.println("5. Categorías");
            System.out.println("6. Productos");
            System.out.println("7. Pedidos (crear / ver)");
            System.out.println("0. Salir");
            int op = leerEntero("Opción: ");
            switch (op) {
                case 1: submenu("Administradores", MenuConsola::crearAdmin, MenuConsola::listarAdmin,
                        MenuConsola::actualizarAdmin, MenuConsola::eliminarAdmin); break;
                case 2: submenu("Meseros", MenuConsola::crearMesero, MenuConsola::listarMeseros,
                        MenuConsola::actualizarMesero, MenuConsola::eliminarMesero); break;
                case 3: submenu("Personal de cocina", MenuConsola::crearCocina, MenuConsola::listarCocina,
                        MenuConsola::actualizarCocina, MenuConsola::eliminarCocina); break;
                case 4: submenu("Mesas", MenuConsola::crearMesa, MenuConsola::listarMesas,
                        MenuConsola::actualizarMesa, MenuConsola::eliminarMesa); break;
                case 5: submenu("Categorías", MenuConsola::crearCategoria, MenuConsola::listarCategorias,
                        MenuConsola::actualizarCategoria, MenuConsola::eliminarCategoria); break;
                case 6: submenu("Productos", MenuConsola::crearProducto, MenuConsola::listarProductos,
                        MenuConsola::actualizarProducto, MenuConsola::eliminarProducto); break;
                case 7: menuPedidos(); break;
                case 0: System.out.println("Hasta luego."); return;
                default: System.out.println("Opción no válida.");
            }
        }
    }

    // ------------------------------------------------------------ infraestructura

    private static void submenu(String titulo, Runnable crear, Runnable listar,
                                Runnable actualizar, Runnable eliminar) {
        while (true) {
            System.out.println("\n--- " + titulo + " ---");
            System.out.println("1. Crear");
            System.out.println("2. Listar");
            System.out.println("3. Actualizar");
            System.out.println("4. Eliminar");
            System.out.println("0. Volver");
            int op = leerEntero("Opción: ");
            if (op == 0) return;
            Runnable accion = op == 1 ? crear : op == 2 ? listar : op == 3 ? actualizar : op == 4 ? eliminar : null;
            if (accion == null) { System.out.println("Opción no válida."); continue; }
            ejecutar(accion);
        }
    }

    /** Captura los errores de validación / BD para que el programa no se caiga. */
    private static void ejecutar(Runnable accion) {
        try {
            accion.run();
        } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
            System.out.println("[!] " + e.getMessage());
        } catch (RuntimeException e) {
            System.out.println("[!] Error inesperado: " + e.getMessage());
        }
    }

    private static String leerTexto(String msg) {
        System.out.print(msg);
        return in.nextLine().trim();
    }

    private static int leerEntero(String msg) {
        while (true) {
            System.out.print(msg);
            String s = in.nextLine().trim();
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                System.out.println("Escribe un número entero válido.");
            }
        }
    }

    /** Al actualizar: si el usuario solo presiona Enter se conserva el valor actual. */
    private static String leerTextoOpcional(String campo, String actual) {
        String s = leerTexto(campo + " [" + (actual == null ? "" : actual) + "]: ");
        return s.isEmpty() ? actual : s;
    }

    private static int leerEnteroOpcional(String campo, int actual) {
        while (true) {
            String s = leerTexto(campo + " [" + actual + "]: ");
            if (s.isEmpty()) return actual;
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                System.out.println("Escribe un número entero válido.");
            }
        }
    }

    private static void ok(String msg) { System.out.println("[OK] " + msg); }

    // ------------------------------------------------------------ Administrador

    private static void crearAdmin() {
        Administrador a = new Administrador();
        a.setNombre(leerTexto("Nombre: "));
        a.setUsuario(leerTexto("Usuario: "));
        a.setContrasena(leerTexto("Contraseña: "));
        adminSrv.crear(a);
        ok("Administrador creado con id " + a.getId_administrador());
    }

    private static void listarAdmin() {
        List<Administrador> lista = adminSrv.listar();
        if (lista.isEmpty()) System.out.println("(sin registros)");
        for (Administrador a : lista) {
            System.out.printf("%d | %s | usuario: %s | %s%n", a.getId_administrador(), a.getNombre(),
                    a.getUsuario(), adminSrv.estaActivo(a.getId_administrador()) ? "activo" : "inactivo");
        }
    }

    private static void actualizarAdmin() {
        Administrador a = adminSrv.obtenerPorId(leerEntero("Id del administrador: "));
        if (a == null) { System.out.println("No existe."); return; }
        a.setNombre(leerTextoOpcional("Nombre", a.getNombre()));
        a.setUsuario(leerTextoOpcional("Usuario", a.getUsuario()));
        a.setContrasena(leerTexto("Nueva contraseña (Enter = conservar): "));
        adminSrv.actualizar(a);
        ok("Administrador actualizado.");
    }

    private static void eliminarAdmin() {
        adminSrv.eliminar(leerEntero("Id del administrador a eliminar: "));
        ok("Operación realizada.");
    }

    // ------------------------------------------------------------ Mesero

    private static void crearMesero() {
        Mesero m = new Mesero();
        m.setNombre(leerTexto("Nombre: "));
        m.setUsuario(leerTexto("Usuario: "));
        m.setContrasena(leerTexto("Contraseña: "));
        meseroSrv.crear(m);
        ok("Mesero creado con id " + m.getId_mesero());
    }

    private static void listarMeseros() {
        List<Mesero> lista = meseroSrv.listar();
        if (lista.isEmpty()) System.out.println("(sin registros)");
        for (Mesero m : lista) {
            System.out.printf("%d | %s | usuario: %s | %s%n", m.getId_mesero(), m.getNombre(),
                    m.getUsuario(), meseroSrv.estaActivo(m.getId_mesero()) ? "activo" : "inactivo");
        }
    }

    private static void actualizarMesero() {
        Mesero m = meseroSrv.obtenerPorId(leerEntero("Id del mesero: "));
        if (m == null) { System.out.println("No existe."); return; }
        m.setNombre(leerTextoOpcional("Nombre", m.getNombre()));
        m.setUsuario(leerTextoOpcional("Usuario", m.getUsuario()));
        m.setContrasena(leerTexto("Nueva contraseña (Enter = conservar): "));
        meseroSrv.actualizar(m);
        ok("Mesero actualizado.");
    }

    private static void eliminarMesero() {
        meseroSrv.eliminar(leerEntero("Id del mesero a eliminar: "));
        ok("Operación realizada.");
    }

    // ------------------------------------------------------------ Personal de cocina

    private static void crearCocina() {
        PersonalCocina c = new PersonalCocina();
        c.setNombre(leerTexto("Nombre: "));
        c.setUsuario(leerTexto("Usuario: "));
        c.setContrasena(leerTexto("Contraseña: "));
        cocinaSrv.crear(c);
        ok("Personal de cocina creado con id " + c.getId_cocina());
    }

    private static void listarCocina() {
        List<PersonalCocina> lista = cocinaSrv.listar();
        if (lista.isEmpty()) System.out.println("(sin registros)");
        for (PersonalCocina c : lista) {
            System.out.printf("%d | %s | usuario: %s | %s%n", c.getId_cocina(), c.getNombre(),
                    c.getUsuario(), cocinaSrv.estaActivo(c.getId_cocina()) ? "activo" : "inactivo");
        }
    }

    private static void actualizarCocina() {
        PersonalCocina c = cocinaSrv.obtenerPorId(leerEntero("Id del personal de cocina: "));
        if (c == null) { System.out.println("No existe."); return; }
        c.setNombre(leerTextoOpcional("Nombre", c.getNombre()));
        c.setUsuario(leerTextoOpcional("Usuario", c.getUsuario()));
        c.setContrasena(leerTexto("Nueva contraseña (Enter = conservar): "));
        cocinaSrv.actualizar(c);
        ok("Personal de cocina actualizado.");
    }

    private static void eliminarCocina() {
        cocinaSrv.eliminar(leerEntero("Id del personal de cocina a eliminar: "));
        ok("Operación realizada.");
    }

    // ------------------------------------------------------------ Mesa

    private static void crearMesa() {
        Mesa m = new Mesa();
        m.setNumero(leerEntero("Número de mesa: "));
        m.setId_administrador(leerEntero("Id del administrador que la registra: "));
        // el estado queda en "Libre" por defecto (lo asigna MesaService)
        mesaSrv.crear(m);
        ok("Mesa creada con id " + m.getId_mesa());
    }

    private static void listarMesas() {
        List<Mesa> lista = mesaSrv.listar();
        if (lista.isEmpty()) System.out.println("(sin registros)");
        for (Mesa m : lista) {
            System.out.printf("%d | mesa #%d | %s | admin: %d%n", m.getId_mesa(), m.getNumero(),
                    m.getEstado(), m.getId_administrador());
        }
    }

    private static void actualizarMesa() {
        Mesa m = mesaSrv.obtenerPorId(leerEntero("Id de la mesa: "));
        if (m == null) { System.out.println("No existe."); return; }
        m.setNumero(leerEnteroOpcional("Número", m.getNumero()));
        String estado = leerTextoOpcional("Estado (" + Constantes.MESA_LIBRE + "/" + Constantes.MESA_OCUPADA + ")",
                m.getEstado());
        m.setEstado(estado);
        mesaSrv.actualizar(m);
        ok("Mesa actualizada.");
    }

    private static void eliminarMesa() {
        mesaSrv.eliminar(leerEntero("Id de la mesa a eliminar: "));
        ok("Operación realizada.");
    }

    // ------------------------------------------------------------ Categoría

    private static void crearCategoria() {
        Categoria c = new Categoria();
        c.setNombre(leerTexto("Nombre de la categoría: "));
        c.setId_administrador(leerEntero("Id del administrador que la registra: "));
        catSrv.crear(c);
        ok("Categoría creada con id " + c.getId_categoria());
    }

    private static void listarCategorias() {
        List<Categoria> lista = catSrv.listar();
        if (lista.isEmpty()) System.out.println("(sin registros)");
        for (Categoria c : lista) {
            System.out.printf("%d | %s | admin: %d%n", c.getId_categoria(), c.getNombre(), c.getId_administrador());
        }
    }

    private static void actualizarCategoria() {
        Categoria c = catSrv.obtenerPorId(leerEntero("Id de la categoría: "));
        if (c == null) { System.out.println("No existe."); return; }
        c.setNombre(leerTextoOpcional("Nombre", c.getNombre()));
        catSrv.actualizar(c);
        ok("Categoría actualizada.");
    }

    private static void eliminarCategoria() {
        catSrv.eliminar(leerEntero("Id de la categoría a eliminar: "));
        ok("Operación realizada.");
    }

    // ------------------------------------------------------------ Producto

    private static void crearProducto() {
        Producto p = new Producto();
        p.setNombre(leerTexto("Nombre: "));
        p.setDescripcion(leerTexto("Descripción: "));
        p.setPrecio(leerEntero("Precio: "));
        p.setId_categoria(leerEntero("Id de la categoría: "));
        p.setId_administrador(leerEntero("Id del administrador que lo registra: "));
        String disp = leerTexto("Disponibilidad (" + Constantes.DISPONIBLE + "/" + Constantes.NO_DISPONIBLE
                + ", Enter = " + Constantes.DISPONIBLE + "): ");
        p.setDisponibilidad(disp.isEmpty() ? null : disp);
        prodSrv.crear(p);
        ok("Producto creado con id " + p.getId_producto());
    }

    private static void listarProductos() {
        List<Producto> lista = prodSrv.listar();
        if (lista.isEmpty()) System.out.println("(sin registros)");
        for (Producto p : lista) {
            System.out.printf("%d | %s | $%d | categoría: %d | %s%n", p.getId_producto(), p.getNombre(),
                    p.getPrecio(), p.getId_categoria(), p.getDisponibilidad());
        }
    }

    private static void actualizarProducto() {
        Producto p = prodSrv.obtenerPorId(leerEntero("Id del producto: "));
        if (p == null) { System.out.println("No existe."); return; }
        p.setNombre(leerTextoOpcional("Nombre", p.getNombre()));
        p.setDescripcion(leerTextoOpcional("Descripción", p.getDescripcion()));
        p.setPrecio(leerEnteroOpcional("Precio", p.getPrecio()));
        p.setId_categoria(leerEnteroOpcional("Id categoría", p.getId_categoria()));
        p.setDisponibilidad(leerTextoOpcional("Disponibilidad", p.getDisponibilidad()));
        prodSrv.actualizar(p);
        ok("Producto actualizado.");
    }

    private static void eliminarProducto() {
        prodSrv.eliminar(leerEntero("Id del producto a eliminar: "));
        ok("Operación realizada (si ya se usó en pedidos queda como 'No disponible').");
    }

    // ------------------------------------------------------------ Pedidos

    private static void menuPedidos() {
        while (true) {
            System.out.println("\n--- Pedidos ---");
            System.out.println("1. Crear pedido");
            System.out.println("2. Listar pedidos");
            System.out.println("3. Ver resumen de un pedido");
            System.out.println("0. Volver");
            int op = leerEntero("Opción: ");
            if (op == 0) return;
            if (op == 1) ejecutar(MenuConsola::crearPedido);
            else if (op == 2) ejecutar(MenuConsola::listarPedidos);
            else if (op == 3) ejecutar(() -> System.out.println(pedSrv.resumenPedido(leerEntero("Id del pedido: "))));
            else System.out.println("Opción no válida.");
        }
    }

    private static void crearPedido() {
        System.out.println("Tipo de pedido: 1. En mesa  2. Para llevar  3. A domicilio");
        int t = leerEntero("Opción: ");

        Pedido p = new Pedido();
        p.setId_mesero(leerEntero("Id del mesero: "));
        Domicilio dom = null;

        if (t == 1) {
            p.setTipo_pedido(Constantes.TIPO_MESA);
            System.out.println("Mesas libres:");
            for (Mesa m : mesaSrv.listarLibres()) System.out.println("  id " + m.getId_mesa() + " -> mesa #" + m.getNumero());
            p.setId_mesa(leerEntero("Id de la mesa: "));
        } else if (t == 2) {
            p.setTipo_pedido(Constantes.TIPO_LLEVAR);
            p.setNombre_cliente(leerTexto("Nombre del cliente: "));
        } else if (t == 3) {
            p.setTipo_pedido(Constantes.TIPO_DOMICILIO);
            p.setNombre_cliente(leerTexto("Nombre del cliente: "));
            dom = new Domicilio();
            dom.setDireccion(leerTexto("Dirección: "));
            dom.setTelefono(leerTexto("Teléfono: "));
            dom.setCosto_envio(leerEntero("Costo de envío: "));
        } else {
            System.out.println("Tipo no válido.");
            return;
        }

        List<DetallePedido> detalles = new ArrayList<>();
        System.out.println("Productos disponibles:");
        for (Producto pr : prodSrv.listarDisponibles()) {
            System.out.printf("  id %d -> %s ($%d)%n", pr.getId_producto(), pr.getNombre(), pr.getPrecio());
        }
        while (true) {
            int idProd = leerEntero("Id del producto (0 = terminar): ");
            if (idProd == 0) break;
            DetallePedido d = new DetallePedido();
            d.setId_producto(idProd);
            d.setCantidad(leerEntero("Cantidad: "));
            String nota = leerTexto("Notas (Enter = ninguna): ");
            d.setNotas(nota.isEmpty() ? null : nota);
            detalles.add(d);
        }

        Pedido creado = (dom == null) ? pedSrv.crearPedido(p, detalles) : pedSrv.crearPedido(p, detalles, dom);
        ok("Pedido creado con id " + creado.getId_pedido());
        System.out.println(pedSrv.resumenPedido(creado.getId_pedido()));
    }

    private static void listarPedidos() {
        List<Pedido> lista = pedSrv.listar();
        if (lista.isEmpty()) System.out.println("(sin registros)");
        for (Pedido p : lista) {
            System.out.printf("%d | %s | %s | total: $%d | mesero: %d%n", p.getId_pedido(), p.getTipo_pedido(),
                    p.getEstado(), p.getTotal(), p.getId_mesero());
        }
    }
}