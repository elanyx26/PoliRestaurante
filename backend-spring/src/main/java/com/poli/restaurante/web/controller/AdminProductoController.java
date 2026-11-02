package com.poli.restaurante.web.controller;

import com.poli.restaurante.service.ProductoService;
import com.poli.restaurante.web.dto.CatalogoDtos.ProductoRequest;
import com.poli.restaurante.web.dto.CatalogoDtos.ProductoResponse;
import com.poli.restaurante.web.dto.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
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
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/productos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRADOR')")
@Tag(name = "Administración de menú")
public class AdminProductoController {

    private final ProductoService productos;

    @GetMapping
    public PageResponse<ProductoResponse> listar(
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Boolean disponible,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "nombre", direction = Sort.Direction.ASC) Pageable pageable) {
        return productos.listar(categoriaId, disponible, q, pageable);
    }

    @PostMapping
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest req) {
        ProductoResponse creado = productos.crear(req);
        return ResponseEntity.created(URI.create("/api/admin/productos/" + creado.id())).body(creado);
    }

    @PutMapping("/{id}")
    public ProductoResponse actualizar(@PathVariable Long id, @Valid @RequestBody ProductoRequest req) {
        return productos.actualizar(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        productos.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    /** Sube o reemplaza la foto del producto (multipart, campo "archivo"; JPG, PNG o WebP de hasta 2 MB). */
    @PutMapping(value = "/{id}/imagen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProductoResponse subirImagen(@PathVariable Long id, @RequestParam("archivo") MultipartFile archivo) {
        return productos.actualizarImagen(id, archivo);
    }

    @DeleteMapping("/{id}/imagen")
    public ResponseEntity<Void> quitarImagen(@PathVariable Long id) {
        productos.eliminarImagen(id);
        return ResponseEntity.noContent().build();
    }
}
