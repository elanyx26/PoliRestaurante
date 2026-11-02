package com.poli.restaurante;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jayway.jsonpath.JsonPath;
import java.lang.reflect.Type;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

class NotificacionesWebSocketIT extends IntegrationTestBase {

    @LocalServerPort
    private int puerto;

    private StompSession conectar(String token) throws Exception {
        WebSocketStompClient cliente = new WebSocketStompClient(new StandardWebSocketClient());
        // Entrega el JSON crudo como texto: el servidor lo envía como application/json.
        cliente.setMessageConverter(new StringMessageConverter() {
            @Override
            protected boolean supportsMimeType(MessageHeaders headers) {
                return true;
            }
        });
        StompHeaders cabeceras = new StompHeaders();
        if (token != null) {
            cabeceras.add("Authorization", "Bearer " + token);
        }
        return cliente.connectAsync("ws://localhost:" + puerto + "/ws", new WebSocketHttpHeaders(), cabeceras,
                new StompSessionHandlerAdapter() { }).get(10, TimeUnit.SECONDS);
    }

    private static BlockingQueue<String> suscribir(StompSession sesion, String destino) {
        BlockingQueue<String> mensajes = new LinkedBlockingQueue<>();
        sesion.subscribe(destino, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                mensajes.add((String) payload);
            }
        });
        return mensajes;
    }

    @Test
    void cocinaYMeseroRecibenNotificacionesEnVivo() throws Exception {
        Sesion mesero = crearUsuario("MESERO");
        Sesion cocina = crearUsuario("COCINA");
        long producto = crearProducto("8000");

        StompSession wsCocina = conectar(cocina.token());
        StompSession wsMesero = conectar(mesero.token());
        BlockingQueue<String> enCocina = suscribir(wsCocina, "/topic/cocina");
        BlockingQueue<String> enMesero = suscribir(wsMesero, "/user/queue/pedidos");
        Thread.sleep(700);

        long pedido = crearPedidoSalon(mesero, producto, 1);

        String creado = enCocina.poll(5, TimeUnit.SECONDS);
        assertNotNull(creado, "cocina debía recibir el pedido nuevo");
        assertEquals("PEDIDO_CREADO", JsonPath.read(creado, "$.tipo"));
        assertEquals(pedido, ((Number) JsonPath.read(creado, "$.pedidoId")).longValue());

        esperar(200, HttpMethod.PATCH, "/api/pedidos/" + pedido + "/estado", cocina.token(), estado("EN_PREPARACION"));

        // El mesero recibe su propio pedido nuevo y luego el cambio de estado hecho por cocina.
        assertEquals("PEDIDO_CREADO", JsonPath.read(enMesero.poll(5, TimeUnit.SECONDS), "$.tipo"));
        String cambio = enMesero.poll(5, TimeUnit.SECONDS);
        assertNotNull(cambio, "el mesero debía enterarse del cambio de estado");
        assertEquals("EN_PREPARACION", JsonPath.read(cambio, "$.estado"));
        assertEquals("PENDIENTE", JsonPath.read(cambio, "$.estadoAnterior"));

        wsCocina.disconnect();
        wsMesero.disconnect();
    }

    @Test
    void unMeseroNoRecibeLosPedidosDeOtroMesero() throws Exception {
        Sesion a = crearUsuario("MESERO");
        Sesion b = crearUsuario("MESERO");
        long producto = crearProducto("8000");

        StompSession wsB = conectar(b.token());
        BlockingQueue<String> enB = suscribir(wsB, "/user/queue/pedidos");
        Thread.sleep(700);

        crearPedidoSalon(a, producto, 1);
        assertNull(enB.poll(2, TimeUnit.SECONDS), "otro mesero no debe recibir notificaciones ajenas");
        wsB.disconnect();
    }

    @Test
    void sinTokenNoSePuedeConectar() {
        assertThrows(Exception.class, () -> conectar(null));
        assertThrows(Exception.class, () -> conectar("token-invalido"));
    }

    @Test
    void unRolNoPuedeSuscribirseATemasAjenos() throws Exception {
        Sesion mesero = crearUsuario("MESERO");
        Sesion otro = crearUsuario("MESERO");
        long producto = crearProducto("8000");

        StompSession ws = conectar(mesero.token());
        BlockingQueue<String> intruso = suscribir(ws, "/topic/cocina");
        Thread.sleep(1000);
        assertFalse(ws.isConnected(), "la suscripción no autorizada debe cerrar la conexión");

        crearPedidoSalon(otro, producto, 1);
        assertNull(intruso.poll(1, TimeUnit.SECONDS));
    }
}
