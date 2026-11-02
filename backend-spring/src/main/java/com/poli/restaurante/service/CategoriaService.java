package com.poli.restaurante.service;

import com.poli.restaurante.domain.Categoria;
import com.poli.restaurante.exception.ConflictException;
import com.poli.restaurante.exception.ResourceNotFoundException;
import com.poli.restaurante.repository.CategoriaRepository;
import com.poli.restaurante.repository.ProductoRepository;
import com.poli.restaurante.web.dto.CatalogoDtos.CategoriaRequest;
import com.poli.restaurante.web.dto.CatalogoDtos.CategoriaResponse;
import com.poli.restaurante.web.dto.PageResponse;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoriaService {

    private final CategoriaRepository categorias;
    private final ProductoRepository productos;

    @Transactional(readOnly = true)
    public List<CategoriaResponse> menu() {
        return categorias.findByActivaTrueOrderByNombreAsc().stream().map(CategoriaResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<CategoriaResponse> listar(String q, Boolean activa, Pageable pageable) {
        Specification<Categoria> spec = (root, query, cb) -> {
            var ps = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (q != null && !q.isBlank()) {
                ps.add(cb.like(cb.lower(root.get("nombre")), "%" + q.trim().toLowerCase(Locale.ROOT) + "%"));
            }
            if (activa != null) {
                ps.add(cb.equal(root.get("activa"), activa));
            }
            return cb.and(ps.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        return PageResponse.de(categorias.findAll(spec, pageable), CategoriaResponse::de);
    }

    public CategoriaResponse crear(CategoriaRequest req) {
        String nombre = req.nombre().trim();
        if (categorias.existsByNombreIgnoreCase(nombre)) {
            throw new ConflictException("Ya existe una categoría llamada '" + nombre + "'");
        }
        Categoria c = new Categoria();
        aplicar(c, req, nombre);
        return CategoriaResponse.de(categorias.saveAndFlush(c));
    }

    public CategoriaResponse actualizar(Long id, CategoriaRequest req) {
        Categoria c = buscar(id);
        String nombre = req.nombre().trim();
        if (categorias.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new ConflictException("Ya existe una categoría llamada '" + nombre + "'");
        }
        aplicar(c, req, nombre);
        return CategoriaResponse.de(categorias.saveAndFlush(c));
    }

    public void eliminar(Long id) {
        Categoria c = buscar(id);
        if (productos.existsByCategoriaId(id)) {
            throw new ConflictException("La categoría tiene productos; muévalos o desactive la categoría");
        }
        categorias.delete(c);
    }

    private void aplicar(Categoria c, CategoriaRequest req, String nombre) {
        c.setNombre(nombre);
        c.setDescripcion(req.descripcion());
        if (req.activa() != null) {
            c.setActiva(req.activa());
        }
    }

    private Categoria buscar(Long id) {
        return categorias.findById(id).orElseThrow(() -> ResourceNotFoundException.de("Categoría", id));
    }
}
