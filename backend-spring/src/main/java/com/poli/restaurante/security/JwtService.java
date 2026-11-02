package com.poli.restaurante.security;

import com.poli.restaurante.config.AppProperties;
import com.poli.restaurante.domain.Usuario;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtService {

    private static final String EMISOR = "poli-restaurante";

    private final JwtEncoder encoder;
    private final AppProperties props;
    private final Clock clock;

    public record Token(String valor, Duration vigencia) {
    }

    public Token emitir(Usuario usuario) {
        Instant ahora = clock.instant();
        Duration vigencia = Duration.ofMinutes(props.jwt().expirationMinutes());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(EMISOR)
                .subject(String.valueOf(usuario.getId()))
                .issuedAt(ahora)
                .expiresAt(ahora.plus(vigencia))
                .claim("rol", usuario.getRol().name())
                .build();
        JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
        String valor = encoder.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();
        return new Token(valor, vigencia);
    }
}
