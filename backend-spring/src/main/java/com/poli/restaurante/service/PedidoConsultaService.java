package com.poli.restaurante.service;

import com.poli.restaurante.config.AppProperties;
import com.poli.restaurante.domain.EstadoPedido;
import com.poli.restaurante.domain.Pedido;
import com.poli.restaurante.domain.Rol;
import com.poli.restaurante.domain.TipoPedido;
import com.poli.restaurante.exception.ResourceNotFoundException;
import com.poli.restaurante.repository.HistorialEstadoRepository;
import com.poli.restaurante.repository.PedidoRepository;
import com.poli.restaurante.repository.PedidoSpecs;
import com.poli.restaurante.security.UsuarioActual;
import com.poli.restaurante.web.dto.PageResponse;
import com.poli.restaurante.web.dto.PedidoDtos.HistorialResponse;
import com.poli.restaurante.web.dto.PedidoDtos.PedidoResponse;
import com.poli.restaurante.web.dto.PedidoDtos.PedidoResumenResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Consultas de pedidos para mesero, cocina y administración. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PedidoConsultaService {

    private static final EnumSet<EstadoPedido> COLA_COCINA = EnumSet.of(EstadoPedido.PENDIENTE, EstadoPedido.EN_PREPARACION);

    private final PedidoRepository pedidos;
    private final HistorialEstadoRepository historial;
    private final AppProperties props;

    /** Mesero: solo los suyos. Cocina y administrador: cualquiera. */
    public PedidoResponse obtener(Long id, UsuarioActual actor) {
        Pedido p = detallado(id);
        exigirLectura(p, actor);
        return PedidoResponse.de(p);
    }

    public List<PedidoResponse> colaCocina() {
        return pedidos.findCola(COLA_COCINA).stream().map(PedidoResponse::de).toList();
    }

    public PageResponse<PedidoResumenResponse> mios(EstadoPedido estado, Pageable pageable, UsuarioActual actor) {
        var spec = PedidoSpecs.filtro(estado, null, actor.id(), null, null, null);
        return PageResponse.de(pedidos.findAll(spec, pageable), PedidoResumenResponse::de);
    }

    public List<HistorialResponse> historial(Long id, UsuarioActual actor) {
        Pedido p = pedidos.findById(id).orElseThrow(() -> ResourceNotFoundException.de("Pedido", id));
        exigirLectura(p, actor);
        return historial.findByPedidoIdOrderByCreatedAtAscIdAsc(id).stream().map(HistorialResponse::de).toList();
    }

    // ---------------------------------------------------------- administración

    public PageResponse<PedidoResumenResponse> listarAdmin(EstadoPedido estado, TipoPedido tipo, Long meseroId,
                                                           Long mesaId, LocalDate desde, LocalDate hasta,
                                                           Pageable pageable) {
        Instant inicio = desde == null ? null : desde.atStartOfDay(props.zoneId()).toInstant();
        Instant fin = hasta == null ? null : hasta.plusDays(1).atStartOfDay(props.zoneId()).toInstant();
        var spec = PedidoSpecs.filtro(estado, tipo, meseroId, mesaId, inicio, fin);
        return PageResponse.de(pedidos.findAll(spec, pageable), PedidoResumenResponse::de);
    }

    public PedidoResponse obtenerAdmin(Long id) {
        return PedidoResponse.de(detallado(id));
    }

    private Pedido detallado(Long id) {
        return pedidos.findDetalladoById(id).orElseThrow(() -> ResourceNotFoundException.de("Pedido", id));
    }

    private static void exigirLectura(Pedido p, UsuarioActual actor) {
        if (actor.rol() == Rol.MESERO && !p.getMesero().getId().equals(actor.id())) {
            throw new AccessDeniedException("Un mesero solo puede consultar sus propios pedidos");
        }
    }
}
