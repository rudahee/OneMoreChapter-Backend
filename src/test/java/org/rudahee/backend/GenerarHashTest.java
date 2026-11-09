package org.rudahee.backend;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

class GenerarHashTest {

    @Test
    void generar() {
        System.out.println(Argon2PasswordEncoder
                .defaultsForSpringSecurity_v5_8()
                .encode("Jeguvo89"));
    }
}