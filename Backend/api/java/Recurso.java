package api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Base de todos los recursos REST. Se encarga de: separar la ruta, leer el JSON del
 * cuerpo, responder JSON, CORS y convertir las excepciones de los servicios en
 * códigos HTTP (400 datos inválidos, 404 no existe, 409 conflicto, 500 error interno).
 */
public abstract class Recurso implements HttpHandler {
    private final String base;

    protected Recurso(String base) {
        this.base = base;
    }

    /**
     * @param metodo GET, POST, PUT, PATCH, DELETE
     * @param ruta   partes de la URL después de la base. /api/mesas/3/estado -> ["3","estado"]
     * @param cuerpo JSON recibido como mapa (vacío si no hay cuerpo)
     * @param query  parámetros ?clave=valor
     */
    protected abstract Respuesta manejar(String metodo, String[] ruta,
                                         Map<String, Object> cuerpo, Map<String, String> query);

    @Override
    public void handle(HttpExchange ex) throws IOException {
        Respuesta r;
        try {
            ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE,OPTIONS");
            ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
            String metodo = ex.getRequestMethod().toUpperCase();
            if (metodo.equals("OPTIONS")) {
                enviar(ex, Respuesta.sinContenido());
                return;
            }
            String path = ex.getRequestURI().getPath();
            String resto = path.length() > base.length() ? path.substring(base.length()) : "";
            r = manejar(metodo, partir(resto), leerCuerpo(ex), leerQuery(ex.getRequestURI().getRawQuery()));
        } catch (NoEncontrado e) {
            r = error(404, e.getMessage());
        } catch (IllegalArgumentException e) {
            String m = String.valueOf(e.getMessage());
            r = error(m.startsWith("No existe") ? 404 : 400, m);
        } catch (IllegalStateException e) {
            String m = String.valueOf(e.getMessage());
            // Errores de la base de datos (conexión, SQL) = 500; reglas de negocio = 409
            r = error(m.startsWith("Error de base de datos") ? 500 : 409, m);
        } catch (RuntimeException e) {
            e.printStackTrace();
            r = error(500, "Error interno: " + e.getMessage());
        }
        enviar(ex, r);
    }

    // ------------------------------------------------------------ utilidades

    /** 404 explícito. */
    protected static final class NoEncontrado extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public NoEncontrado(String mensaje) { super(mensaje); }
    }

    protected static Respuesta error(int estado, String mensaje) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("error", mensaje);
        return Respuesta.de(estado, m);
    }

    protected static Respuesta metodoNoPermitido() {
        return error(405, "Método o ruta no soportados");
    }

    /** Convierte el id de la URL a entero o lanza 400. */
    protected static int id(String texto) {
        try {
            return Integer.parseInt(texto);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El id '" + texto + "' no es un número válido");
        }
    }

    protected static int entero(Map<String, Object> m, String clave, int defecto) {
        Object v = m.get(clave);
        if (v == null) return defecto;
        if (v instanceof Number) return ((Number) v).intValue();
        try {
            return Integer.parseInt(v.toString().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El campo '" + clave + "' debe ser un número entero");
        }
    }

    protected static String texto(Map<String, Object> m, String clave) {
        Object v = m.get(clave);
        return v == null ? null : v.toString();
    }

    private static String[] partir(String resto) {
        String[] bruto = resto.split("/");
        java.util.List<String> l = new java.util.ArrayList<>();
        for (String p : bruto) {
            if (!p.isEmpty()) l.add(p);
        }
        return l.toArray(new String[0]);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> leerCuerpo(HttpExchange ex) throws IOException {
        String m = ex.getRequestMethod().toUpperCase();
        if (!m.equals("POST") && !m.equals("PUT") && !m.equals("PATCH")) {
            return new LinkedHashMap<>();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (InputStream in = ex.getRequestBody()) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        }
        String texto = new String(out.toByteArray(), StandardCharsets.UTF_8).trim();
        if (texto.isEmpty()) return new LinkedHashMap<>();
        Object o = Json.leer(texto);
        if (!(o instanceof Map)) {
            throw new IllegalArgumentException("El cuerpo debe ser un objeto JSON { ... }");
        }
        return (Map<String, Object>) o;
    }

    private static Map<String, String> leerQuery(String raw) {
        Map<String, String> q = new LinkedHashMap<>();
        if (raw == null || raw.isEmpty()) return q;
        for (String par : raw.split("&")) {
            int i = par.indexOf('=');
            String k = i < 0 ? par : par.substring(0, i);
            String v = i < 0 ? "" : par.substring(i + 1);
            q.put(URLDecoder.decode(k, StandardCharsets.UTF_8), URLDecoder.decode(v, StandardCharsets.UTF_8));
        }
        return q;
    }

    private static void enviar(HttpExchange ex, Respuesta r) throws IOException {
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        if (r.estado == 204 || r.cuerpo == null) {
            ex.sendResponseHeaders(r.estado, -1);
            ex.close();
            return;
        }
        byte[] datos = Json.escribir(r.cuerpo).getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(r.estado, datos.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(datos);
        }
    }
}
