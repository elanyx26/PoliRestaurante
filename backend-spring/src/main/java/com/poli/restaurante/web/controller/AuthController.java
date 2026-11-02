package com.poli.restaurante.web.controller;

import com.poli.restaurante.security.UsuarioActual;
import com.poli.restaurante.service.AuthService;
import com.poli.restaurante.service.UsuarioService;
import com.poli.restaurante.web.dto.UsuarioDtos.LoginRequest;
import com.poli.restaurante.web.dto.UsuarioDtos.LoginResponse;
import com.poli.restaurante.web.dto.UsuarioDtos.UsuarioResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación")
public class AuthController {

    private final AuthService auth;
    private final UsuarioService usuarios;

    @PostMapping("/login")
    @SecurityRequirements
    public LoginResponse login(@Valid @RequestBody LoginRequest req, HttpServletRequest http) {
        return auth.login(req, http.getRemoteAddr());
    }

    @GetMapping("/me")
    public UsuarioResponse me(@AuthenticationPrincipal UsuarioActual actor) {
        return usuarios.obtener(actor.id());
    }
}
