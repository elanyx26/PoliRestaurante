package com.poli.restaurante.web.controller;

import com.poli.restaurante.service.AlmacenImagenes;
import com.poli.restaurante.service.ProductoService;
import com.poli.restaurante.web.dto.PublicoDtos.MenuPublico;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Contenido de la portada: no requiere autenticación y es de solo lectura. */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
@SecurityRequirements
@Tag(name = "Público (sin autenticación)")
public class PublicoController {

    private final ProductoService productos;
    private final AlmacenImagenes imagenes;

    @GetMapping("/menu")
    public ResponseEntity<MenuPublico> menu() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(30)).cachePublic())
                .body(productos.menuPublico());
    }

    /** El nombre del archivo lo valida AlmacenImagenes (UUID + extensión permitida). */
    @GetMapping("/imagenes/{nombre}")
    public ResponseEntity<Resource> imagen(@PathVariable String nombre) {
        Resource recurso = imagenes.cargar(nombre);
        return ResponseEntity.ok()
                .contentType(AlmacenImagenes.tipoDe(nombre))
                // El nombre cambia en cada subida, por eso la copia en caché puede ser permanente.
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .body(recurso);
    }
}
