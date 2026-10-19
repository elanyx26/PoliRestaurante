package util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Utilidades JDBC para que los servicios no repitan código de conexión. */
public final class Bd {
    private Bd() {
    }

    /** Convierte una fila del ResultSet en un objeto. */
    public interface Mapeador<T> {
        T mapear(ResultSet rs) throws SQLException;
    }

    /** Las llaves foráneas opcionales se guardan como NULL cuando el id es 0. */
    public static Object fk(int id) {
        return id > 0 ? (Object) id : null;
    }

    public static <T> List<T> consultar(String sql, Mapeador<T> mapeador, Object... params) {
        try (Connection c = Conexion.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            asignar(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> r = new ArrayList<>();
                while (rs.next()) {
                    r.add(mapeador.mapear(rs));
                }
                return r;
            }
        } catch (SQLException e) {
            throw error(e);
        }
    }

    /** Primer resultado o null. */
    public static <T> T uno(String sql, Mapeador<T> mapeador, Object... params) {
        List<T> r = consultar(sql, mapeador, params);
        return r.isEmpty() ? null : r.get(0);
    }

    /** Primer valor entero de la primera fila (COUNT, SUM, etc.). */
    public static int entero(String sql, Object... params) {
        try (Connection c = Conexion.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            asignar(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw error(e);
        }
    }

    public static int ejecutar(String sql, Object... params) {
        try (Connection c = Conexion.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            asignar(ps, params);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw error(e);
        }
    }

    /**
     * INSERT genérico. Si id &gt; 0 se usa ese id (error si ya existe); si no, lo
     * genera
     * AUTO_INCREMENT. Devuelve el id del registro creado.
     */
    public static int insertar(String tabla, String colId, int id, String[] cols, Object[] vals) {
        List<String> c = new ArrayList<>(Arrays.asList(cols));
        List<Object> v = new ArrayList<>(Arrays.asList(vals));
        if (id > 0) {
            if (entero("SELECT COUNT(*) FROM " + tabla + " WHERE " + colId + " = ?", id) > 0) {
                throw new IllegalArgumentException("Ya existe un registro con id " + id);
            }
            c.add(0, colId);
            v.add(0, id);
        }
        StringBuilder marcas = new StringBuilder();
        for (int i = 0; i < c.size(); i++) {
            marcas.append(i == 0 ? "?" : ",?");
        }
        String sql = "INSERT INTO " + tabla + " (" + String.join(",", c) + ") VALUES (" + marcas + ")";
        try (Connection cn = Conexion.abrir();
                PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            asignar(ps, v.toArray());
            ps.executeUpdate();
            if (id > 0) {
                return id;
            }
            try (ResultSet k = ps.getGeneratedKeys()) {
                if (k.next()) {
                    return k.getInt(1);
                }
            }
            throw new IllegalStateException("La base de datos no devolvió el id generado");
        } catch (SQLException e) {
            throw error(e);
        }
    }

    /** UPDATE genérico por llave primaria. */
    public static void actualizar(String tabla, String colId, int id, String[] cols, Object[] vals) {
        StringBuilder set = new StringBuilder();
        for (int i = 0; i < cols.length; i++) {
            set.append(i == 0 ? "" : ", ").append(cols[i]).append(" = ?");
        }
        List<Object> params = new ArrayList<>(Arrays.asList(vals));
        params.add(id);
        ejecutar("UPDATE " + tabla + " SET " + set + " WHERE " + colId + " = ?", params.toArray());
    }

    public static void eliminar(String tabla, String colId, int id) {
        ejecutar("DELETE FROM " + tabla + " WHERE " + colId + " = ?", id);
    }

    private static void asignar(PreparedStatement ps, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }

    private static RuntimeException error(SQLException e) {
        return new IllegalStateException("Error de base de datos: " + e.getMessage(), e);
    }

    public static double real(String sql, Object... params) {
        try (Connection c = Conexion.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            asignar(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0.0;
            }
        } catch (SQLException e) {
            throw error(e);
        }
    }
}
