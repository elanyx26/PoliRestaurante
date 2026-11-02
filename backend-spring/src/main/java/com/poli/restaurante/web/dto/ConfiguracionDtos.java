package com.poli.restaurante.web.dto;

import com.poli.restaurante.domain.ConfiguracionDomicilios;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

public final class ConfiguracionDtos {

    private ConfiguracionDtos() {
    }

    public record ConfiguracionDomiciliosRequest(
            @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal limiteContraentrega) {
    }

    public record ConfiguracionDomiciliosResponse(BigDecimal limiteContraentrega, Instant actualizadoEn,
                                                  String actualizadoPor) {

        public static ConfiguracionDomiciliosResponse de(ConfiguracionDomicilios c) {
            return new ConfiguracionDomiciliosResponse(c.getLimiteContraentrega(), c.getCreatedAt(),
                    c.getCreatedBy() == null ? null : c.getCreatedBy().getNombre());
        }
    }
}
