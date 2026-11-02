package com.poli.restaurante.web.controller;

import com.poli.restaurante.domain.EstadoPedido;
import com.poli.restaurante.security.UsuarioActual;
import com.poli.restaurante.service.PedidoConsultaService;
import com.poli.restaurante.service.PedidoService;
import com.poli.restaurante.web.dto.PageResponse;
import com.poli.restaurante.web.dto.PedidoDtos.ActualizarDetalleRequest;
import com.poli.restaurante.web.dto.PedidoDtos.CambiarEstadoPedidoRequest;
import com.poli.restaurante.web.dto.PedidoDtos.CrearPedidoRequest;
import com.poli.restaurante.web.dto.PedidoDtos.HistorialResponse;
import com.poli.restaurante.web.dto.PedidoDtos.ItemPedidoRequest;
import com.poli.restaurante.web.dto.PedidoDtos.PedidoResponse;
import com.poli.restaurante.web.dto.PedidoDtos.PedidoResumenResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
@Tag(name = "Pedidos (mesero y cocina)")
public class PedidoController {

    private final PedidoService comandos;
    private final PedidoConsultaService consultas;

    @PostMapping
    @PreAuthorize("hasRole('MESERO')")
    public ResponseEntity<PedidoResponse> crear(@Valid @RequestBody CrearPedidoRequest req,
                                                @AuthenticationPrincipal UsuarioActual actor) {
        PedidoResponse creado = comandos.crear(req, actor);
        return ResponseEntity.created(URI.create("/api/pedidos/" + creado.id())).body(creado);
    }

    @GetMapping("/cocina")
    @PreAuthorize("hasRole('COCINA')")
    public List<PedidoResponse> colaCocina() {
        return consultas.colaCocina();
    }

    @GetMapping("/mios")
    @PreAuthorize("hasRole('MESERO')")
    public PageResponse<PedidoResumenResponse> mios(
            @RequestParam(required = false) EstadoPedido estado,
            @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC)
            Pageable pageable,
            @AuthenticationPrincipal UsuarioActual actor) {
        return consultas.mios(estado, pageable, actor);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MESERO','COCINA','ADMINISTRADOR')")
    public PedidoResponse obtener(@PathVariable Long id, @AuthenticationPrincipal UsuarioActual actor) {
        return consultas.obtener(id, actor);
    }

    @GetMapping("/{id}/historial")
    @PreAuthorize("hasAnyRole('MESERO','COCINA','ADMINISTRADOR')")
    public List<HistorialResponse> historial(@PathVariable Long id, @AuthenticationPrincipal UsuarioActual actor) {
        return consultas.historial(id, actor);
    }

    @PostMapping("/{id}/detalles")
    @PreAuthorize("hasRole('MESERO')")
    public ResponseEntity<PedidoResponse> agregarDetalle(@PathVariable Long id,
                                                         @Valid @RequestBody ItemPedidoRequest req,
                                                         @AuthenticationPrincipal UsuarioActual actor) {
        return ResponseEntity.status(201).body(comandos.agregarDetalle(id, req, actor));
    }

    @PutMapping("/{pedidoId}/detalles/{detalleId}")
    @PreAuthorize("hasRole('MESERO')")
    public PedidoResponse actualizarDetalle(@PathVariable Long pedidoId, @PathVariable Long detalleId,
                                            @Valid @RequestBody ActualizarDetalleRequest req,
                                            @AuthenticationPrincipal UsuarioActual actor) {
        return comandos.actualizarDetalle(pedidoId, detalleId, req, actor);
    }

    @DeleteMapping("/{pedidoId}/detalles/{detalleId}")
    @PreAuthorize("hasRole('MESERO')")
    public PedidoResponse eliminarDetalle(@PathVariable Long pedidoId, @PathVariable Long detalleId,
                                          @AuthenticationPrincipal UsuarioActual actor) {
        return comandos.eliminarDetalle(pedidoId, detalleId, actor);
    }

    /** El administrador queda fuera a propósito: no puede cambiar el estado operativo. */
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('MESERO','COCINA')")
    public PedidoResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambiarEstadoPedidoRequest req,
                                        @AuthenticationPrincipal UsuarioActual actor) {
        return comandos.cambiarEstado(id, req, actor);
    }
}
