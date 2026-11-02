package com.poli.restaurante.security;

import com.poli.restaurante.config.AppProperties;
import com.poli.restaurante.exception.TooManyAttemptsException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Limita los intentos fallidos de login por correo + IP para frenar la fuerza bruta.
 * El estado vive en memoria: con varias instancias del backend conviene moverlo a Redis.
 */
@Component
@RequiredArgsConstructor
public class LoginAttemptService {

    private record Estado(int fallos, Instant bloqueadoHasta) {
    }

    private static final int MAX_ENTRADAS = 10_000;

    private final AppProperties props;
    private final Clock clock;
    private final Map<String, Estado> intentos = new ConcurrentHashMap<>();

    public static String clave(String email, String ip) {
        return email.trim().toLowerCase(Locale.ROOT) + "|" + ip;
    }

    public void verificarNoBloqueado(String clave) {
        Estado e = intentos.get(clave);
        if (e != null && e.bloqueadoHasta() != null) {
            if (e.bloqueadoHasta().isAfter(clock.instant())) {
                throw new TooManyAttemptsException("Demasiados intentos fallidos. Intente de nuevo más tarde");
            }
            intentos.remove(clave);
        }
    }

    public void registrarFallo(String clave) {
        Instant ahora = clock.instant();
        if (intentos.size() > MAX_ENTRADAS) {
            // Protege la memoria ante un ataque con correos aleatorios.
            intentos.clear();
        }
        intentos.merge(clave, new Estado(1, null), (actual, nuevo) -> {
            int fallos = actual.fallos() + 1;
            Instant hasta = fallos >= props.login().maxAttempts()
                    ? ahora.plus(Duration.ofMinutes(props.login().lockMinutes())) : null;
            return new Estado(fallos, hasta);
        });
    }

    public void registrarExito(String clave) {
        intentos.remove(clave);
    }
}
