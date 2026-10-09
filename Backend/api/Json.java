package api;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Lector y escritor de JSON mínimo (sin librerías externas). */
public final class Json {
    private Json() { }

    // ------------------------------------------------------------ escribir

    public static String escribir(Object o) {
        StringBuilder sb = new StringBuilder();
        escribir(o, sb);
        return sb.toString();
    }

    private static void escribir(Object o, StringBuilder sb) {
        if (o == null) {
            sb.append("null");
        } else if (o instanceof String) {
            cadena((String) o, sb);
        } else if (o instanceof Number || o instanceof Boolean) {
            sb.append(o);
        } else if (o instanceof Map) {
            sb.append('{');
            boolean primero = true;
            for (Map.Entry<?, ?> e : ((Map<?, ?>) o).entrySet()) {
                if (!primero) sb.append(',');
                primero = false;
                cadena(String.valueOf(e.getKey()), sb);
                sb.append(':');
                escribir(e.getValue(), sb);
            }
            sb.append('}');
        } else if (o instanceof Iterable) {
            sb.append('[');
            boolean primero = true;
            for (Object x : (Iterable<?>) o) {
                if (!primero) sb.append(',');
                primero = false;
                escribir(x, sb);
            }
            sb.append(']');
        } else {
            cadena(o.toString(), sb);
        }
    }

    private static void cadena(String s, StringBuilder sb) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        sb.append('"');
    }

    // ------------------------------------------------------------- leer

    /** Devuelve Map, List, String, Long, Double, Boolean o null. */
    public static Object leer(String texto) {
        Lector l = new Lector(texto);
        l.espacios();
        Object v = l.valor();
        l.espacios();
        if (l.pos != texto.length()) throw l.error("Texto sobrante");
        return v;
    }

    private static final class Lector {
        final String s;
        int pos = 0;

        Lector(String s) { this.s = s; }

        IllegalArgumentException error(String msg) {
            return new IllegalArgumentException("JSON inválido (posición " + pos + "): " + msg);
        }

        void espacios() {
            while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) pos++;
        }

        Object valor() {
            if (pos >= s.length()) throw error("Fin inesperado");
            char c = s.charAt(pos);
            if (c == '{') return objeto();
            if (c == '[') return arreglo();
            if (c == '"') return texto();
            if (s.startsWith("true", pos)) { pos += 4; return Boolean.TRUE; }
            if (s.startsWith("false", pos)) { pos += 5; return Boolean.FALSE; }
            if (s.startsWith("null", pos)) { pos += 4; return null; }
            return numero();
        }

        Map<String, Object> objeto() {
            Map<String, Object> m = new LinkedHashMap<>();
            pos++; // {
            espacios();
            if (pos < s.length() && s.charAt(pos) == '}') { pos++; return m; }
            while (true) {
                espacios();
                if (pos >= s.length() || s.charAt(pos) != '"') throw error("Se esperaba el nombre de un campo");
                String k = texto();
                espacios();
                if (pos >= s.length() || s.charAt(pos) != ':') throw error("Se esperaba ':'");
                pos++;
                espacios();
                m.put(k, valor());
                espacios();
                if (pos >= s.length()) throw error("Fin inesperado");
                char c = s.charAt(pos++);
                if (c == '}') return m;
                if (c != ',') throw error("Se esperaba ',' o '}'");
            }
        }

        List<Object> arreglo() {
            List<Object> l = new ArrayList<>();
            pos++; // [
            espacios();
            if (pos < s.length() && s.charAt(pos) == ']') { pos++; return l; }
            while (true) {
                espacios();
                l.add(valor());
                espacios();
                if (pos >= s.length()) throw error("Fin inesperado");
                char c = s.charAt(pos++);
                if (c == ']') return l;
                if (c != ',') throw error("Se esperaba ',' o ']'");
            }
        }

        String texto() {
            pos++; // "
            StringBuilder sb = new StringBuilder();
            while (pos < s.length()) {
                char c = s.charAt(pos++);
                if (c == '"') return sb.toString();
                if (c == '\\') {
                    if (pos >= s.length()) break;
                    char e = s.charAt(pos++);
                    switch (e) {
                        case 'n': sb.append('\n'); break;
                        case 't': sb.append('\t'); break;
                        case 'r': sb.append('\r'); break;
                        case 'b': sb.append('\b'); break;
                        case 'f': sb.append('\f'); break;
                        case 'u':
                            if (pos + 4 > s.length()) throw error("Secuencia \\u incompleta");
                            sb.append((char) Integer.parseInt(s.substring(pos, pos + 4), 16));
                            pos += 4;
                            break;
                        default: sb.append(e); // \" \\ \/
                    }
                } else {
                    sb.append(c);
                }
            }
            throw error("Texto sin cerrar");
        }

        Object numero() {
            int ini = pos;
            while (pos < s.length() && "+-0123456789.eE".indexOf(s.charAt(pos)) >= 0) pos++;
            if (ini == pos) throw error("Valor no reconocido");
            String t = s.substring(ini, pos);
            try {
                if (t.contains(".") || t.contains("e") || t.contains("E")) return Double.valueOf(t);
                return Long.valueOf(t);
            } catch (NumberFormatException ex) {
                throw error("Número inválido '" + t + "'");
            }
        }
    }
}
