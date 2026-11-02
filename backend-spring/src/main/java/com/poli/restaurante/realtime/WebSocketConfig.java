package com.poli.restaurante.realtime;

import com.poli.restaurante.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/** STOMP sobre WebSocket en /ws con broker en memoria. Para varias instancias se reemplaza por un relay (RabbitMQ). */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final AppProperties props;
    private final StompAuthInterceptor autenticacion;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins(props.cors().allowedOrigins().toArray(String[]::new));
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        ThreadPoolTaskScheduler latidos = new ThreadPoolTaskScheduler();
        latidos.setPoolSize(1);
        latidos.setThreadNamePrefix("ws-heartbeat-");
        latidos.initialize();

        registry.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[] {10_000, 10_000})
                .setTaskScheduler(latidos);
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(autenticacion);
    }
}
