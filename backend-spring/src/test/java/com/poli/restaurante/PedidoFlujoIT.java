package com.poli.restaurante;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

class PedidoFlujoIT extends IntegrationTestBase {

    @Test
    void flujoCompletoDeSalonConHistorialYMesaQueSeLibera() throws Exception {
        Sesion mesero = crearUsuario("MESERO");
        Sesion cocina = crearUsuario("COCINA");
        long producto = crearProducto("12500.00");
        long mesa = crearMesa();

        String creado = esperar(201, HttpMethod.POST, "/api/pedidos", mesero.token(), pedidoSalon(mesa, producto, 2));
        long pedido = id(creado);
        assertEquals("PENDIENTE", campo(creado, "$.estado"));
        assertTotal(creado, "25000");

        // La mesa deja de estar disponible mientras el pedido siga activo.
        List<Integer> libres = campo(esperar(200, HttpMethod.GET, "/api/mesas/disponibles", mesero.token(), null), "$[*].id");
        assertFalse(libres.stream().anyMatch(m -> m.longValue() == mesa));
        esperar(409, HttpMethod.POST, "/api/pedidos", mesero.token(), pedidoSalon(mesa, producto, 1));

        List<Integer> cola = campo(esperar(200, HttpMethod.GET, "/api/pedidos/cocina", cocina.token(), null), "$[*].id");
        assertTrue(cola.stream().anyMatch(p -> p.longValue() == pedido));

        esperar(200, HttpMethod.PATCH, "/api/pedidos/" + pedido + "/estado", cocina.token(), estado("EN_PREPARACION"));
        // Con el pedido en preparación ya no se puede editar.
        esperar(409, HttpMethod.POST, "/api/pedidos/" + pedido + "/detalles", mesero.token(),
                "{\"productoId\":%d,\"cantidad\":1}".formatted(producto));
        esperar(200, HttpMethod.PATCH, "/api/pedidos/" + pedido + "/estado", cocina.token(), estado("LISTO"));
        String entregado = esperar(200, HttpMethod.PATCH, "/api/pedidos/" + pedido + "/estado", mesero.token(),
                estado("ENTREGADO"));
        assertEquals("ENTREGADO", campo(entregado, "$.estado"));

        List<Integer> libresDespues = campo(esperar(200, HttpMethod.GET, "/api/mesas/disponibles", mesero.token(), null), "$[*].id");
        assertTrue(libresDespues.stream().anyMatch(m -> m.longValue() == mesa));

        String historial = esperar(200, HttpMethod.GET, "/api/pedidos/" + pedido + "/historial", mesero.token(), null);
        List<String> estados = campo(historial, "$[*].estadoNuevo");
        assertEquals(List.of("PENDIENTE", "EN_PREPARACION", "LISTO", "ENTREGADO"), estados);
        List<Integer> usuarios = campo(historial, "$[*].usuario.id");
        assertEquals(List.of(mesero.id(), cocina.id(), cocina.id(), mesero.id()),
                usuarios.stream().map(Integer::longValue).toList());
    }

    @Test
    void soloSeAceptanTransicionesValidasYSoloPorElRolCorrecto() throws Exception {
        Sesion mesero = crearUsuario("MESERO");
        Sesion cocina = crearUsuario("COCINA");
        long pedido = crearPedidoSalon(mesero, crearProducto("5000"), 1);
        String url = "/api/pedidos/" + pedido + "/estado";

        esperar(403, HttpMethod.PATCH, url, mesero.token(), estado("EN_PREPARACION"));
        esperar(403, HttpMethod.PATCH, url, cocina.token(), estado("CANCELADO"));
        esperar(403, HttpMethod.PATCH, url, tokenAdmin(), estado("EN_PREPARACION"));
        esperar(409, HttpMethod.PATCH, url, cocina.token(), estado("LISTO"));
        esperar(409, HttpMethod.PATCH, url, mesero.token(), estado("ENTREGADO"));

        esperar(200, HttpMethod.PATCH, url, cocina.token(), estado("EN_PREPARACION"));
        esperar(200, HttpMethod.PATCH, url, cocina.token(), estado("LISTO"));
        // Desde LISTO ya no se puede cancelar.
        esperar(409, HttpMethod.PATCH, url, mesero.token(), estado("CANCELADO"));
    }

