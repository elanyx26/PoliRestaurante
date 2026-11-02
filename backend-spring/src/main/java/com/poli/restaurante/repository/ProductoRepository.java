package com.poli.restaurante.repository;

import com.poli.restaurante.domain.Producto;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface ProductoRepository extends JpaRepository<Producto, Long>, JpaSpecificationExecutor<Producto> {

    boolean existsByCategoriaId(Long categoriaId);

    boolean existsByCategoriaIdAndNombreIgnoreCase(Long categoriaId, String nombre);

    boolean existsByCategoriaIdAndNombreIgnoreCaseAndIdNot(Long categoriaId, String nombre, Long id);

    @Override
    @EntityGraph(attributePaths = "categoria")
    Page<Producto> findAll(Specification<Producto> spec, Pageable pageable);

    @Query("""
            select p from Producto p join fetch p.categoria c
            where p.disponible = true and c.activa = true
              and (:categoriaId is null or c.id = :categoriaId)
            order by c.nombre, p.nombre
            """)
    List<Producto> findMenu(Long categoriaId);
}
