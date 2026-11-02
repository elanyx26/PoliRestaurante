package com.poli.restaurante.service;

import com.poli.restaurante.domain.ConfiguracionDomicilios;
import com.poli.restaurante.repository.ConfiguracionDomiciliosRepository;
import com.poli.restaurante.repository.UsuarioRepository;
import com.poli.restaurante.security.UsuarioActual;
import com.poli.restaurante.web.dto.ConfiguracionDtos.ConfiguracionDomiciliosResponse;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ConfiguracionDomiciliosService {

    private final ConfiguracionDomiciliosRepository configuraciones;
    private final UsuarioRepository usuarios;

    @Transactional(readOnly = true)
    public ConfiguracionDomiciliosResponse obtener() {
        return ConfiguracionDomiciliosResponse.de(activa());
    }

    @Transactional(readOnly = true)
    public BigDecimal limiteContraentrega() {
        return activa().getLimiteContraentrega();
    }

    /** Desactiva la configuración vigente y crea una nueva, de modo que quede el historial de cambios. */
    public ConfiguracionDomiciliosResponse actualizar(BigDecimal limite, UsuarioActual actor) {
        ConfiguracionDomicilios actual = activa();
        actual.setActiva(false);
        configuraciones.saveAndFlush(actual);

        ConfiguracionDomicilios nueva = new ConfiguracionDomicilios();
        nueva.setLimiteContraentrega(limite);
        nueva.setCreatedBy(usuarios.getReferenceById(actor.id()));
        return ConfiguracionDomiciliosResponse.de(configuraciones.saveAndFlush(nueva));
    }

    private ConfiguracionDomicilios activa() {
        return configuraciones.findByActivaTrue()
                .orElseThrow(() -> new IllegalStateException("No hay configuración de domicilios activa"));
    }
}
