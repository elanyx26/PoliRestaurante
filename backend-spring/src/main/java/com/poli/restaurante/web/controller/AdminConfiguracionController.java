package com.poli.restaurante.web.controller;

import com.poli.restaurante.security.UsuarioActual;
import com.poli.restaurante.service.ConfiguracionDomiciliosService;
import com.poli.restaurante.web.dto.ConfiguracionDtos.ConfiguracionDomiciliosRequest;
import com.poli.restaurante.web.dto.ConfiguracionDtos.ConfiguracionDomiciliosResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/configuracion/domicilios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRADOR')")
@Tag(name = "Configuración de domicilios")
public class AdminConfiguracionController {

    private final ConfiguracionDomiciliosService configuracion;

    @GetMapping
    public ConfiguracionDomiciliosResponse obtener() {
        return configuracion.obtener();
    }

    @PutMapping
    public ConfiguracionDomiciliosResponse actualizar(@Valid @RequestBody ConfiguracionDomiciliosRequest req,
                                                      @AuthenticationPrincipal UsuarioActual actor) {
        return configuracion.actualizar(req.limiteContraentrega(), actor);
    }
}
