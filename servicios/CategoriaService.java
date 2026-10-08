package servicios;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import entidades.Categoria;
import util.Bd;
import util.Validar;

/** HU18 / RF-10: gestión de categorías del menú (tabla Categoria). */
public class CategoriaService {
    private static final String[] COLS = {"nombre", "id_administrador"};

    private static Categoria mapear(ResultSet rs) throws SQLException {
        Categoria c = new Categoria();
        c.setId_categoria(rs.getInt("id_categoria"));
        c.setNombre(rs.getString("nombre"));
        c.setId_administrador(rs.getInt("id_administrador"));
        return c;
    }

    private static Object[] valores(Categoria c) {
        return new Object[] {c.getNombre(), Bd.fk(c.getId_administrador())};
    }

    public Categoria crear(Categoria entidad) {
        validar(entidad, 0);
        entidad.setId_categoria(Bd.insertar("Categoria", "id_categoria", entidad.getId_categoria(), COLS, valores(entidad)));
        hidratar(entidad);
        return entidad;
    }

    public Categoria obtenerPorId(int id_categoria) {
        Categoria c = Bd.uno("SELECT * FROM Categoria WHERE id_categoria = ?", CategoriaService::mapear, id_categoria);
        if (c != null) {
            hidratar(c);
        }
        return c;
    }

    public List<Categoria> listar() {
        return Bd.consultar("SELECT * FROM Categoria ORDER BY nombre", CategoriaService::mapear);
    }

    /** Al editar, los productos conservan la relación (se enlazan por id_categoria). */
    public Categoria actualizar(Categoria entidad) {
        Validar.noNulo(entidad, "Categoria");
        if (obtenerPorId(entidad.getId_categoria()) == null) {
            throw new IllegalArgumentException("No existe una categoría con id " + entidad.getId_categoria());
        }
        validar(entidad, entidad.getId_categoria());
        Bd.actualizar("Categoria", "id_categoria", entidad.getId_categoria(), COLS, valores(entidad));
        hidratar(entidad);
        return entidad;
    }

    public void eliminar(int id_categoria) {
        if (obtenerPorId(id_categoria) == null) {
            return;
        }
        if (Bd.entero("SELECT COUNT(*) FROM Producto WHERE id_categoria = ?", id_categoria) > 0) {
            throw new IllegalStateException("La categoría tiene productos asociados");
        }
        Bd.eliminar("Categoria", "id_categoria", id_categoria);
    }

    private void validar(Categoria c, int idPropio) {
        Validar.noNulo(c, "Categoria");
        c.setNombre(Validar.texto(c.getNombre(), "nombre"));
        if (Bd.entero("SELECT COUNT(*) FROM Categoria WHERE LOWER(nombre) = LOWER(?) AND id_categoria <> ?",
                c.getNombre(), idPropio) > 0) {
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
