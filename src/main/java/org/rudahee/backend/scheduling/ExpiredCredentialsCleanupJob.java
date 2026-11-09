package org.rudahee.backend.scheduling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rudahee.backend.scheduling.config.SchedulingProperties;
import org.rudahee.backend.security.repository.RefreshTokenRepository;
import org.rudahee.backend.space.repository.InvitationRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Limpieza de credenciales caducadas.
 *
 * No se borran en cuanto expiran: se conservan unos dias porque un token
 * caducado es la unica pista que queda si hay que investigar una reutilizacion
 * despues de que salte la alarma. Igual con las invitaciones, que permiten
 * saber quien invito a quien.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "scheduling", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class ExpiredCredentialsCleanupJob {

    private final RefreshTokenRepository refreshTokenRepository;
    private final InvitationRepository invitationRepository;
    private final SchedulingProperties properties;

    @Scheduled(cron = "0 0 3 * * *", zone = "Europe/Madrid")
    @Transactional
    public void purgeExpired() {
        int tokens = refreshTokenRepository.deleteExpiredBefore(
                Instant.now().minus(properties.tokenRetention()));

        int invitations = invitationRepository.deleteExpiredBefore(
                Instant.now().minus(properties.invitationRetention()));

        if (tokens > 0 || invitations > 0) {
            log.info("Limpieza: {} tokens y {} invitaciones eliminados", tokens, invitations);
        }
    }
}
