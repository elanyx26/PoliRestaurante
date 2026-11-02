package com.poli.restaurante.web.controller;

import com.poli.restaurante.domain.EstadoMesa;
import com.poli.restaurante.service.MesaService;
import com.poli.restaurante.web.dto.CatalogoDtos.MesaRequest;
import com.poli.restaurante.web.dto.CatalogoDtos.MesaResponse;
import com.poli.restaurante.web.dto.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/mesas")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRADOR')")
@Tag(name = "Administración de mesas")
public class AdminMesaController {

    private final MesaService mesas;

    @GetMapping
    public PageResponse<MesaResponse> listar(
            @RequestParam(required = false) EstadoMesa estado,
            @PageableDefault(size = 50, sort = "numero", direction = Sort.Direction.ASC) Pageable pageable) {
        return mesas.listar(estado, pageable);
    }

    @PostMapping
    public ResponseEntity<MesaResponse> crear(@Valid @RequestBody MesaRequest req) {
        MesaResponse creada = mesas.crear(req);
        return ResponseEntity.created(URI.create("/api/admin/mesas/" + creada.id())).body(creada);
    }

    @PutMapping("/{id}")
    public MesaResponse actualizar(@PathVariable Long id, @Valid @RequestBody MesaRequest req) {
        return mesas.actualizar(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        mesas.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
