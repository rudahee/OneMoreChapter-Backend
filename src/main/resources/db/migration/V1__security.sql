-- Cuentas y sesiones. No hay alta abierta: los usuarios se crean al canjear
-- una invitacion, y de eso se encarga el paquete space.

CREATE TABLE app_user (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    email         VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name  VARCHAR(60)  NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    last_login_at DATETIME(6),
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_app_user_email UNIQUE (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE refresh_token (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    user_id    BIGINT      NOT NULL,
    -- SHA-256 en hexadecimal: 64 caracteres. Nunca el token en claro.
    token_hash CHAR(64)    NOT NULL,
    -- Agrupa los tokens nacidos del mismo inicio de sesion, para poder
    -- invalidar la sesion entera si se detecta reutilizacion.
    family_id  CHAR(36)    NOT NULL,
    issued_at  DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    revoked_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_refresh_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id)
        REFERENCES app_user (id) ON DELETE CASCADE,
    INDEX idx_refresh_token_user (user_id),
    INDEX idx_refresh_token_family (family_id),
    INDEX idx_refresh_token_expires (expires_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
