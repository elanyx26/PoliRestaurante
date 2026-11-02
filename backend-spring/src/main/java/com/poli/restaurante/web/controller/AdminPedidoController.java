package com.poli.restaurante.web.controller;

import com.poli.restaurante.domain.EstadoPedido;
import com.poli.restaurante.domain.TipoPedido;
import com.poli.restaurante.service.PedidoConsultaService;
import com.poli.restaurante.web.dto.PageResponse;
import com.poli.restaurante.web.dto.PedidoDtos.PedidoResponse;
import com.poli.restaurante.web.dto.PedidoDtos.PedidoResumenResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Consulta administrativa de pedidos: solo lectura. */
@RestController
@RequestMapping("/api/admin/pedidos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRADOR')")
@Tag(name = "Administración de pedidos")
public class AdminPedidoController {

    private final PedidoConsultaService consultas;

    @GetMapping
    public PageResponse<PedidoResumenResponse> listar(
            @RequestParam(required = false) EstadoPedido estado,
            @RequestParam(required = false) TipoPedido tipo,
            @RequestParam(required = false) Long meseroId,
            @RequestParam(required = false) Long mesaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return consultas.listarAdmin(estado, tipo, meseroId, mesaId, desde, hasta, pageable);
    }

    @GetMapping("/{id}")
    public PedidoResponse obtener(@PathVariable Long id) {
        return consultas.obtenerAdmin(id);
    }
}
