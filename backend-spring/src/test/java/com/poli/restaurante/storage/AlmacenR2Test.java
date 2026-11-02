package com.poli.restaurante.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Comprueba que AlmacenR2 habla el protocolo S3 con las opciones que Cloudflare R2 necesita (ruta con bucket, firma y
 * sin sumas de verificación nuevas). Usa un servidor S3 mínimo en memoria; no valida contra Cloudflare real.
 */
class AlmacenR2Test {

    private HttpServer servidor;
    private AlmacenR2 almacen;
    private final Map<String, byte[]> objetos = new ConcurrentHashMap<>();
    private final Map<String, String> tipos = new ConcurrentHashMap<>();
    private final List<Map<String, String>> cabeceras = new CopyOnWriteArrayList<>();

    @BeforeEach
    void iniciar() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", this::atender);
        servidor.start();
        almacen = new AlmacenR2("http://127.0.0.1:" + servidor.getAddress().getPort(), "fotos", "clave-id", "clave-secreta");
    }

    @AfterEach
    void detener() {
        almacen.close();
        servidor.stop(0);
    }

    private void atender(HttpExchange e) throws IOException {
        String ruta = e.getRequestURI().getPath(); // /fotos/<nombre>
        String metodo = e.getRequestMethod();
        Map<String, String> h = new ConcurrentHashMap<>();
        e.getRequestHeaders().forEach((k, v) -> h.put(k.toLowerCase(), String.join(",", v)));
        h.put("_metodo", metodo);
        h.put("_ruta", ruta);
        cabeceras.add(h);

        byte[] cuerpo = e.getRequestBody().readAllBytes();
        switch (metodo) {
            case "PUT" -> {
                objetos.put(ruta, cuerpo);
                tipos.put(ruta, h.getOrDefault("content-type", ""));
                e.getResponseHeaders().add("ETag", "\"abc\"");
                e.sendResponseHeaders(200, -1);
            }
            case "GET" -> {
                byte[] datos = objetos.get(ruta);
                if (datos == null) {
                    byte[] xml = "<?xml version=\"1.0\"?><Error><Code>NoSuchKey</Code><Message>no existe</Message></Error>"
                            .getBytes(StandardCharsets.UTF_8);
                    e.getResponseHeaders().add("Content-Type", "application/xml");
                    e.sendResponseHeaders(404, xml.length);
                    e.getResponseBody().write(xml);
                } else {
                    e.getResponseHeaders().add("Content-Type", tipos.getOrDefault(ruta, "application/octet-stream"));
                    e.sendResponseHeaders(200, datos.length);
                    e.getResponseBody().write(datos);
                }
            }
            case "DELETE" -> {
                objetos.remove(ruta);
                e.sendResponseHeaders(204, -1);
            }
            default -> e.sendResponseHeaders(405, -1);
        }
        e.close();
    }

    @Test
    void guardaLeeYBorraUsandoElBucketEnLaRuta() throws IOException {
        byte[] foto = {(byte) 0x89, 'P', 'N', 'G', 1, 2, 3, 4};
        almacen.guardar("a.png", foto, "image/png");

        assertArrayEquals(foto, objetos.get("/fotos/a.png"));
        assertEquals("image/png", tipos.get("/fotos/a.png"));
        assertArrayEquals(foto, almacen.leer("a.png").orElseThrow().getContentAsByteArray());

        almacen.borrar("a.png");
        assertTrue(almacen.leer("a.png").isEmpty());
    }

    @Test
    void unArchivoInexistenteEsVacioYNoUnError() {
        assertTrue(almacen.leer("no-existe.png").isEmpty());
    }

    @Test
    void laPeticionVaFirmadaYSinLasSumasDeVerificacionQueRechazaR2() {
        almacen.guardar("b.png", new byte[] {1, 2, 3}, "image/png");

        Map<String, String> put = cabeceras.stream().filter(h -> "PUT".equals(h.get("_metodo"))).findFirst().orElseThrow();
        assertTrue(put.get("authorization").startsWith("AWS4-HMAC-SHA256"), "debe ir firmada con SigV4");
        assertTrue(put.get("authorization").contains("clave-id"));
        assertTrue(put.get("authorization").contains("/auto/s3/"), "la región de R2 es 'auto'");
        assertFalse(put.keySet().stream().anyMatch(k -> k.startsWith("x-amz-checksum") || k.equals("x-amz-sdk-checksum-algorithm")),
                "R2 no acepta las sumas de verificación por defecto del SDK nuevo: " + put.keySet());
        assertFalse(put.getOrDefault("x-amz-content-sha256", "").startsWith("STREAMING"),
                "R2 no acepta la codificación por bloques");
    }

    @Test
    void sinConfiguracionCompletaFallaAlArrancarConUnMensajeClaro() {
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> new AlmacenR2("", "b", "k", "s"));
        assertTrue(e.getMessage().contains("R2_ENDPOINT"));
    }
}
