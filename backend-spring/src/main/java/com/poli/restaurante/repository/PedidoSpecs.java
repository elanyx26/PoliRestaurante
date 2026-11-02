package com.poli.restaurante.repository;

import com.poli.restaurante.domain.EstadoPedido;
import com.poli.restaurante.domain.Pedido;
import com.poli.restaurante.domain.TipoPedido;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/** Filtros dinámicos de pedidos; los parámetros nulos se ignoran. Rango de fechas: [desde, hasta). */
public final class PedidoSpecs {

    private PedidoSpecs() {
    }

    public static Specification<Pedido> filtro(EstadoPedido estado, TipoPedido tipo, Long meseroId, Long mesaId,
                                               Instant desde, Instant hasta) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (estado != null) {
                ps.add(cb.equal(root.get("estado"), estado));
            }
            if (tipo != null) {
                ps.add(cb.equal(root.get("tipo"), tipo));
            }
            if (meseroId != null) {
                ps.add(cb.equal(root.get("mesero").get("id"), meseroId));
            }
            if (mesaId != null) {
                ps.add(cb.equal(root.get("mesa").get("id"), mesaId));
            }
            if (desde != null) {
                ps.add(cb.greaterThanOrEqualTo(root.get("createdAt"), desde));
            }
            if (hasta != null) {
                ps.add(cb.lessThan(root.get("createdAt"), hasta));
            }
            return cb.and(ps.toArray(Predicate[]::new));
        };
    }
}
