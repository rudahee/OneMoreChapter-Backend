package org.rudahee.backend.realtime.config;

import lombok.RequiredArgsConstructor;
import org.rudahee.backend.realtime.security.StompAuthChannelInterceptor;
import org.rudahee.backend.space.repository.SpaceMemberRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP sobre WebSocket.
 *
 * Broker simple en memoria: sirve para dos usuarios y una instancia. Si algun
 * dia se replica la aplicacion, un cliente conectado a la instancia A no
 * recibiria los eventos publicados en la B, y habria que poner un broker de
 * verdad o Redis pub/sub por debajo.
 */
@Configuration
@RequiredArgsConstructor
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtDecoder jwtDecoder;
    private final SpaceMemberRepository memberRepository;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic")
                // Latido en ambos sentidos: sin el, una conexion movil que
                // muere al cambiar de red se queda colgada hasta el timeout
                // del sistema operativo.
                .setHeartbeatValue(new long[]{10_000, 10_000});
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(
                        "https://uncapitulomas.tudominio.es",
                        "http://localhost:4200",
                        "http://localhost",
                        "capacitor://localhost");
        // Sin SockJS: WebSocket nativo funciona en todos los navegadores
        // actuales y en Capacitor. Anyadirlo solo sumaria un plan B que nunca
        // se usaria y que complica el proxy.
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(
                new StompAuthChannelInterceptor(jwtDecoder, memberRepository));
    }
}
