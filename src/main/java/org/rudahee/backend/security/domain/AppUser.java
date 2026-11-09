package org.rudahee.backend.security.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.rudahee.backend.common.domain.Auditable;

import java.time.Instant;
import java.util.Locale;

/**
 * Cuenta de usuario.
 *
 * No hay alta abierta: solo se crea al canjear una invitacion, y de eso se
 * encarga {@code space}. Este paquete no sabe que existen las invitaciones,
 * de forma que la dependencia va en un solo sentido: space -> security.
 */
@Getter
@Entity
@Table(
        name = "app_user",
        uniqueConstraints = @UniqueConstraint(name = "uk_app_user_email", columnNames = "email"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppUser extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Siempre en minusculas: evita que el mismo correo entre dos veces. */
    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @Setter
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Setter
    @Column(name = "display_name", nullable = false, length = 60)
    private String displayName;

    @Setter
    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Setter
    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    public AppUser(String email, String passwordHash, String displayName) {
        this.email = normalizeEmail(email);
        this.passwordHash = passwordHash;
        this.displayName = displayName;
    }

    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
