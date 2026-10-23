package servicios;

import java.util.List;
import dao.DetallePedidoDAO;
import dao.ProductoDAO;
import entidades.Producto;
import util.Constantes;
import util.Validar;

/** HU5 / RF-10: gestión de productos del menú (tabla Producto). */
public class ProductoService {
    private final ProductoDAO dao = new ProductoDAO();

    public Producto crear(Producto entidad) {
        validar(entidad);
        dao.crear(entidad);
        hidratar(entidad);
        return entidad;
    }

    public Producto obtenerPorId(int id_producto) {
        Producto p = dao.obtenerPorId(id_producto);
        if (p != null) {
            hidratar(p);
        }
        return p;
    }

    public List<Producto> listar() {
        return dao.obtenerTodos();
    }

    public Producto actualizar(Producto entidad) {
        Validar.noNulo(entidad, "Producto");
        if (obtenerPorId(entidad.getId_producto()) == null) {
            throw new IllegalArgumentException("No existe un producto con id " + entidad.getId_producto());
        }
        validar(entidad);
        dao.actualizar(entidad);
        hidratar(entidad);
        return entidad;
    }

    /**
     * Elimina el producto. Si ya fue usado en algún pedido no se borra (para no
     * romper el historial): queda marcado como "No disponible".
     */
    public void eliminar(int id_producto) {
        if (obtenerPorId(id_producto) == null) {
            return;
        }
        if (new DetallePedidoDAO().contarPorProducto(id_producto) > 0) {
            dao.actualizarDisponibilidad(id_producto, Constantes.NO_DISPONIBLE);
            return;
        }
        dao.eliminar(id_producto);
    }

    // ---------------------------------------------------------------- extras

    /** HU5: los productos "No disponible" no aparecen al crear pedidos. */
    public List<Producto> listarDisponibles() {
        return dao.obtenerPorDisponibilidad(Constantes.DISPONIBLE);
    }

    public boolean estaDisponible(Producto p) {
        return p != null && Constantes.DISPONIBLE.equalsIgnoreCase(p.getDisponibilidad());
    }

    private void validar(Producto p) {
        Validar.noNulo(p, "Producto");
        p.setNombre(Validar.texto(p.getNombre(), "nombre"));
        Validar.positivo(p.getPrecio(), "precio");
        if (new CategoriaService().obtenerPorId(p.getId_categoria()) == null) {
            throw new IllegalArgumentException("El producto requiere una categoría existente");
        }
        if (new AdministradorService().obtenerPorId(p.getId_administrador()) == null) {
            throw new IllegalArgumentException("El administrador indicado no existe");
        }
        if (p.getDisponibilidad() == null || p.getDisponibilidad().trim().isEmpty()) {
            p.setDisponibilidad(Constantes.DISPONIBLE);
        }
        if (!Constantes.DISPONIBLE.equalsIgnoreCase(p.getDisponibilidad())
                && !Constantes.NO_DISPONIBLE.equalsIgnoreCase(p.getDisponibilidad())) {
            throw new IllegalArgumentException("La disponibilidad solo puede ser 'Disponible' o 'No disponible'");
        }
    }

    private void hidratar(Producto p) {
        p.setCategoria(new CategoriaService().obtenerPorId(p.getId_categoria()));
        p.setAdministrador(new AdministradorService().obtenerPorId(p.getId_administrador()));
    }
}
