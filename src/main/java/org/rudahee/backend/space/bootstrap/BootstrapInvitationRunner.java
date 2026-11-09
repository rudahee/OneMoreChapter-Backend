package org.rudahee.backend.space.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rudahee.backend.security.repository.AppUserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Resuelve el problema del huevo y la gallina.
 *
 * El alta es solo por invitacion, las invitaciones las crea un miembro de un
 * espacio, y los espacios los crea un usuario. Sin esto, una instalacion nueva
 * no tiene forma de crear la primera cuenta.
 *
 * Al arrancar, si no hay ningun usuario, se registra en el log un aviso con las
 * instrucciones. No se crea nada automaticamente ni se generan credenciales por
 * defecto: una cuenta de administrador con contrasenya conocida en una API
 * publica es justo lo que no queremos.
 *
 * El primer usuario se crea con {@code bootstrap.sql} o a mano una sola vez;
 * a partir de ahi, todo pasa por invitaciones.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BootstrapInvitationRunner implements ApplicationRunner {

    private final AppUserRepository userRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }
        log.warn("""

                ==========================================================
                 No hay ninguna cuenta en la base de datos.

                 El registro esta cerrado por diseno: solo se entra con
                 invitacion. Para crear la primera cuenta, ejecuta el
                 script scripts/bootstrap-first-user.sql contra la base de
                 datos y despues inicia sesion con esas credenciales.

                 Cambia la contrasenya nada mas entrar.
                ==========================================================
                """);
    }
}
