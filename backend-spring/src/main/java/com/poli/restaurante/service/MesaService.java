package com.poli.restaurante.service;

import com.poli.restaurante.domain.EstadoMesa;
import com.poli.restaurante.domain.Mesa;
import com.poli.restaurante.exception.ConflictException;
import com.poli.restaurante.exception.ResourceNotFoundException;
import com.poli.restaurante.repository.MesaRepository;
import com.poli.restaurante.repository.PedidoRepository;
import com.poli.restaurante.web.dto.CatalogoDtos.MesaRequest;
import com.poli.restaurante.web.dto.CatalogoDtos.MesaResponse;
import com.poli.restaurante.web.dto.PageResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MesaService {

    private static final int CAPACIDAD_POR_DEFECTO = 4;

    private final MesaRepository mesas;
    private final PedidoRepository pedidos;

    @Transactional(readOnly = true)
    public List<MesaResponse> disponibles() {
        return mesas.findByEstadoOrderByNumeroAsc(EstadoMesa.LIBRE).stream().map(MesaResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<MesaResponse> listar(EstadoMesa estado, Pageable pageable) {
        Specification<Mesa> spec = (root, query, cb) ->
                estado == null ? cb.conjunction() : cb.equal(root.get("estado"), estado);
        return PageResponse.de(mesas.findAll(spec, pageable), MesaResponse::de);
    }

    public MesaResponse crear(MesaRequest req) {
        if (mesas.existsByNumero(req.numero())) {
            throw new ConflictException("Ya existe la mesa " + req.numero());
        }
        Mesa m = new Mesa();
        m.setNumero(req.numero());
        m.setCapacidad(req.capacidad() != null ? req.capacidad() : CAPACIDAD_POR_DEFECTO);
        return MesaResponse.de(mesas.saveAndFlush(m));
    }

    /** El estado de la mesa lo gobiernan los pedidos; aquí solo se editan número y capacidad. */
    public MesaResponse actualizar(Long id, MesaRequest req) {
        Mesa m = buscar(id);
        if (mesas.existsByNumeroAndIdNot(req.numero(), id)) {
            throw new ConflictException("Ya existe la mesa " + req.numero());
        }
        m.setNumero(req.numero());
        if (req.capacidad() != null) {
            m.setCapacidad(req.capacidad());
        }
        return MesaResponse.de(mesas.saveAndFlush(m));
    }

    public void eliminar(Long id) {
        Mesa m = buscar(id);
        if (m.getEstado() == EstadoMesa.OCUPADA || pedidos.existsByMesaId(id)) {
            throw new ConflictException("La mesa está ocupada o tiene pedidos registrados");
        }
        mesas.delete(m);
    }

    private Mesa buscar(Long id) {
        return mesas.findById(id).orElseThrow(() -> ResourceNotFoundException.de("Mesa", id));
    }
}
