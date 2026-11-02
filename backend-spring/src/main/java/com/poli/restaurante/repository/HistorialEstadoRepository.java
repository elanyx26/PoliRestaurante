package com.poli.restaurante.repository;

import com.poli.restaurante.domain.HistorialEstado;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistorialEstadoRepository extends JpaRepository<HistorialEstado, Long> {

    @EntityGraph(attributePaths = "usuario")
    List<HistorialEstado> findByPedidoIdOrderByCreatedAtAscIdAsc(Long pedidoId);
}