    @Test
    void cancelarDesdePendienteOEnPreparacionLiberaLaMesaYQuedaEnElHistorial() throws Exception {
        Sesion mesero = crearUsuario("MESERO");
        Sesion cocina = crearUsuario("COCINA");
        long producto = crearProducto("5000");

        long pendiente = crearPedidoSalon(mesero, producto, 1);
        esperar(200, HttpMethod.PATCH, "/api/pedidos/" + pendiente + "/estado", mesero.token(),
                "{\"estado\":\"CANCELADO\",\"comentario\":\"El cliente se fue\"}");

        long enPreparacion = crearPedidoSalon(mesero, producto, 1);
        esperar(200, HttpMethod.PATCH, "/api/pedidos/" + enPreparacion + "/estado", cocina.token(), estado("EN_PREPARACION"));
        esperar(200, HttpMethod.PATCH, "/api/pedidos/" + enPreparacion + "/estado", mesero.token(), estado("CANCELADO"));

        String historial = esperar(200, HttpMethod.GET, "/api/pedidos/" + pendiente + "/historial", mesero.token(), null);
        assertEquals("El cliente se fue", campo(historial, "$[1].comentario"));
        // Ya cancelado no admite más cambios.
        esperar(409, HttpMethod.PATCH, "/api/pedidos/" + pendiente + "/estado", mesero.token(), estado("CANCELADO"));
    }

    @Test
    void soloElMeseroPropietarioEditaYConsultaSuPedido() throws Exception {
        Sesion duenio = crearUsuario("MESERO");
        Sesion otro = crearUsuario("MESERO");
        long producto = crearProducto("5000");
        long pedido = crearPedidoSalon(duenio, producto, 1);

        esperar(403, HttpMethod.POST, "/api/pedidos/" + pedido + "/detalles", otro.token(),
                "{\"productoId\":%d,\"cantidad\":1}".formatted(producto));
        esperar(403, HttpMethod.GET, "/api/pedidos/" + pedido, otro.token(), null);
        esperar(403, HttpMethod.PATCH, "/api/pedidos/" + pedido + "/estado", otro.token(), estado("CANCELADO"));
        esperar(200, HttpMethod.GET, "/api/pedidos/" + pedido, duenio.token(), null);

        String mios = esperar(200, HttpMethod.GET, "/api/pedidos/mios?estado=PENDIENTE", duenio.token(), null);
        assertEquals(1, ((Number) campo(mios, "$.totalElements")).intValue());
        String ajenos = esperar(200, HttpMethod.GET, "/api/pedidos/mios", otro.token(), null);
        assertEquals(0, ((Number) campo(ajenos, "$.totalElements")).intValue());
    }

    @Test
    void editarProductosRecalculaElTotalConLosPreciosGuardadosYNoPermiteVaciarElPedido() throws Exception {
        Sesion mesero = crearUsuario("MESERO");
        long categoria = crearCategoria();
        long hamburguesa = crearProducto(categoria, "10000.00");
        long jugo = crearProducto(categoria, "3000.00");
        String creado = esperar(201, HttpMethod.POST, "/api/pedidos", mesero.token(),
                pedidoSalon(crearMesa(), hamburguesa, 1));
        long pedido = id(creado);
        long detalleHamburguesa = ((Number) campo(creado, "$.detalles[0].id")).longValue();

        // El producto sube de precio, pero el pedido conserva el precio con que se pidió.
        esperar(200, HttpMethod.PUT, "/api/admin/productos/" + hamburguesa, tokenAdmin(),
                "{\"categoriaId\":%d,\"nombre\":\"Hamburguesa nueva\",\"precio\":99999.00}".formatted(categoria));

        String conJugo = esperar(201, HttpMethod.POST, "/api/pedidos/" + pedido + "/detalles", mesero.token(),
                "{\"productoId\":%d,\"cantidad\":2,\"notas\":\"con hielo\"}".formatted(jugo));
        assertTotal(conJugo, "16000");

        String editado = esperar(200, HttpMethod.PUT, "/api/pedidos/" + pedido + "/detalles/" + detalleHamburguesa,
                mesero.token(), "{\"cantidad\":3,\"notas\":\"sin salsa\"}");
        assertTotal(editado, "36000");
        assertEquals("sin salsa", campo(editado, "$.detalles[0].notas"));

        long detalleJugo = ((Number) campo(editado, "$.detalles[1].id")).longValue();
        String sinJugo = esperar(200, HttpMethod.DELETE, "/api/pedidos/" + pedido + "/detalles/" + detalleJugo,
                mesero.token(), null);
        assertTotal(sinJugo, "30000");

        String ultimo = esperar(422, HttpMethod.DELETE, "/api/pedidos/" + pedido + "/detalles/" + detalleHamburguesa,
                mesero.token(), null);
        assertTrue(ultimo.contains("último producto"));
    }

