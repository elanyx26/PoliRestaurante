package util;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Conexión a la base de datos "restaurante".
 *
 * Configuración (en este orden de prioridad):
 *   1. Propiedades del sistema:  -Ddb.url=... -Ddb.user=... -Ddb.password=...
 *   2. Variables de entorno:     DB_URL, DB_USER, DB_PASSWORD
 *   3. Archivo db.properties en la carpeta desde donde se ejecuta el programa
 *   4. Valores por defecto (MySQL local, usuario root sin contraseña)
 */
public final class Conexion {
    private Conexion() { }

    private static final String URL_DEFECTO =
            "jdbc:mysql://localhost:3306/restaurante"
            + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8";

    private static final Properties ARCHIVO = cargarArchivo();
    private static boolean preparada = false;

    private static Properties cargarArchivo() {
        Properties p = new Properties();
        try (FileInputStream in = new FileInputStream("db.properties")) {
            p.load(in);
        } catch (IOException e) {
            // el archivo es opcional
        }
        return p;
    }

    private static String valor(String propiedad, String variable, String archivoClave, String defecto) {
        String v = System.getProperty(propiedad);
        if (v == null || v.isEmpty()) v = System.getenv(variable);
        if (v == null || v.isEmpty()) v = ARCHIVO.getProperty(archivoClave);
        return (v == null) ? defecto : v;
    }

    /** Abre una conexión nueva. Quien la use debe cerrarla (try-with-resources). */
    public static Connection abrir() throws SQLException {
        Connection c = DriverManager.getConnection(
                valor("db.url", "DB_URL", "url", URL_DEFECTO),
                valor("db.user", "DB_USER", "user", "root"),
                valor("db.password", "DB_PASSWORD", "password", "Poli123*"));
        if (!preparada) {
            prepararEsquema(c);
            preparada = true;
        }
        return c;
    }

    /**
     * Agrega (solo si no existen) los elementos que el modelo del Word necesita y que
     * el script SQL original no incluye. No modifica ni borra nada existente:
     *  - activo (usuarios)            -> HU19 activar / desactivar usuarios
     *  - Pedido.enviado_cocina        -> HU3 el pedido confirmado ya no se edita
     *  - tabla HistorialEstado        -> RF-15 / RNF-09 fecha y hora de cada cambio de estado
     */
    private static void prepararEsquema(Connection c) throws SQLException {
        for (String tabla : new String[] {"Mesero", "PersonalCocina", "Administrador"}) {
            agregarColumna(c, tabla, "activo", "TINYINT(1) NOT NULL DEFAULT 1");
        }
        agregarColumna(c, "Pedido", "enviado_cocina", "TINYINT(1) NOT NULL DEFAULT 0");
        try (Statement st = c.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS HistorialEstado ("
                    + "id_historial INT NOT NULL AUTO_INCREMENT, "
                    + "id_pedido INT NOT NULL, "
                    + "estado VARCHAR(255), "
                    + "fecha VARCHAR(255), "
                    + "PRIMARY KEY (id_historial), "
                    + "CONSTRAINT fk_historial_pedido FOREIGN KEY (id_pedido) REFERENCES Pedido(id_pedido)"
                    + ") ENGINE=InnoDB");
        }
    }

    private static void agregarColumna(Connection c, String tabla, String columna, String definicion)
            throws SQLException {
        String consulta = "SELECT COUNT(*) FROM information_schema.COLUMNS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = '" + tabla
                + "' AND COLUMN_NAME = '" + columna + "'";
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(consulta)) {
            rs.next();
            if (rs.getInt(1) == 0) {
                try (Statement alter = c.createStatement()) {
                    alter.executeUpdate("ALTER TABLE " + tabla + " ADD COLUMN " + columna + " " + definicion);
                }
            }
        }
    }
}
