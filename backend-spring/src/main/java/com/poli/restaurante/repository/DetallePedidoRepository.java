package com.poli.restaurante.repository;

import com.poli.restaurante.domain.DetallePedido;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetallePedidoRepository extends JpaRepository<DetallePedido, Long> {

    boolean existsByProductoId(Long productoId);
}