    @Test
    void domicilioExigeEntregaYAplicaElLimiteDeContraentrega() throws Exception {
        Sesion mesero = crearUsuario("MESERO");
        long caro = crearProducto("70000.00");
        String admin = tokenAdmin();

        String base = "{\"tipo\":\"DOMICILIO\",\"nombreCliente\":\"Ana\",%s\"items\":[{\"productoId\":%d,\"cantidad\":1}]}";
        // Sin datos de entrega.
        esperar(422, HttpMethod.POST, "/api/pedidos", mesero.token(), base.formatted("", caro));
        // 70.000 + 15.000 de envío = 85.000 > 80.000: no se admite contraentrega.
        String entregaContraentrega = "\"entrega\":{\"direccion\":\"Calle 1 # 2-3\",\"telefono\":\"3001234567\","
                + "\"costoEnvio\":15000,\"metodoPago\":\"EFECTIVO_CONTRAENTREGA\"},";
        esperar(422, HttpMethod.POST, "/api/pedidos", mesero.token(), base.formatted(entregaContraentrega, caro));

        String entregaVirtual = entregaContraentrega.replace("EFECTIVO_CONTRAENTREGA", "PAGO_VIRTUAL");
        String virtual = esperar(201, HttpMethod.POST, "/api/pedidos", mesero.token(), base.formatted(entregaVirtual, caro));
        assertTotal(virtual, "85000");
        assertEquals("PAGO_VIRTUAL", campo(virtual, "$.entrega.metodoPago"));

        // Con envío de 5.000 el total es 75.000 y la contraentrega es válida.
        String barata = entregaContraentrega.replace("15000", "5000");
        assertTotal(esperar(201, HttpMethod.POST, "/api/pedidos", mesero.token(), base.formatted(barata, caro)), "75000");

        // El administrador sube el límite a 100.000 y el mismo pedido ahora sí se acepta.
        esperar(200, HttpMethod.PUT, "/api/admin/configuracion/domicilios", admin, "{\"limiteContraentrega\":100000}");
        esperar(201, HttpMethod.POST, "/api/pedidos", mesero.token(), base.formatted(entregaContraentrega, caro));
        esperar(200, HttpMethod.PUT, "/api/admin/configuracion/domicilios", admin, "{\"limiteContraentrega\":80000}");
        String limite = esperar(200, HttpMethod.GET, "/api/admin/configuracion/domicilios", admin, null);
        assertEquals(0, numero(limite, "$.limiteContraentrega").compareTo(new java.math.BigDecimal("80000")));
    }

    @Test
    void editarUnDomicilioNoPuedeSuperarElLimiteDeContraentrega() throws Exception {
        Sesion mesero = crearUsuario("MESERO");
        long producto = crearProducto("30000.00");
        String creado = esperar(201, HttpMethod.POST, "/api/pedidos", mesero.token(),
                ("{\"tipo\":\"DOMICILIO\",\"nombreCliente\":\"Luis\",\"entrega\":{\"direccion\":\"Av 5\","
                        + "\"telefono\":\"3000000000\",\"costoEnvio\":0,\"metodoPago\":\"EFECTIVO_CONTRAENTREGA\"},"
                        + "\"items\":[{\"productoId\":%d,\"cantidad\":2}]}").formatted(producto));
        long pedido = id(creado);
        // 60.000 -> agregar 1 más = 90.000 > 80.000
        esperar(422, HttpMethod.POST, "/api/pedidos/" + pedido + "/detalles", mesero.token(),
                "{\"productoId\":%d,\"cantidad\":1}".formatted(producto));
        assertTotal(esperar(200, HttpMethod.GET, "/api/pedidos/" + pedido, mesero.token(), null), "60000");
    }

