package com.poli.restaurante.report;

import com.poli.restaurante.domain.EstadoPedido;
import com.poli.restaurante.domain.TipoPedido;
import com.poli.restaurante.web.dto.ReporteDtos.CancelacionesPorEstado;
import com.poli.restaurante.web.dto.ReporteDtos.CancelacionesPorMesero;
import com.poli.restaurante.web.dto.ReporteDtos.ProductoVendido;
import com.poli.restaurante.web.dto.ReporteDtos.TiempoPorTipo;
import com.poli.restaurante.web.dto.ReporteDtos.VentasPorDia;
import com.poli.restaurante.web.dto.ReporteDtos.VentasPorTipo;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** Consultas de agregación en SQL puro: son de solo lectura y más claras y rápidas que armarlas con JPA. */
@Repository
@RequiredArgsConstructor
public class ReporteRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public record ResumenVentas(long pedidos, BigDecimal total, BigDecimal envios) {
    }

    public record ResumenTiempos(long pedidos, Double promedio, Double minimo, Double maximo, Double p90) {
    }

    public record ResumenCancelaciones(long total, BigDecimal valor) {
    }

    private static MapSqlParameterSource rango(OffsetDateTime inicio, OffsetDateTime fin) {
        return new MapSqlParameterSource().addValue("inicio", inicio).addValue("fin", fin);
    }

    // ---------------------------------------------------------------- ventas

    public ResumenVentas resumenVentas(OffsetDateTime inicio, OffsetDateTime fin) {
        return jdbc.queryForObject("""
                SELECT count(*) AS pedidos, COALESCE(sum(total), 0) AS total, COALESCE(sum(costo_envio), 0) AS envios
                FROM pedidos
                WHERE estado = 'ENTREGADO' AND entregado_en >= :inicio AND entregado_en < :fin
                """, rango(inicio, fin),
                (rs, i) -> new ResumenVentas(rs.getLong("pedidos"), rs.getBigDecimal("total"), rs.getBigDecimal("envios")));
    }

    public List<VentasPorTipo> ventasPorTipo(OffsetDateTime inicio, OffsetDateTime fin) {
        return jdbc.query("""
                SELECT tipo, count(*) AS pedidos, sum(total) AS total
                FROM pedidos
                WHERE estado = 'ENTREGADO' AND entregado_en >= :inicio AND entregado_en < :fin
                GROUP BY tipo ORDER BY tipo
                """, rango(inicio, fin),
                (rs, i) -> new VentasPorTipo(TipoPedido.valueOf(rs.getString("tipo")), rs.getLong("pedidos"),
                        rs.getBigDecimal("total")));
    }

    public List<VentasPorDia> ventasPorDia(OffsetDateTime inicio, OffsetDateTime fin, String zona) {
        return jdbc.query("""
                SELECT CAST(entregado_en AT TIME ZONE :zona AS date) AS fecha, count(*) AS pedidos, sum(total) AS total
                FROM pedidos
                WHERE estado = 'ENTREGADO' AND entregado_en >= :inicio AND entregado_en < :fin
                GROUP BY fecha ORDER BY fecha
                """, rango(inicio, fin).addValue("zona", zona),
                (rs, i) -> new VentasPorDia(rs.getObject("fecha", java.time.LocalDate.class), rs.getLong("pedidos"),
                        rs.getBigDecimal("total")));
    }

    // ------------------------------------------------------ productos vendidos

    public List<ProductoVendido> productosMasVendidos(OffsetDateTime inicio, OffsetDateTime fin, int limite) {
        return jdbc.query("""
                SELECT pr.id, pr.nombre, sum(d.cantidad) AS unidades, sum(d.cantidad * d.precio_unitario) AS ingresos
                FROM detalles_pedido d
                JOIN pedidos p ON p.id = d.pedido_id
                JOIN productos pr ON pr.id = d.producto_id
                WHERE p.estado = 'ENTREGADO' AND p.entregado_en >= :inicio AND p.entregado_en < :fin
                GROUP BY pr.id, pr.nombre
                ORDER BY unidades DESC, ingresos DESC, pr.nombre
                LIMIT :limite
                """, rango(inicio, fin).addValue("limite", limite),
                (rs, i) -> new ProductoVendido(rs.getLong("id"), rs.getString("nombre"), rs.getLong("unidades"),
                        rs.getBigDecimal("ingresos")));
    }

    // ---------------------------------------------------------- cancelaciones

    public ResumenCancelaciones resumenCancelaciones(OffsetDateTime inicio, OffsetDateTime fin) {
        return jdbc.queryForObject("""
                SELECT count(*) AS total, COALESCE(sum(p.total), 0) AS valor
                FROM historial_estados h JOIN pedidos p ON p.id = h.pedido_id
                WHERE h.estado_nuevo = 'CANCELADO' AND h.created_at >= :inicio AND h.created_at < :fin
                """, rango(inicio, fin), (rs, i) -> new ResumenCancelaciones(rs.getLong("total"), rs.getBigDecimal("valor")));
    }

    public List<CancelacionesPorEstado> cancelacionesPorEstadoPrevio(OffsetDateTime inicio, OffsetDateTime fin) {
        return jdbc.query("""
                SELECT h.estado_anterior, count(*) AS cancelaciones
                FROM historial_estados h
                WHERE h.estado_nuevo = 'CANCELADO' AND h.created_at >= :inicio AND h.created_at < :fin
                GROUP BY h.estado_anterior ORDER BY h.estado_anterior
                """, rango(inicio, fin),
                (rs, i) -> new CancelacionesPorEstado(EstadoPedido.valueOf(rs.getString("estado_anterior")),
                        rs.getLong("cancelaciones")));
    }

    public List<CancelacionesPorMesero> cancelacionesPorMesero(OffsetDateTime inicio, OffsetDateTime fin) {
        return jdbc.query("""
                SELECT u.id, u.nombre, count(*) AS cancelaciones, COALESCE(sum(p.total), 0) AS valor
                FROM historial_estados h
                JOIN pedidos p ON p.id = h.pedido_id
                JOIN usuarios u ON u.id = h.usuario_id
                WHERE h.estado_nuevo = 'CANCELADO' AND h.created_at >= :inicio AND h.created_at < :fin
                GROUP BY u.id, u.nombre ORDER BY cancelaciones DESC, u.nombre
                """, rango(inicio, fin),
                (rs, i) -> new CancelacionesPorMesero(rs.getLong("id"), rs.getString("nombre"),
                        rs.getLong("cancelaciones"), rs.getBigDecimal("valor")));
    }

    // ----------------------------------------------------- tiempo de preparación

    private static final String TIEMPOS = """
            SELECT p.tipo AS tipo,
                   EXTRACT(EPOCH FROM (l.created_at - e.created_at)) AS segundos
            FROM historial_estados e
            JOIN historial_estados l ON l.pedido_id = e.pedido_id AND l.estado_nuevo = 'LISTO'
            JOIN pedidos p ON p.id = e.pedido_id
            WHERE e.estado_nuevo = 'EN_PREPARACION' AND l.created_at >= :inicio AND l.created_at < :fin
            """;

    public ResumenTiempos resumenTiempos(OffsetDateTime inicio, OffsetDateTime fin) {
        return jdbc.queryForObject("SELECT count(*) AS pedidos, avg(segundos) AS promedio, min(segundos) AS minimo, "
                        + "max(segundos) AS maximo, percentile_cont(0.9) WITHIN GROUP (ORDER BY segundos) AS p90 "
                        + "FROM (" + TIEMPOS + ") t", rango(inicio, fin),
                (rs, i) -> new ResumenTiempos(rs.getLong("pedidos"), decimal(rs, "promedio"), decimal(rs, "minimo"),
                        decimal(rs, "maximo"), decimal(rs, "p90")));
    }

    public List<TiempoPorTipo> tiemposPorTipo(OffsetDateTime inicio, OffsetDateTime fin) {
        return jdbc.query("SELECT tipo, count(*) AS pedidos, avg(segundos) AS promedio FROM (" + TIEMPOS
                        + ") t GROUP BY tipo ORDER BY tipo", rango(inicio, fin),
                (rs, i) -> new TiempoPorTipo(TipoPedido.valueOf(rs.getString("tipo")), rs.getLong("pedidos"),
                        rs.getDouble("promedio")));
    }

    private static Double decimal(ResultSet rs, String columna) throws SQLException {
        double valor = rs.getDouble(columna);
        return rs.wasNull() ? null : valor;
    }
}
