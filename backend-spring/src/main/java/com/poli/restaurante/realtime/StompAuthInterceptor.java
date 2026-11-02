package com.poli.restaurante.realtime;

import com.poli.restaurante.security.JwtUsuarioConverter;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

/**
 * Seguridad del canal STOMP: autentica el CONNECT con el mismo JWT de la API (cabecera Authorization),
 * autoriza cada SUBSCRIBE según el rol y rechaza que los clientes envíen mensajes (el canal es solo de salida).
 */
@Component
@RequiredArgsConstructor
public class StompAuthInterceptor implements ChannelInterceptor {

    private static final String PREFIJO = "Bearer ";
    private static final Set<String> TODOS = Set.of("ROLE_ADMINISTRADOR", "ROLE_MESERO", "ROLE_COCINA");

    private static final Map<String, Set<String>> DESTINOS = Map.of(
            "/topic/cocina", Set.of("ROLE_COCINA", "ROLE_ADMINISTRADOR"),
            "/topic/mesas", Set.of("ROLE_MESERO", "ROLE_ADMINISTRADOR"),
            "/topic/admin/pedidos", Set.of("ROLE_ADMINISTRADOR"),
            "/user/queue/pedidos", TODOS);

    private final JwtDecoder decoder;
    private final JwtUsuarioConverter converter;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }
        StompCommand comando = accessor.getCommand();
        if (StompCommand.CONNECT.equals(comando)) {
            accessor.setUser(autenticar(accessor.getFirstNativeHeader("Authorization")));
        } else if (StompCommand.SUBSCRIBE.equals(comando)) {
            autorizar(accessor);
        } else if (StompCommand.SEND.equals(comando)) {
            throw new MessageDeliveryException("Los clientes no pueden enviar mensajes por este canal");
        }
        return message;
    }

    private Authentication autenticar(String cabecera) {
        if (cabecera == null || !cabecera.startsWith(PREFIJO)) {
            throw new BadCredentialsException("Falta el token Bearer");
        }
        try {
            return converter.convert(decoder.decode(cabecera.substring(PREFIJO.length()).trim()));
        } catch (JwtException e) {
            throw new BadCredentialsException("Token inválido o vencido", e);
        }
    }

    private void autorizar(StompHeaderAccessor accessor) {
        if (!(accessor.getUser() instanceof Authentication auth)) {
            throw new AccessDeniedException("No autenticado");
        }
        Set<String> permitidos = DESTINOS.get(accessor.getDestination());
        boolean ok = permitidos != null
                && auth.getAuthorities().stream().anyMatch(a -> permitidos.contains(a.getAuthority()));
        if (!ok) {
            throw new AccessDeniedException("No puede suscribirse a " + accessor.getDestination());
        }
    }
}