    @Test
    void reglasPorTipoDePedidoYProductosNoDisponibles() throws Exception {
        Sesion mesero = crearUsuario("MESERO");
        long categoria = crearCategoria();
        long producto = crearProducto(categoria, "4000");

        esperar(422, HttpMethod.POST, "/api/pedidos", mesero.token(),
                "{\"tipo\":\"PARA_LLEVAR\",\"items\":[{\"productoId\":%d,\"cantidad\":1}]}".formatted(producto));
        String llevar = esperar(201, HttpMethod.POST, "/api/pedidos", mesero.token(),
                "{\"tipo\":\"PARA_LLEVAR\",\"nombreCliente\":\"Marta\",\"items\":[{\"productoId\":%d,\"cantidad\":1}]}"
                        .formatted(producto));
        assertEquals("PARA_LLEVAR", campo(llevar, "$.tipo"));
        esperar(422, HttpMethod.POST, "/api/pedidos", mesero.token(),
                "{\"tipo\":\"PARA_LLEVAR\",\"nombreCliente\":\"M\",\"mesaId\":1,\"items\":[{\"productoId\":%d,\"cantidad\":1}]}"
                        .formatted(producto));
        esperar(400, HttpMethod.POST, "/api/pedidos", mesero.token(), "{\"tipo\":\"PARA_LLEVAR\",\"nombreCliente\":\"M\",\"items\":[]}");
        esperar(404, HttpMethod.POST, "/api/pedidos", mesero.token(),
                "{\"tipo\":\"PARA_LLEVAR\",\"nombreCliente\":\"M\",\"items\":[{\"productoId\":999999,\"cantidad\":1}]}");

        // Un producto marcado como no disponible desaparece del menú y no se puede pedir.
        esperar(200, HttpMethod.PUT, "/api/admin/productos/" + producto, tokenAdmin(),
                "{\"categoriaId\":%d,\"nombre\":\"Agotado\",\"precio\":4000,\"disponible\":false}"
                        .formatted(categoria));
        esperar(422, HttpMethod.POST, "/api/pedidos", mesero.token(),
                "{\"tipo\":\"PARA_LLEVAR\",\"nombreCliente\":\"M\",\"items\":[{\"productoId\":%d,\"cantidad\":1}]}"
                        .formatted(producto));
        List<Integer> menu = campo(esperar(200, HttpMethod.GET, "/api/menu/productos", mesero.token(), null), "$[*].id");
        assertFalse(menu.stream().anyMatch(p -> p.longValue() == producto));
    }

    @Test
    void elAdministradorConsultaPedidosConFiltrosYPaginacion() throws Exception {
        Sesion mesero = crearUsuario("MESERO");
        long producto = crearProducto("4000");
        crearPedidoSalon(mesero, producto, 1);
        crearPedidoSalon(mesero, producto, 1);

        String admin = tokenAdmin();
        String pagina = esperar(200, HttpMethod.GET,
                "/api/admin/pedidos?meseroId=" + mesero.id() + "&estado=PENDIENTE&size=1", admin, null);
        assertEquals(2, ((Number) campo(pagina, "$.totalElements")).intValue());
        assertEquals(2, ((Number) campo(pagina, "$.totalPages")).intValue());
        assertEquals(1, ((List<?>) campo(pagina, "$.content")).size());
        long pedido = ((Number) campo(pagina, "$.content[0].id")).longValue();
        assertEquals(pedido, id(esperar(200, HttpMethod.GET, "/api/admin/pedidos/" + pedido, admin, null)));
        esperar(400, HttpMethod.GET, "/api/admin/pedidos?sort=noExiste,asc", admin, null);
        esperar(400, HttpMethod.GET, "/api/admin/pedidos?estado=INVENTADO", admin, null);
    }

    @Test
    void noSeBorranCategoriasProductosNiMesasEnUso() throws Exception {
        Sesion mesero = crearUsuario("MESERO");
        long categoria = crearCategoria();
        long producto = crearProducto(categoria, "4000");
        String admin = tokenAdmin();

        esperar(409, HttpMethod.DELETE, "/api/admin/categorias/" + categoria, admin, null);
        long pedido = crearPedidoSalon(mesero, producto, 1);
        esperar(409, HttpMethod.DELETE, "/api/admin/productos/" + producto, admin, null);
        long mesa = ((Number) campo(esperar(200, HttpMethod.GET, "/api/pedidos/" + pedido, mesero.token(), null),
                "$.mesa.id")).longValue();
        esperar(409, HttpMethod.DELETE, "/api/admin/mesas/" + mesa, admin, null);

        long sinUso = crearProducto("1000");
        esperar(204, HttpMethod.DELETE, "/api/admin/productos/" + sinUso, admin, null);
        esperar(404, HttpMethod.GET, "/api/admin/pedidos/999999", admin, null);
    }
}
