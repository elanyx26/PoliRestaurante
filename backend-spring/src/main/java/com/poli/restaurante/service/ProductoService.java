package com.poli.restaurante.service;

import com.poli.restaurante.domain.Categoria;
import com.poli.restaurante.domain.Producto;
import com.poli.restaurante.exception.ConflictException;
import com.poli.restaurante.exception.ResourceNotFoundException;
import com.poli.restaurante.repository.CategoriaRepository;
import com.poli.restaurante.repository.DetallePedidoRepository;
import com.poli.restaurante.repository.ProductoRepository;
import com.poli.restaurante.web.dto.CatalogoDtos.ProductoRequest;
import com.poli.restaurante.web.dto.CatalogoDtos.ProductoResponse;
import com.poli.restaurante.web.dto.PageResponse;
import com.poli.restaurante.web.dto.PublicoDtos.CategoriaPublica;
import com.poli.restaurante.web.dto.PublicoDtos.MenuPublico;
import com.poli.restaurante.web.dto.PublicoDtos.ProductoPublico;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductoService {

    private final ProductoRepository productos;
    private final CategoriaRepository categorias;
    private final DetallePedidoRepository detalles;
    private final AlmacenImagenes imagenes;

    /** Menú visible para el personal: solo productos disponibles de categorías activas. */
    @Transactional(readOnly = true)
    public List<ProductoResponse> menu(Long categoriaId) {
        return productos.findMenu(categoriaId).stream().map(ProductoResponse::de).toList();
    }

    /** Menú para la portada pública: categorías activas con sus productos disponibles, sin datos internos. */
    @Transactional(readOnly = true)
    public MenuPublico menuPublico() {
        Map<Long, List<ProductoPublico>> porCategoria = new LinkedHashMap<>();
        Map<Long, Categoria> categoriasPorId = new LinkedHashMap<>();
        for (Producto p : productos.findMenu(null)) {
            Categoria c = p.getCategoria();
            categoriasPorId.putIfAbsent(c.getId(), c);
            porCategoria.computeIfAbsent(c.getId(), k -> new ArrayList<>())
                    .add(new ProductoPublico(p.getId(), p.getNombre(), p.getDescripcion(), p.getPrecio(),
                            ProductoResponse.urlImagen(p)));
        }
        List<CategoriaPublica> resultado = porCategoria.entrySet().stream()
                .map(e -> {
                    Categoria c = categoriasPorId.get(e.getKey());
                    return new CategoriaPublica(c.getId(), c.getNombre(), c.getDescripcion(), e.getValue());
                })
                .toList();
        return new MenuPublico(resultado);
    }

    /** Reemplaza la foto del producto; la anterior se borra del disco. */
    public ProductoResponse actualizarImagen(Long id, MultipartFile archivo) {
        Producto p = buscar(id);
        String anterior = p.getImagen();
        p.setImagen(imagenes.guardar(archivo));
        ProductoResponse respuesta = ProductoResponse.de(productos.saveAndFlush(p));
        imagenes.borrar(anterior);
        return respuesta;
    }

    public void eliminarImagen(Long id) {
        Producto p = buscar(id);
        String anterior = p.getImagen();
        p.setImagen(null);
        productos.saveAndFlush(p);
        imagenes.borrar(anterior);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductoResponse> listar(Long categoriaId, Boolean disponible, String q, Pageable pageable) {
        Specification<Producto> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (categoriaId != null) {
                ps.add(cb.equal(root.get("categoria").get("id"), categoriaId));
            }
            if (disponible != null) {
                ps.add(cb.equal(root.get("disponible"), disponible));
            }
            if (q != null && !q.isBlank()) {
                ps.add(cb.like(cb.lower(root.get("nombre")), "%" + q.trim().toLowerCase(Locale.ROOT) + "%"));
            }
            return cb.and(ps.toArray(Predicate[]::new));
        };
        return PageResponse.de(productos.findAll(spec, pageable), ProductoResponse::de);
    }

    public ProductoResponse crear(ProductoRequest req) {
        Categoria categoria = categoria(req.categoriaId());
        String nombre = req.nombre().trim();
        if (productos.existsByCategoriaIdAndNombreIgnoreCase(categoria.getId(), nombre)) {
            throw new ConflictException("Ya existe el producto '" + nombre + "' en la categoría");
        }
        Producto p = new Producto();
        aplicar(p, req, categoria, nombre);
        return ProductoResponse.de(productos.saveAndFlush(p));
    }

    public ProductoResponse actualizar(Long id, ProductoRequest req) {
        Producto p = buscar(id);
        Categoria categoria = categoria(req.categoriaId());
        String nombre = req.nombre().trim();
        if (productos.existsByCategoriaIdAndNombreIgnoreCaseAndIdNot(categoria.getId(), nombre, id)) {
            throw new ConflictException("Ya existe el producto '" + nombre + "' en la categoría");
        }
        aplicar(p, req, categoria, nombre);
        return ProductoResponse.de(productos.saveAndFlush(p));
    }

    public void eliminar(Long id) {
        Producto p = buscar(id);
        if (detalles.existsByProductoId(id)) {
            throw new ConflictException("El producto ya fue usado en pedidos; márquelo como no disponible");
        }
        productos.delete(p);
        imagenes.borrar(p.getImagen());
    }

    private void aplicar(Producto p, ProductoRequest req, Categoria categoria, String nombre) {
        p.setCategoria(categoria);
        p.setNombre(nombre);
        p.setDescripcion(req.descripcion());
        p.setPrecio(req.precio());
        if (req.disponible() != null) {
            p.setDisponible(req.disponible());
        }
    }

    private Producto buscar(Long id) {
        return productos.findById(id).orElseThrow(() -> ResourceNotFoundException.de("Producto", id));
    }

    private Categoria categoria(Long id) {
        return categorias.findById(id).orElseThrow(() -> ResourceNotFoundException.de("Categoría", id));
    }
}
