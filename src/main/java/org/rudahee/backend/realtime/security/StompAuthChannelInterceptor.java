package org.rudahee.backend.realtime.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rudahee.backend.space.repository.SpaceMemberRepository;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Autenticacion y autorizacion del canal STOMP.
 *
 * La cadena de filtros HTTP no protege esto: el apreton de manos es una sola
 * peticion y despues todo viaja por el socket, fuera del alcance de Spring
 * Security web. Sin este interceptor, cualquiera que abra un socket puede
 * suscribirse al topico de otro espacio y leer su actividad en directo.
 *
 * Dos momentos:
 * <ul>
 *   <li>CONNECT: valida el JWT que el cliente manda en la cabecera del frame
 *       y asocia el usuario a la sesion del socket.</li>
 *   <li>SUBSCRIBE: comprueba que ese usuario pertenece al espacio del topico.
 *       Autenticar sin esto solo garantiza que es alguien, no que sea alguien
 *       con derecho a ese espacio.</li>
 * </ul>
 */
@Slf4j
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final Pattern SPACE_TOPIC = Pattern.compile("^/topic/spaces/(\\d+)$");
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtDecoder jwtDecoder;
    private final SpaceMemberRepository memberRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            accessor.setUser(authenticate(accessor));
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscription(accessor);
        }
        return message;
    }

    private Authentication authenticate(StompHeaderAccessor accessor) {
        List<String> headers = accessor.getNativeHeader(AUTH_HEADER);
        if (headers == null || headers.isEmpty()) {
            throw new MessageDeliveryException("Falta el token en el frame CONNECT");
        }

        String raw = headers.get(0);
        if (!raw.startsWith(BEARER_PREFIX)) {
            throw new MessageDeliveryException("Formato de token no valido");
        }

        try {
            Jwt jwt = jwtDecoder.decode(raw.substring(BEARER_PREFIX.length()));
            // Se usa el mismo subject que en HTTP (el id numerico), para que
            // getName() devuelva lo mismo en ambos canales.
            return new UsernamePasswordAuthenticationToken(jwt.getSubject(), null, List.of());
        } catch (JwtException ex) {
            log.debug("Token no valido en CONNECT: {}", ex.getMessage());
            throw new MessageDeliveryException("Token no valido");
        }
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null) {
            throw new MessageDeliveryException("Suscripcion sin destino");
        }

        Matcher matcher = SPACE_TOPIC.matcher(destination);
        if (!matcher.matches()) {
            // Lista blanca: solo se permiten los topicos que conocemos.
            throw new MessageDeliveryException("Destino no permitido: " + destination);
        }

        Long userId = currentUserId(accessor);
        Long spaceId = Long.valueOf(matcher.group(1));

        if (memberRepository.findRole(spaceId, userId).isEmpty()) {
            log.warn("Usuario {} intento suscribirse al espacio {} sin pertenecer",
                    userId, spaceId);
            throw new MessageDeliveryException("No perteneces a este espacio");
        }
    }

    private Long currentUserId(StompHeaderAccessor accessor) {
        if (accessor.getUser() == null) {
            throw new MessageDeliveryException("Sesion no autenticada");
        }
        try {
            return Long.valueOf(accessor.getUser().getName());
        } catch (NumberFormatException ex) {
            throw new MessageDeliveryException("Identidad no valida en la sesion");
        }
    }
}
