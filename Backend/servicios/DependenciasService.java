package servicios;

import java.util.ArrayList;
import java.util.List;
import dao.DependenciasDAO;

/**
 * Consulta qué registros están enlazados (por llave foránea) a otro registro.
 * Sirve para saber qué hay que desenlazar o eliminar antes de borrar a un actor.
 */
public class DependenciasService {

    private final DependenciasDAO dao = new DependenciasDAO();

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
                agregar(r, "Mesas registradas por él", dao.mesasDeAdministrador(id));
                agregar(r, "Categorías registradas por él", dao.categoriasDeAdministrador(id));
                agregar(r, "Productos registrados por él", dao.productosDeAdministrador(id));
                agregar(r, "Pedidos asociados a él", dao.pedidosDeAdministrador(id));
                break;
            case MESERO:
                agregar(r, "Pedidos atendidos por este mesero", dao.pedidosDeMesero(id));
                break;
            case COCINA:
                agregar(r, "Pedidos preparados por este usuario de cocina", dao.pedidosDeCocina(id));
                break;
            case MESA:
                agregar(r, "Pedidos hechos en esta mesa", dao.pedidosDeMesa(id));
                break;
            case CATEGORIA:
                agregar(r, "Productos de esta categoría", dao.productosDeCategoria(id));
                break;
            case PRODUCTO:
                agregar(r, "Líneas de pedido que usan este producto", dao.detallesDeProducto(id));
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

    private static void agregar(List<String> salida, String titulo, List<String> filas) {
        if (filas.isEmpty()) {
            return;
        }
        salida.add(titulo + " (" + filas.size() + "):");
        for (String f : filas) {
            salida.add("    - " + f);
        }
    }
}
