package com.poli.restaurante.security;

import com.poli.restaurante.domain.Usuario;
import com.poli.restaurante.repository.UsuarioRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Convierte un JWT válido en la autenticación de la aplicación. El usuario y su rol se leen de la base
 * en cada petición: desactivar una cuenta o cambiar su rol surte efecto de inmediato, aunque el token siga vigente.
 */
@Component
@RequiredArgsConstructor
public class JwtUsuarioConverter implements Converter<Jwt, UsernamePasswordAuthenticationToken> {

    private final UsuarioRepository usuarios;

    @Override
    public UsernamePasswordAuthenticationToken convert(Jwt jwt) {
        Usuario u = cargar(jwt.getSubject());
        UsuarioActual principal = new UsuarioActual(u.getId(), u.getEmail(), u.getNombre(), u.getRol());
        return UsernamePasswordAuthenticationToken.authenticated(principal, jwt,
                List.of(new SimpleGrantedAuthority("ROLE_" + u.getRol().name())));
    }

    private Usuario cargar(String subject) {
        try {
            return usuarios.findById(Long.valueOf(subject))
                    .filter(Usuario::isActivo)
                    .orElseThrow(() -> new BadCredentialsException("Usuario inexistente o desactivado"));
        } catch (NumberFormatException e) {
            throw new BadCredentialsException("Token inválido");
        }
    }
}
