package app;

import entidades.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import servicios.*;
import util.Constantes;

public class MenuConsola {

    private static final Scanner in = new Scanner(System.in);

    private static final AdministradorService adminSrv = new AdministradorService();
    private static final MeseroService meseroSrv = new MeseroService();
    private static final PersonalCocinaService cocinaSrv = new PersonalCocinaService();
    private static final MesaService mesaSrv = new MesaService();
    private static final CategoriaService catSrv = new CategoriaService();
    private static final ProductoService prodSrv = new ProductoService();
    private static final PedidoService pedSrv = new PedidoService();
    private static final DependenciasService depSrv = new DependenciasService();

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
                case 1:
                    submenu("Administradores", MenuConsola::crearAdmin, MenuConsola::listarAdmin,
                            MenuConsola::actualizarAdmin, MenuConsola::eliminarAdmin,
                            () -> verEnlaces(DependenciasService.ADMINISTRADOR, "del administrador"),
                            () -> cambiarEstado("del administrador", adminSrv::obtenerPorId, adminSrv::estaActivo,
                                    adminSrv::activar, adminSrv::desactivar));
                    break;
                case 2:
                    submenu("Meseros", MenuConsola::crearMesero, MenuConsola::listarMeseros,
                            MenuConsola::actualizarMesero, MenuConsola::eliminarMesero,
                            () -> verEnlaces(DependenciasService.MESERO, "del mesero"),
                            () -> cambiarEstado("del mesero", meseroSrv::obtenerPorId, meseroSrv::estaActivo,
                                    meseroSrv::activar, meseroSrv::desactivar));
                    break;
                case 3:
                    submenu("Personal de cocina", MenuConsola::crearCocina, MenuConsola::listarCocina,
                            MenuConsola::actualizarCocina, MenuConsola::eliminarCocina,
                            () -> verEnlaces(DependenciasService.COCINA, "del personal de cocina"),
                            () -> cambiarEstado("del personal de cocina", cocinaSrv::obtenerPorId,
                                    cocinaSrv::estaActivo,
                                    cocinaSrv::activar, cocinaSrv::desactivar));
                    break;
                case 4:
                    submenu("Mesas", MenuConsola::crearMesa, MenuConsola::listarMesas,
                            MenuConsola::actualizarMesa, MenuConsola::eliminarMesa,
                            () -> verEnlaces(DependenciasService.MESA, "de la mesa"));
                    break;
                case 5:
                    submenu("Categorías", MenuConsola::crearCategoria, MenuConsola::listarCategorias,
                            MenuConsola::actualizarCategoria, MenuConsola::eliminarCategoria,
                            () -> verEnlaces(DependenciasService.CATEGORIA, "de la categoría"));
                    break;
                case 6:
                    submenu("Productos", MenuConsola::crearProducto, MenuConsola::listarProductos,
                            MenuConsola::actualizarProducto, MenuConsola::eliminarProducto,
                            () -> verEnlaces(DependenciasService.PRODUCTO, "del producto"));
                    break;
                case 7:
                    menuPedidos();
                    break;
                case 0:
                    System.out.println("Hasta luego.");
                    return;
                default:
                    System.out.println("Opción no válida.");
            }
        }
    }

    private static void submenu(String titulo, Runnable crear, Runnable listar,
            Runnable actualizar, Runnable eliminar, Runnable enlaces) {
        submenu(titulo, crear, listar, actualizar, eliminar, enlaces, null);
    }

    /**
     * estado puede ser null (Mesas, Categorías y Productos no se
     * activan/desactivan).
     */
    private static void submenu(String titulo, Runnable crear, Runnable listar,
            Runnable actualizar, Runnable eliminar, Runnable enlaces, Runnable estado) {
        while (true) {
            System.out.println("\n--- " + titulo + " ---");
            System.out.println("1. Crear");
            System.out.println("2. Listar");
            System.out.println("3. Actualizar");
            System.out.println("4. Eliminar");
            System.out.println("5. Ver qué tiene enlazado");
            if (estado != null)
                System.out.println("6. Activar / desactivar");
            System.out.println("0. Volver");
            int op = leerEntero("Opción: ");
            if (op == 0)
                return;
            Runnable accion = op == 1 ? crear
                    : op == 2 ? listar
                            : op == 3 ? actualizar
                                    : op == 4 ? eliminar
                                            : op == 5 ? enlaces : op == 6 ? estado : null;
            if (accion == null) {
                System.out.println("Opción no válida.");
                continue;
            }
            ejecutar(accion);
        }
    }

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

    private static double leerDouble(String msg) {
        while (true) {
            System.out.print(msg);
            String s = in.nextLine().trim();
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException e) {
                System.out.println("Escribe un número decimal válido.");
            }
        }
    }

    private static String leerTextoOpcional(String campo, String actual) {
        String s = leerTexto(campo + " [" + (actual == null ? "" : actual) + "]: ");
        return s.isEmpty() ? actual : s;
    }

    private static int leerEnteroOpcional(String campo, int actual) {
        while (true) {
            String s = leerTexto(campo + " [" + actual + "]: ");
            if (s.isEmpty())
                return actual;
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                System.out.println("Escribe un número entero válido.");
            }
        }
    }

    private static double leerDoubleOpcional(String campo, double actual) {
        while (true) {
            String s = leerTexto(campo + " [" + actual + "]: ");
            if (s.isEmpty())
                return actual;
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException e) {
                System.out.println("Escribe un número decimal válido.");
            }
        }
    }

    /**
     * Opción 5: muestra qué registros dependen de este (lo que hay que desenlazar
     * antes de eliminar).
     */
    private static void verEnlaces(String tipo, String etiqueta) {
        int id = leerEntero("Id " + etiqueta + ": ");
        if (!mostrarEnlaces(tipo, id)) {
            System.out.println("No tiene registros enlazados: se puede eliminar sin problema.");
        }
    }

    /** Imprime los enlaces y el consejo. Devuelve true si había enlaces. */
    private static boolean mostrarEnlaces(String tipo, int id) {
        java.util.List<String> lineas = depSrv.enlaces(tipo, id);
        if (lineas.isEmpty())
            return false;
        System.out.println("Tiene registros enlazados:");
        for (String l : lineas)
            System.out.println("  " + l);
        System.out.println("-> " + depSrv.consejo(tipo));
        return true;
    }

    /**
     * Opción 6 de los usuarios: muestra el estado actual y pregunta si se quiere
     * cambiar.
     */
    private static void cambiarEstado(String etiqueta, java.util.function.IntFunction<Object> existe,
            java.util.function.IntPredicate estaActivo,
            java.util.function.IntConsumer activar,
            java.util.function.IntConsumer desactivar) {
        int id = leerEntero("Id " + etiqueta + ": ");
        if (existe.apply(id) == null) {
            System.out.println("No existe.");
            return;
        }
        boolean activo = estaActivo.test(id);
        System.out.println("Estado actual: " + (activo ? "activo" : "inactivo"));
        String r = leerTexto(activo ? "¿Desactivar? (s/n): " : "¿Activar? (s/n): ");
        if (!r.equalsIgnoreCase("s")) {
            System.out.println("Sin cambios.");
            return;
        }
        if (activo)
            desactivar.accept(id);
        else
            activar.accept(id);
        ok(activo ? "Desactivado." : "Activado.");
    }

    private static void ok(String msg) {
        System.out.println("[OK] " + msg);
    }

    private static void crearAdmin() {
        Administrador a = new Administrador();
        a.setNombre(leerTexto("Nombre: "));
        a.setUsuario(leerTexto("Usuario: "));
        a.setContrasena(leerTexto("Contraseña: "));
        a.setTipo_documento(leerTexto("Tipo de documento (ej. CC, CE): "));
        a.setNumero_documento(leerTexto("Número de documento: "));
        a.setCorreo(leerTexto("Correo electrónico: "));
        a.setTelefono(leerTexto("Teléfono: "));
        a.setDireccion(leerTexto("Dirección: "));
        adminSrv.crear(a);
        ok("Administrador creado con id " + a.getId_administrador());
    }

    private static void listarAdmin() {
        List<Administrador> lista = adminSrv.listar();
        if (lista.isEmpty()) {
            System.out.println("(sin registros)");
        } else {
            for (Administrador a : lista) {
                System.out.println(a);
            }
        }
    }

    private static void actualizarAdmin() {
        Administrador a = adminSrv.obtenerPorId(leerEntero("Id del administrador: "));
        if (a == null) {
            System.out.println("No existe.");
            return;
        }
        a.setNombre(leerTextoOpcional("Nombre", a.getNombre()));
        a.setUsuario(leerTextoOpcional("Usuario", a.getUsuario()));
        a.setContrasena(leerTexto("Nueva contraseña (Enter = conservar): "));
        a.setTipo_documento(leerTextoOpcional("Tipo de documento", a.getTipo_documento()));
        a.setNumero_documento(leerTextoOpcional("Número de documento", a.getNumero_documento()));
        a.setCorreo(leerTextoOpcional("Correo electrónico", a.getCorreo()));
        a.setTelefono(leerTextoOpcional("Teléfono", a.getTelefono()));
        a.setDireccion(leerTextoOpcional("Dirección", a.getDireccion()));
        adminSrv.actualizar(a);
        ok("Administrador actualizado.");
    }

    private static void eliminarAdmin() {
        int id = leerEntero("Id del administrador a eliminar: ");
        mostrarEnlaces(DependenciasService.ADMINISTRADOR, id);
        adminSrv.eliminar(id);
        ok("Operación realizada.");
    }

    private static void crearMesero() {
        Mesero m = new Mesero();
        m.setNombre(leerTexto("Nombre: "));
        m.setUsuario(leerTexto("Usuario: "));
        m.setContrasena(leerTexto("Contraseña: "));
        m.setTipo_documento(leerTexto("Tipo de documento (ej. CC, CE): "));
        m.setNumero_documento(leerTexto("Número de documento: "));
        m.setCorreo(leerTexto("Correo electrónico: "));
        m.setTelefono(leerTexto("Teléfono: "));
        m.setDireccion(leerTexto("Dirección: "));
        meseroSrv.crear(m);
        ok("Mesero creado con id " + m.getId_mesero());
    }

    private static void listarMeseros() {
        List<Mesero> lista = meseroSrv.listar();
        if (lista.isEmpty()) {
            System.out.println("(sin registros)");
        } else {
            for (Mesero m : lista) {
                System.out.println(m);
            }
        }
    }

    private static void actualizarMesero() {
        Mesero m = meseroSrv.obtenerPorId(leerEntero("Id del mesero: "));
        if (m == null) {
            System.out.println("No existe.");
            return;
        }
        m.setNombre(leerTextoOpcional("Nombre", m.getNombre()));
        m.setUsuario(leerTextoOpcional("Usuario", m.getUsuario()));
        m.setContrasena(leerTexto("Nueva contraseña (Enter = conservar): "));
        m.setTipo_documento(leerTextoOpcional("Tipo de documento", m.getTipo_documento()));
        m.setNumero_documento(leerTextoOpcional("Número de documento", m.getNumero_documento()));
        m.setCorreo(leerTextoOpcional("Correo electrónico", m.getCorreo()));
        m.setTelefono(leerTextoOpcional("Teléfono", m.getTelefono()));
        m.setDireccion(leerTextoOpcional("Dirección", m.getDireccion()));
        meseroSrv.actualizar(m);
        ok("Mesero actualizado.");
    }

    private static void eliminarMesero() {
        int id = leerEntero("Id del mesero a eliminar: ");
        mostrarEnlaces(DependenciasService.MESERO, id);
        meseroSrv.eliminar(id);
        ok("Operación realizada.");
    }

    private static void crearCocina() {
        PersonalCocina c = new PersonalCocina();
        c.setNombre(leerTexto("Nombre: "));
        c.setUsuario(leerTexto("Usuario: "));
        c.setContrasena(leerTexto("Contraseña: "));
        c.setTipo_documento(leerTexto("Tipo de documento (ej. CC, CE): "));
        c.setNumero_documento(leerTexto("Número de documento: "));
        c.setCorreo(leerTexto("Correo electrónico: "));
        c.setTelefono(leerTexto("Teléfono: "));
        c.setDireccion(leerTexto("Dirección: "));
        cocinaSrv.crear(c);
        ok("Personal de cocina creado con id " + c.getId_cocina());
    }

    private static void listarCocina() {
        List<PersonalCocina> lista = cocinaSrv.listar();
        if (lista.isEmpty()) {
            System.out.println("(sin registros)");
        } else {
            for (PersonalCocina c : lista) {
                System.out.println(c);
            }
        }
    }

    private static void actualizarCocina() {
        PersonalCocina c = cocinaSrv.obtenerPorId(leerEntero("Id del personal de cocina: "));
        if (c == null) {
            System.out.println("No existe.");
            return;
        }
        c.setNombre(leerTextoOpcional("Nombre", c.getNombre()));
        c.setUsuario(leerTextoOpcional("Usuario", c.getUsuario()));
        c.setContrasena(leerTexto("Nueva contraseña (Enter = conservar): "));
        c.setTipo_documento(leerTextoOpcional("Tipo de documento", c.getTipo_documento()));
        c.setNumero_documento(leerTextoOpcional("Número de documento", c.getNumero_documento()));
        c.setCorreo(leerTextoOpcional("Correo electrónico", c.getCorreo()));
        c.setTelefono(leerTextoOpcional("Teléfono", c.getTelefono()));
        c.setDireccion(leerTextoOpcional("Dirección", c.getDireccion()));
        cocinaSrv.actualizar(c);
        ok("Personal de cocina actualizado.");
    }

    private static void eliminarCocina() {
        int id = leerEntero("Id del personal de cocina a eliminar: ");
        mostrarEnlaces(DependenciasService.COCINA, id);
        cocinaSrv.eliminar(id);
        ok("Operación realizada.");
    }

    private static void crearMesa() {
        Mesa m = new Mesa();
        m.setNumero(leerEntero("Número de mesa: "));
        m.setId_administrador(leerEntero("Id del administrador que la registra: "));
        mesaSrv.crear(m);
        ok("Mesa creada con id " + m.getId_mesa());
    }

    private static void listarMesas() {
        List<Mesa> lista = mesaSrv.listar();
        if (lista.isEmpty())
            System.out.println("(sin registros)");
        for (Mesa m : lista) {
            System.out.printf("%d | mesa #%d | %s | admin: %d%n", m.getId_mesa(), m.getNumero(),
                    m.getEstado(), m.getId_administrador());
        }
    }

    private static void actualizarMesa() {
        Mesa m = mesaSrv.obtenerPorId(leerEntero("Id de la mesa: "));
        if (m == null) {
            System.out.println("No existe.");
            return;
        }
        m.setNumero(leerEnteroOpcional("Número", m.getNumero()));
        String estado = leerTextoOpcional("Estado (" + Constantes.MESA_LIBRE + "/" + Constantes.MESA_OCUPADA + ")",
                m.getEstado());
        m.setEstado(estado);
        mesaSrv.actualizar(m);
        ok("Mesa actualizada.");
    }

    private static void eliminarMesa() {
        int id = leerEntero("Id de la mesa a eliminar: ");
        mostrarEnlaces(DependenciasService.MESA, id);
        mesaSrv.eliminar(id);
        ok("Operación realizada.");
    }

    private static void crearCategoria() {
        Categoria c = new Categoria();
        c.setNombre(leerTexto("Nombre de la categoría: "));
        c.setId_administrador(leerEntero("Id del administrador que la registra: "));
        catSrv.crear(c);
        ok("Categoría creada con id " + c.getId_categoria());
    }

    private static void listarCategorias() {
        List<Categoria> lista = catSrv.listar();
        if (lista.isEmpty())
            System.out.println("(sin registros)");
        for (Categoria c : lista) {
            System.out.printf("%d | %s | admin: %d%n", c.getId_categoria(), c.getNombre(), c.getId_administrador());
        }
    }

    private static void actualizarCategoria() {
        Categoria c = catSrv.obtenerPorId(leerEntero("Id de la categoría: "));
        if (c == null) {
            System.out.println("No existe.");
            return;
        }
        c.setNombre(leerTextoOpcional("Nombre", c.getNombre()));
        catSrv.actualizar(c);
        ok("Categoría actualizada.");
    }

    private static void eliminarCategoria() {
        int id = leerEntero("Id de la categoría a eliminar: ");
        mostrarEnlaces(DependenciasService.CATEGORIA, id);
        catSrv.eliminar(id);
        ok("Operación realizada.");
    }

    private static void crearProducto() {
        Producto p = new Producto();
        p.setNombre(leerTexto("Nombre: "));
        p.setDescripcion(leerTexto("Descripción: "));
        p.setPrecio(leerDouble("Precio: "));
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
        if (lista.isEmpty())
            System.out.println("(sin registros)");
        for (Producto p : lista) {
            System.out.printf("%d | %s | $%.2f | categoría: %d | %s%n", p.getId_producto(), p.getNombre(),
                    p.getPrecio(), p.getId_categoria(), p.getDisponibilidad());
        }
    }

    private static void actualizarProducto() {
        Producto p = prodSrv.obtenerPorId(leerEntero("Id del producto: "));
        if (p == null) {
            System.out.println("No existe.");
            return;
        }
        p.setNombre(leerTextoOpcional("Nombre", p.getNombre()));
        p.setDescripcion(leerTextoOpcional("Descripción", p.getDescripcion()));
        p.setPrecio(leerDoubleOpcional("Precio", p.getPrecio()));
        p.setId_categoria(leerEnteroOpcional("Id categoría", p.getId_categoria()));
        p.setDisponibilidad(leerTextoOpcional("Disponibilidad", p.getDisponibilidad()));
        prodSrv.actualizar(p);
        ok("Producto actualizado.");
    }

    private static void eliminarProducto() {
        int id = leerEntero("Id del producto a eliminar: ");
        mostrarEnlaces(DependenciasService.PRODUCTO, id);
        prodSrv.eliminar(id);
        ok("Operación realizada (si ya se usó en pedidos queda como 'No disponible').");
    }

    private static void menuPedidos() {
        while (true) {
            System.out.println("\n--- Pedidos ---");
            System.out.println("1. Crear pedido");
            System.out.println("2. Listar pedidos");
            System.out.println("3. Ver resumen de un pedido");
            System.out.println("4. Actualizar pedido");
            System.out.println("5. Eliminar pedido");
            System.out.println("0. Volver");
            int op = leerEntero("Opción: ");
            if (op == 0)
                return;
            if (op == 1)
                ejecutar(MenuConsola::crearPedido);
            else if (op == 2)
                ejecutar(MenuConsola::listarPedidos);
            else if (op == 3)
                ejecutar(() -> System.out.println(pedSrv.resumenPedido(leerEntero("Id del pedido: "))));
            else if (op == 4)
                ejecutar(MenuConsola::actualizarPedido);
            else if (op == 5)
                ejecutar(MenuConsola::eliminarPedido);
            else
                System.out.println("Opción no válida.");
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
            for (Mesa m : mesaSrv.listarLibres())
                System.out.println("  id " + m.getId_mesa() + " -> mesa #" + m.getNumero());
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
            dom.setCosto_envio(leerDouble("Costo de envío: "));
        } else {
            System.out.println("Tipo no válido.");
            return;
        }

        // <-- AQUÍ SOLICITAMOS EL MÉTODO DE PAGO -->
        p.setMetodo_pago(leerTexto("Método de pago (Ej. Efectivo, Tarjeta, Transferencia): "));

        List<DetallePedido> detalles = new ArrayList<>();
        System.out.println("Productos disponibles:");
        for (Producto pr : prodSrv.listarDisponibles()) {
            System.out.printf("  id %d -> %s ($%.2f)%n", pr.getId_producto(), pr.getNombre(), pr.getPrecio());
        }
        while (true) {
            int idProd = leerEntero("Id del producto (0 = terminar): ");
            if (idProd == 0)
                break;
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

    private static void actualizarPedido() {
        int id = leerEntero("Id del pedido a actualizar: ");
        Pedido p = pedSrv.obtenerPorId(id);
        if (p == null) {
            System.out.println("No existe un pedido con ese ID.");
            return;
        }

        System.out.println("Estado actual: " + p.getEstado());
        p.setEstado(leerTexto("Nuevo estado (Ej. Pendiente, Preparando, Entregado): "));
        p.setMetodo_pago(leerTexto("Nuevo método de pago (Enter para dejar igual): "));

        pedSrv.actualizar(p);
        ok("Pedido actualizado correctamente.");
    }

    private static void eliminarPedido() {
        int id = leerEntero("Id del pedido a eliminar: ");
        pedSrv.eliminar(id);
        ok("Pedido eliminado correctamente.");
    }

    private static void listarPedidos() {
        List<Pedido> lista = pedSrv.listar();
        if (lista.isEmpty())
            System.out.println("(sin registros)");
        for (Pedido p : lista) {
            System.out.printf("%d | %s | %s | total: $%.2f | pago: %s | mesero: %d%n",
                    p.getId_pedido(),
                    p.getTipo_pedido(),
                    p.getEstado(),
                    p.getTotal(),
                    p.getMetodo_pago() != null ? p.getMetodo_pago() : "No especificado",
                    p.getId_mesero());
        }
    }
}