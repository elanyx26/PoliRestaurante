package com.poli.restaurante.web.dto;

import com.poli.restaurante.domain.Rol;
import com.poli.restaurante.domain.Usuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class UsuarioDtos {

    private UsuarioDtos() {
    }

    public record LoginRequest(
            @NotBlank @Email @Size(max = 150) String email,
            @NotBlank @Size(max = 72) String password) {
    }

    public record LoginResponse(String accessToken, String tokenType, long expiresInSeconds, UsuarioResponse usuario) {
    }

    public record CrearUsuarioRequest(
            @NotBlank @Size(max = 120) String nombre,
            @NotBlank @Email @Size(max = 150) String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotNull Rol rol) {
    }

    public record ActualizarUsuarioRequest(
            @NotBlank @Size(max = 120) String nombre,
            @NotBlank @Email @Size(max = 150) String email,
            @NotNull Rol rol) {
    }

    public record CambiarEstadoUsuarioRequest(@NotNull Boolean activo) {
    }

    public record CambiarContrasenaRequest(@NotBlank @Size(min = 8, max = 72) String nuevaContrasena) {
    }

    public record UsuarioResponse(Long id, String nombre, String email, Rol rol, boolean activo, Instant createdAt) {

        public static UsuarioResponse de(Usuario u) {
            return new UsuarioResponse(u.getId(), u.getNombre(), u.getEmail(), u.getRol(), u.isActivo(),
                    u.getCreatedAt());
        }
    }
}
