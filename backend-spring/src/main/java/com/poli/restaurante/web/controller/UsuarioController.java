package com.poli.restaurante.web.controller;

import com.poli.restaurante.service.UsuarioService;
import com.poli.restaurante.web.dto.UsuarioDtos.CrearUsuarioRequest;
import com.poli.restaurante.web.dto.UsuarioDtos.UsuarioResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRADOR')")
@Tag(name = "Administración de usuarios")
public class UsuarioController {

    private final UsuarioService usuarios;

    @PostMapping
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody CrearUsuarioRequest req) {
        UsuarioResponse creado = usuarios.crear(req);
        return ResponseEntity.created(URI.create("/api/admin/usuarios/" + creado.id())).body(creado);
    }
}
