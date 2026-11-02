package com.poli.restaurante.service;

import com.poli.restaurante.domain.Usuario;
import com.poli.restaurante.repository.UsuarioRepository;
import com.poli.restaurante.security.JwtService;
import com.poli.restaurante.security.LoginAttemptService;
import com.poli.restaurante.web.dto.UsuarioDtos.LoginRequest;
import com.poli.restaurante.web.dto.UsuarioDtos.LoginResponse;
import com.poli.restaurante.web.dto.UsuarioDtos.UsuarioResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final LoginAttemptService intentos;
    /** Hash descartable: se compara cuando el usuario no existe para que el tiempo de respuesta no lo delate. */
    private final String hashFicticio;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder encoder, JwtService jwt,
                       LoginAttemptService intentos) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.jwt = jwt;
        this.intentos = intentos;
        this.hashFicticio = encoder.encode("contrasena-ficticia");
    }

    public LoginResponse login(LoginRequest req, String ip) {
        String clave = LoginAttemptService.clave(req.email(), ip);
        intentos.verificarNoBloqueado(clave);

        Usuario usuario = usuarios.findByEmailIgnoreCase(req.email().trim()).orElse(null);
        String hash = usuario != null ? usuario.getPasswordHash() : hashFicticio;
        boolean coincide = encoder.matches(req.password(), hash);

        if (usuario == null || !coincide || !usuario.isActivo()) {
            intentos.registrarFallo(clave);
            throw new BadCredentialsException("Credenciales inválidas");
        }
        intentos.registrarExito(clave);
        JwtService.Token token = jwt.emitir(usuario);
        return new LoginResponse(token.valor(), "Bearer", token.vigencia().toSeconds(), UsuarioResponse.de(usuario));
    }
}
