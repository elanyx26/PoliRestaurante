package com.poli.restaurante.web.controller;

import com.poli.restaurante.service.ReporteService;
import com.poli.restaurante.web.dto.ReporteDtos.CancelacionesResponse;
import com.poli.restaurante.web.dto.ReporteDtos.ProductosMasVendidosResponse;
import com.poli.restaurante.web.dto.ReporteDtos.TiempoPreparacionResponse;
import com.poli.restaurante.web.dto.ReporteDtos.VentasResponse;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Reportes sobre rangos de fechas inclusivos (yyyy-MM-dd). Sin parámetros: últimos 30 días. */
@RestController
@RequestMapping("/api/admin/reportes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRADOR')")
@Tag(name = "Reportes")
public class AdminReporteController {

    private final ReporteService reportes;

    @GetMapping("/ventas")
    public VentasResponse ventas(
            @Parameter(description = "Inicio inclusivo, yyyy-MM-dd")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @Parameter(description = "Fin inclusivo, yyyy-MM-dd")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return reportes.ventas(desde, hasta);
    }

    @GetMapping("/productos-mas-vendidos")
    public ProductosMasVendidosResponse productosMasVendidos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "10") int limite) {
        return reportes.productosMasVendidos(desde, hasta, limite);
    }

    @GetMapping("/cancelaciones")
    public CancelacionesResponse cancelaciones(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return reportes.cancelaciones(desde, hasta);
    }

    @GetMapping("/tiempo-preparacion")
    public TiempoPreparacionResponse tiempoPreparacion(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return reportes.tiempoPreparacion(desde, hasta);
    }
}
