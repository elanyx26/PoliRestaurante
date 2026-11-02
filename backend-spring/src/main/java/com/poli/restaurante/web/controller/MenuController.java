package com.poli.restaurante.web.controller;

import com.poli.restaurante.service.CategoriaService;
import com.poli.restaurante.service.MesaService;
import com.poli.restaurante.service.ProductoService;
import com.poli.restaurante.web.dto.CatalogoDtos.CategoriaResponse;
import com.poli.restaurante.web.dto.CatalogoDtos.MesaResponse;
import com.poli.restaurante.web.dto.CatalogoDtos.ProductoResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Lectura del menú (cualquier usuario autenticado) y de mesas libres (meseros). */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Menú y mesas")
public class MenuController {

    private final CategoriaService categorias;
    private final ProductoService productos;
    private final MesaService mesas;

    @GetMapping("/menu/categorias")
    public List<CategoriaResponse> categorias() {
        return categorias.menu();
    }

    @GetMapping("/menu/productos")
    public List<ProductoResponse> productos(@RequestParam(required = false) Long categoriaId) {
        return productos.menu(categoriaId);
    }

    @GetMapping("/mesas/disponibles")
    @PreAuthorize("hasRole('MESERO')")
    public List<MesaResponse> mesasDisponibles() {
        return mesas.disponibles();
    }
}
