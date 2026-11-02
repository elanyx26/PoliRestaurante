package com.poli.restaurante.web.dto;

import java.math.BigDecimal;
import java.util.List;

/** Respuestas del menú público: sin ids internos sensibles ni datos de administración. */
public final class PublicoDtos {

    private PublicoDtos() {
    }

    public record ProductoPublico(Long id, String nombre, String descripcion, BigDecimal precio, String imagenUrl) {
    }

    public record CategoriaPublica(Long id, String nombre, String descripcion, List<ProductoPublico> productos) {
    }

    public record MenuPublico(List<CategoriaPublica> categorias) {
    }
}
