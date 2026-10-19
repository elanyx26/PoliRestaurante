package servicios;

import java.util.ArrayList;
import java.util.List;
import util.Bd;

/**
 * Consulta qué registros están enlazados (por llave foránea) a otro registro.
 * Sirve para saber qué hay que desenlazar o eliminar antes de borrar a un actor.
 */
public class DependenciasService {

    public static final String ADMINISTRADOR = "Administrador";
    public static final String MESERO = "Mesero";
    public static final String COCINA = "PersonalCocina";
    public static final String MESA = "Mesa";
    public static final String CATEGORIA = "Categoria";
    public static final String PRODUCTO = "Producto";

    /** Líneas de texto con lo que depende del registro. Lista vacía = no tiene enlaces. */
    public List<String> enlaces(String tipo, int id) {
        List<String> r = new ArrayList<>();
        switch (tipo) {
            case ADMINISTRADOR:
                agregar(r, "Mesas registradas por él",
                        "SELECT id_mesa, numero, estado FROM Mesa WHERE id_administrador = ?",
                        rs -> "mesa id " + rs.getInt("id_mesa") + " (#" + rs.getInt("numero")
                                + ", " + rs.getString("estado") + ")", id);
                agregar(r, "Categorías registradas por él",
                        "SELECT id_categoria, nombre FROM Categoria WHERE id_administrador = ?",
                        rs -> "categoría id " + rs.getInt("id_categoria") + " (" + rs.getString("nombre") + ")", id);
                agregar(r, "Productos registrados por él",
                        "SELECT id_producto, nombre FROM Producto WHERE id_administrador = ?",
                        rs -> "producto id " + rs.getInt("id_producto") + " (" + rs.getString("nombre") + ")", id);
                agregar(r, "Pedidos asociados a él",
                        "SELECT id_pedido, tipo_pedido, estado FROM Pedido WHERE id_administrador = ?",
                        DependenciasService::pedido, id);
                break;
            case MESERO:
                agregar(r, "Pedidos atendidos por este mesero",
                        "SELECT id_pedido, tipo_pedido, estado FROM Pedido WHERE id_mesero = ?",
                        DependenciasService::pedido, id);
                break;
            case COCINA:
                agregar(r, "Pedidos preparados por este usuario de cocina",
                        "SELECT id_pedido, tipo_pedido, estado FROM Pedido WHERE id_cocina = ?",
                        DependenciasService::pedido, id);
                break;
            case MESA:
                agregar(r, "Pedidos hechos en esta mesa",
                        "SELECT id_pedido, tipo_pedido, estado FROM Pedido WHERE id_mesa = ?",
                        DependenciasService::pedido, id);
                break;
            case CATEGORIA:
                agregar(r, "Productos de esta categoría",
                        "SELECT id_producto, nombre FROM Producto WHERE id_categoria = ?",
                        rs -> "producto id " + rs.getInt("id_producto") + " (" + rs.getString("nombre") + ")", id);
                break;
            case PRODUCTO:
                agregar(r, "Líneas de pedido que usan este producto",
                        "SELECT id_detalle, id_pedido, cantidad FROM DetallePedido WHERE id_producto = ?",
                        rs -> "detalle id " + rs.getInt("id_detalle") + " (pedido " + rs.getInt("id_pedido")
                                + ", cantidad " + rs.getInt("cantidad") + ")", id);
                break;
            default:
                throw new IllegalArgumentException("Tipo desconocido: " + tipo);
        }
        return r;
    }

    /** Qué hacer para poder eliminar este tipo de registro. */
    public String consejo(String tipo) {
        switch (tipo) {
            case ADMINISTRADOR:
                return "Elimine primero sus productos, categorías y mesas. Si tiene pedidos no se puede "
                        + "eliminar (son historial): déjelo desactivado.";
            case MESERO:
            case COCINA:
                return "Con pedidos asociados no se puede eliminar (son historial): déjelo desactivado.";
            case MESA:
                return "Con pedidos asociados no se puede eliminar (son historial).";
            case CATEGORIA:
                return "Elimine primero esos productos (o muévalos a otra categoría).";
            case PRODUCTO:
                return "No se borra: se marca como \"No disponible\" para conservar el historial.";
            default:
                return "";
        }
    }

    private static String pedido(java.sql.ResultSet rs) throws java.sql.SQLException {
        return "pedido id " + rs.getInt("id_pedido") + " (" + rs.getString("tipo_pedido")
                + ", " + rs.getString("estado") + ")";
    }

    private static void agregar(List<String> salida, String titulo, String sql, Bd.Mapeador<String> m, int id) {
        List<String> filas = Bd.consultar(sql, m, id);
        if (filas.isEmpty()) {
            return;
        }
        salida.add(titulo + " (" + filas.size() + "):");
        for (String f : filas) {
            salida.add("    - " + f);
        }
    }
}
