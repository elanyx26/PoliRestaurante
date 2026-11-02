package com.poli.restaurante.web.dto;

import com.poli.restaurante.domain.Categoria;
import com.poli.restaurante.domain.EstadoMesa;
import com.poli.restaurante.domain.Mesa;
import com.poli.restaurante.domain.Producto;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public final class CatalogoDtos {

    private CatalogoDtos() {
    }

    public record CategoriaRequest(
            @NotBlank @Size(max = 100) String nombre,
            @Size(max = 255) String descripcion,
            Boolean activa) {
    }

    public record CategoriaResponse(Long id, String nombre, String descripcion, boolean activa) {

        public static CategoriaResponse de(Categoria c) {
            return new CategoriaResponse(c.getId(), c.getNombre(), c.getDescripcion(), c.isActiva());
        }
    }

    public record ProductoRequest(
            @NotNull Long categoriaId,
            @NotBlank @Size(max = 120) String nombre,
            @Size(max = 255) String descripcion,
            @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal precio,
            Boolean disponible) {
    }

    public record ProductoResponse(Long id, Long categoriaId, String categoriaNombre, String nombre,
                                   String descripcion, BigDecimal precio, boolean disponible, String imagenUrl) {

        public static ProductoResponse de(Producto p) {
            return new ProductoResponse(p.getId(), p.getCategoria().getId(), p.getCategoria().getNombre(),
                    p.getNombre(), p.getDescripcion(), p.getPrecio(), p.isDisponible(), urlImagen(p));
        }

        /** Ruta relativa pública de la foto; el cliente la antepone a la dirección del backend. */
        public static String urlImagen(Producto p) {
            return p.getImagen() == null ? null : "/api/public/imagenes/" + p.getImagen();
        }
    }

    public record MesaRequest(
            @NotNull @Min(1) Integer numero,
            @Min(1) Integer capacidad) {
    }

    public record MesaResponse(Long id, Integer numero, Integer capacidad, EstadoMesa estado) {

        public static MesaResponse de(Mesa m) {
            return new MesaResponse(m.getId(), m.getNumero(), m.getCapacidad(), m.getEstado());
        }
    }
}
