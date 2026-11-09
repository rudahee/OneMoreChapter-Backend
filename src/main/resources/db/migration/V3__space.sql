-- Espacios compartidos, pertenencia e invitaciones.

CREATE TABLE space (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    name        VARCHAR(80) NOT NULL,
    -- NULL = sin limite de miembros
    max_members INT,
    created_at  DATETIME(6) NOT NULL,
    updated_at  DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE space_member (
    id        BIGINT      NOT NULL AUTO_INCREMENT,
    space_id  BIGINT      NOT NULL,
    user_id   BIGINT      NOT NULL,
    role      VARCHAR(16) NOT NULL,
    joined_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_space_member_space_user UNIQUE (space_id, user_id),
    CONSTRAINT fk_space_member_space FOREIGN KEY (space_id)
        REFERENCES space (id) ON DELETE CASCADE,
    CONSTRAINT fk_space_member_user FOREIGN KEY (user_id)
        REFERENCES app_user (id) ON DELETE CASCADE,
    -- Sostiene la consulta de autorizacion, que corre en cada peticion.
    INDEX idx_space_member_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE invitation (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    space_id     BIGINT      NOT NULL,
    -- SHA-256 del codigo en mayusculas. El codigo en claro no se guarda.
    code_hash    CHAR(64)    NOT NULL,
    granted_role VARCHAR(16) NOT NULL,
    created_by   BIGINT      NOT NULL,
    created_at   DATETIME(6) NOT NULL,
    expires_at   DATETIME(6) NOT NULL,
    redeemed_at  DATETIME(6),
    redeemed_by  BIGINT,
    revoked_at   DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_invitation_code_hash UNIQUE (code_hash),
    CONSTRAINT fk_invitation_space FOREIGN KEY (space_id)
        REFERENCES space (id) ON DELETE CASCADE,
    -- RESTRICT y no CASCADE: borrar una cuenta no debe llevarse por delante el
    -- rastro de quien invito a quien.
    CONSTRAINT fk_invitation_creator FOREIGN KEY (created_by)
        REFERENCES app_user (id) ON DELETE RESTRICT,
    CONSTRAINT fk_invitation_redeemer FOREIGN KEY (redeemed_by)
        REFERENCES app_user (id) ON DELETE SET NULL,
    INDEX idx_invitation_space (space_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
