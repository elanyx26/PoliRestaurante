package com.poli.restaurante.service;

import com.poli.restaurante.config.AppProperties;
import com.poli.restaurante.exception.BusinessRuleException;
import com.poli.restaurante.report.ReporteRepository;
import com.poli.restaurante.report.ReporteRepository.ResumenCancelaciones;
import com.poli.restaurante.report.ReporteRepository.ResumenTiempos;
import com.poli.restaurante.report.ReporteRepository.ResumenVentas;
import com.poli.restaurante.web.dto.ReporteDtos.CancelacionesResponse;
import com.poli.restaurante.web.dto.ReporteDtos.ProductosMasVendidosResponse;
import com.poli.restaurante.web.dto.ReporteDtos.TiempoPreparacionResponse;
import com.poli.restaurante.web.dto.ReporteDtos.VentasResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReporteService {

    private static final int DIAS_POR_DEFECTO = 30;
    private static final int DIAS_MAXIMOS = 366;

    private final ReporteRepository repositorio;
    private final AppProperties props;
    private final Clock clock;

    private record Rango(LocalDate desde, LocalDate hasta, OffsetDateTime inicio, OffsetDateTime fin) {
    }

    /** Si no se indican fechas, usa los últimos 30 días. Ambos extremos son inclusivos, en la zona del restaurante. */
    private Rango rango(LocalDate desde, LocalDate hasta) {
        ZoneId zona = props.zoneId();
        LocalDate fin = hasta != null ? hasta : LocalDate.now(clock.withZone(zona));
        LocalDate inicio = desde != null ? desde : fin.minusDays(DIAS_POR_DEFECTO - 1L);
        if (inicio.isAfter(fin)) {
            throw new BusinessRuleException("'desde' no puede ser posterior a 'hasta'");
        }
        if (ChronoUnit.DAYS.between(inicio, fin) >= DIAS_MAXIMOS) {
            throw new BusinessRuleException("El rango máximo es de " + DIAS_MAXIMOS + " días");
        }
        return new Rango(inicio, fin,
                inicio.atStartOfDay(zona).toInstant().atOffset(ZoneOffset.UTC),
                fin.plusDays(1).atStartOfDay(zona).toInstant().atOffset(ZoneOffset.UTC));
    }

    public VentasResponse ventas(LocalDate desde, LocalDate hasta) {
        Rango r = rango(desde, hasta);
        ResumenVentas resumen = repositorio.resumenVentas(r.inicio(), r.fin());
        BigDecimal ticket = resumen.pedidos() == 0 ? BigDecimal.ZERO
                : resumen.total().divide(BigDecimal.valueOf(resumen.pedidos()), 2, RoundingMode.HALF_UP);
        return new VentasResponse(r.desde(), r.hasta(), resumen.pedidos(), resumen.total(), resumen.envios(), ticket,
                repositorio.ventasPorTipo(r.inicio(), r.fin()),
                repositorio.ventasPorDia(r.inicio(), r.fin(), props.timezone()));
    }

    public ProductosMasVendidosResponse productosMasVendidos(LocalDate desde, LocalDate hasta, int limite) {
        Rango r = rango(desde, hasta);
        int tope = Math.max(1, Math.min(limite, 100));
        return new ProductosMasVendidosResponse(r.desde(), r.hasta(),
                repositorio.productosMasVendidos(r.inicio(), r.fin(), tope));
    }

    public CancelacionesResponse cancelaciones(LocalDate desde, LocalDate hasta) {
        Rango r = rango(desde, hasta);
        ResumenCancelaciones resumen = repositorio.resumenCancelaciones(r.inicio(), r.fin());
        return new CancelacionesResponse(r.desde(), r.hasta(), resumen.total(), resumen.valor(),
                repositorio.cancelacionesPorEstadoPrevio(r.inicio(), r.fin()),
                repositorio.cancelacionesPorMesero(r.inicio(), r.fin()));
    }

    public TiempoPreparacionResponse tiempoPreparacion(LocalDate desde, LocalDate hasta) {
        Rango r = rango(desde, hasta);
        ResumenTiempos t = repositorio.resumenTiempos(r.inicio(), r.fin());
        return new TiempoPreparacionResponse(r.desde(), r.hasta(), t.pedidos(), t.promedio(), t.minimo(), t.maximo(),
                t.p90(), repositorio.tiemposPorTipo(r.inicio(), r.fin()));
    }
}
