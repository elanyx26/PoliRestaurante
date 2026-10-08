package servicios;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import entidades.Producto;
import util.Bd;
import util.Constantes;
import util.Validar;

/** HU5 / RF-10: gestión de productos del menú (tabla Producto). */
public class ProductoService {
    private static final String[] COLS =
            {"nombre", "descripcion", "precio", "id_categoria", "id_administrador", "disponibilidad"};

    private static Producto mapear(ResultSet rs) throws SQLException {
        Producto p = new Producto();
        p.setId_producto(rs.getInt("id_producto"));
        p.setNombre(rs.getString("nombre"));
        p.setDescripcion(rs.getString("descripcion"));
        p.setPrecio(rs.getInt("precio"));
        p.setId_categoria(rs.getInt("id_categoria"));
        p.setId_administrador(rs.getInt("id_administrador"));
        p.setDisponibilidad(rs.getString("disponibilidad"));
        return p;
    }

    private static Object[] valores(Producto p) {
        return new Object[] {p.getNombre(), p.getDescripcion(), p.getPrecio(),
                Bd.fk(p.getId_categoria()), Bd.fk(p.getId_administrador()), p.getDisponibilidad()};
    }

    public Producto crear(Producto entidad) {
        validar(entidad);
        entidad.setId_producto(Bd.insertar("Producto", "id_producto", entidad.getId_producto(), COLS, valores(entidad)));
        hidratar(entidad);
        return entidad;
    }

    public Producto obtenerPorId(int id_producto) {
        Producto p = Bd.uno("SELECT * FROM Producto WHERE id_producto = ?", ProductoService::mapear, id_producto);
        if (p != null) {
            hidratar(p);
        }
        return p;
    }

    public List<Producto> listar() {
        return Bd.consultar("SELECT * FROM Producto ORDER BY id_producto", ProductoService::mapear);
    }

    public Producto actualizar(Producto entidad) {
        Validar.noNulo(entidad, "Producto");
        if (obtenerPorId(entidad.getId_producto()) == null) {
            throw new IllegalArgumentException("No existe un producto con id " + entidad.getId_producto());
        }
        validar(entidad);
        Bd.actualizar("Producto", "id_producto", entidad.getId_producto(), COLS, valores(entidad));
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
        if (Bd.entero("SELECT COUNT(*) FROM DetallePedido WHERE id_producto = ?", id_producto) > 0) {
            Bd.ejecutar("UPDATE Producto SET disponibilidad = ? WHERE id_producto = ?",
                    Constantes.NO_DISPONIBLE, id_producto);
            return;
        }
        Bd.eliminar("Producto", "id_producto", id_producto);
    }

    // ---------------------------------------------------------------- extras

    /** HU5: los productos "No disponible" no aparecen al crear pedidos. */
    public List<Producto> listarDisponibles() {
        return Bd.consultar("SELECT * FROM Producto WHERE LOWER(disponibilidad) = LOWER(?) ORDER BY nombre",
                ProductoService::mapear, Constantes.DISPONIBLE);
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
