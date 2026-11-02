package com.poli.restaurante.repository;

import com.poli.restaurante.domain.EstadoMesa;
import com.poli.restaurante.domain.Mesa;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface MesaRepository extends JpaRepository<Mesa, Long>, JpaSpecificationExecutor<Mesa> {

    List<Mesa> findByEstadoOrderByNumeroAsc(EstadoMesa estado);

    List<Mesa> findAllByOrderByNumeroAsc();

    boolean existsByNumero(Integer numero);

    boolean existsByNumeroAndIdNot(Integer numero, Long id);

    /** Bloqueo pesimista: evita que dos meseros tomen la misma mesa a la vez. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Mesa m where m.id = :id")
    Optional<Mesa> findByIdForUpdate(Long id);
}
