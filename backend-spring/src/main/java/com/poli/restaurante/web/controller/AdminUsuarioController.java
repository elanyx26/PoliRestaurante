package com.poli.restaurante.web.controller;

import com.poli.restaurante.domain.Rol;
import com.poli.restaurante.service.UsuarioService;
import com.poli.restaurante.web.dto.PageResponse;
import com.poli.restaurante.web.dto.UsuarioDtos.ActualizarUsuarioRequest;
import com.poli.restaurante.web.dto.UsuarioDtos.CambiarContrasenaRequest;
import com.poli.restaurante.web.dto.UsuarioDtos.CambiarEstadoUsuarioRequest;
import com.poli.restaurante.web.dto.UsuarioDtos.UsuarioResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/usuarios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRADOR')")
@Tag(name = "Administración de usuarios")
public class AdminUsuarioController {

    private final UsuarioService usuarios;

    @GetMapping
    public PageResponse<UsuarioResponse> listar(
            @RequestParam(required = false) Rol rol,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "nombre", direction = Sort.Direction.ASC) Pageable pageable) {
        return usuarios.listar(rol, activo, q, pageable);
    }

    @PutMapping("/{id}")
    public UsuarioResponse actualizar(@PathVariable Long id, @Valid @RequestBody ActualizarUsuarioRequest req) {
        return usuarios.actualizar(id, req);
    }

    @PatchMapping("/{id}/estado")
    public UsuarioResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambiarEstadoUsuarioRequest req) {
        return usuarios.cambiarEstado(id, req.activo());
    }

    @PatchMapping("/{id}/contrasena")
    public ResponseEntity<Void> cambiarContrasena(@PathVariable Long id,
                                                  @Valid @RequestBody CambiarContrasenaRequest req) {
        usuarios.cambiarContrasena(id, req.nuevaContrasena());
        return ResponseEntity.noContent().build();
    }
}
