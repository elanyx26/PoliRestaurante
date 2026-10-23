package servicios;

import java.util.List;
import dao.CategoriaDAO;
import dao.ProductoDAO;
import entidades.Categoria;
import util.Validar;

/** HU18 / RF-10: gestión de categorías del menú (tabla Categoria). */
public class CategoriaService {
    private final CategoriaDAO dao = new CategoriaDAO();

    public Categoria crear(Categoria entidad) {
        validar(entidad, 0);
        dao.crear(entidad);
        hidratar(entidad);
        return entidad;
    }

    public Categoria obtenerPorId(int id_categoria) {
        Categoria c = dao.obtenerPorId(id_categoria);
        if (c != null) {
            hidratar(c);
        }
        return c;
    }

    public List<Categoria> listar() {
        return dao.obtenerTodos();
    }

    /** Al editar, los productos conservan la relación (se enlazan por id_categoria). */
    public Categoria actualizar(Categoria entidad) {
        Validar.noNulo(entidad, "Categoria");
        if (obtenerPorId(entidad.getId_categoria()) == null) {
            throw new IllegalArgumentException("No existe una categoría con id " + entidad.getId_categoria());
        }
        validar(entidad, entidad.getId_categoria());
        dao.actualizar(entidad);
        hidratar(entidad);
        return entidad;
    }

    public void eliminar(int id_categoria) {
        if (obtenerPorId(id_categoria) == null) {
            return;
        }
        if (new ProductoDAO().contarPorCategoria(id_categoria) > 0) {
            throw new IllegalStateException("La categoría tiene productos asociados");
        }
        dao.eliminar(id_categoria);
    }

    private void validar(Categoria c, int idPropio) {
        Validar.noNulo(c, "Categoria");
        c.setNombre(Validar.texto(c.getNombre(), "nombre"));
        if (dao.contarPorNombre(c.getNombre(), idPropio) > 0) {
            throw new IllegalArgumentException("Ya existe una categoría llamada '" + c.getNombre() + "'");
        }
        if (new AdministradorService().obtenerPorId(c.getId_administrador()) == null) {
            throw new IllegalArgumentException("El administrador indicado no existe");
        }
    }

    private void hidratar(Categoria c) {
        c.setAdministrador(new AdministradorService().obtenerPorId(c.getId_administrador()));
    }
}
