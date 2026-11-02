package com.poli.restaurante.repository;

import com.poli.restaurante.domain.EstadoPedido;
import com.poli.restaurante.domain.Pedido;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface PedidoRepository extends JpaRepository<Pedido, Long>, JpaSpecificationExecutor<Pedido> {

    /** Pedido completo con sus relaciones, para respuestas de detalle. */
    @Query("select p from Pedido p where p.id = :id")
    @EntityGraph(attributePaths = {"mesero", "mesa", "detalles", "detalles.producto"})
    Optional<Pedido> findDetalladoById(Long id);

    /** Bloquea la fila del pedido: serializa ediciones y cambios de estado concurrentes. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pedido p where p.id = :id")
    Optional<Pedido> findByIdForUpdate(Long id);

    @Query("select p from Pedido p where p.estado in :estados order by p.createdAt asc, p.id asc")
    @EntityGraph(attributePaths = {"mesero", "mesa", "detalles", "detalles.producto"})
    List<Pedido> findCola(Collection<EstadoPedido> estados);

    @Override
    @EntityGraph(attributePaths = {"mesero", "mesa"})
    Page<Pedido> findAll(Specification<Pedido> spec, Pageable pageable);

    boolean existsByMesaId(Long mesaId);
}
