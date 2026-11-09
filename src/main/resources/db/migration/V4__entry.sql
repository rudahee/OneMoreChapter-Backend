-- Listas del espacio: entradas, progreso compartido, valoraciones y notas.

CREATE TABLE entry (
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    space_id         BIGINT      NOT NULL,
    work_id          BIGINT      NOT NULL,
    status           VARCHAR(16) NOT NULL DEFAULT 'BACKLOG',
    current_unit_id  BIGINT,
    position_seconds INT,
    priority         INT         NOT NULL DEFAULT 0,
    added_by         BIGINT      NOT NULL,
    started_at       DATETIME(6),
    finished_at      DATETIME(6),
    version          BIGINT      NOT NULL DEFAULT 0,
    created_at       DATETIME(6) NOT NULL,
    updated_at       DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    -- Una obra aparece una sola vez por espacio.
    CONSTRAINT uk_entry_space_work UNIQUE (space_id, work_id),
    CONSTRAINT fk_entry_space FOREIGN KEY (space_id)
        REFERENCES space (id) ON DELETE CASCADE,
    -- RESTRICT: una obra del catalogo no se borra si hay listas apuntando a ella.
    CONSTRAINT fk_entry_work FOREIGN KEY (work_id)
        REFERENCES media_work (id) ON DELETE RESTRICT,
    CONSTRAINT fk_entry_current_unit FOREIGN KEY (current_unit_id)
        REFERENCES media_unit (id) ON DELETE SET NULL,
    CONSTRAINT fk_entry_added_by FOREIGN KEY (added_by)
        REFERENCES app_user (id) ON DELETE RESTRICT,
    INDEX idx_entry_space_status (space_id, status),
    INDEX idx_entry_work (work_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Progreso compartido: no hay user_id en la clave unica. marked_by solo
-- registra quien pulso el boton.
CREATE TABLE entry_unit_seen (
    id        BIGINT      NOT NULL AUTO_INCREMENT,
    entry_id  BIGINT      NOT NULL,
    unit_id   BIGINT      NOT NULL,
    seen_at   DATETIME(6) NOT NULL,
    marked_by BIGINT      NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_entry_unit_seen UNIQUE (entry_id, unit_id),
    CONSTRAINT fk_entry_unit_seen_entry FOREIGN KEY (entry_id)
        REFERENCES entry (id) ON DELETE CASCADE,
    CONSTRAINT fk_entry_unit_seen_unit FOREIGN KEY (unit_id)
        REFERENCES media_unit (id) ON DELETE CASCADE,
    CONSTRAINT fk_entry_unit_seen_user FOREIGN KEY (marked_by)
        REFERENCES app_user (id) ON DELETE RESTRICT,
    INDEX idx_entry_unit_seen_entry (entry_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Esta si es por persona: comparar notas es media gracia de llevar la lista
-- entre dos.
CREATE TABLE rating (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    entry_id   BIGINT      NOT NULL,
    user_id    BIGINT      NOT NULL,
    score      INT         NOT NULL,
    review     VARCHAR(2000),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_rating_entry_user UNIQUE (entry_id, user_id),
    CONSTRAINT ck_rating_score CHECK (score BETWEEN 1 AND 10),
    CONSTRAINT fk_rating_entry FOREIGN KEY (entry_id)
        REFERENCES entry (id) ON DELETE CASCADE,
    CONSTRAINT fk_rating_user FOREIGN KEY (user_id)
        REFERENCES app_user (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE note (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    entry_id          BIGINT        NOT NULL,
    -- NULL si la nota es sobre la obra en general.
    unit_id           BIGINT,
    timestamp_seconds INT,
    author_id         BIGINT        NOT NULL,
    body              VARCHAR(4000) NOT NULL,
    created_at        DATETIME(6)   NOT NULL,
    updated_at        DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_note_entry FOREIGN KEY (entry_id)
        REFERENCES entry (id) ON DELETE CASCADE,
    CONSTRAINT fk_note_unit FOREIGN KEY (unit_id)
        REFERENCES media_unit (id) ON DELETE SET NULL,
    CONSTRAINT fk_note_author FOREIGN KEY (author_id)
        REFERENCES app_user (id) ON DELETE CASCADE,
    INDEX idx_note_entry (entry_id),
    INDEX idx_note_unit (unit_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE tag (
    id       BIGINT      NOT NULL AUTO_INCREMENT,
    space_id BIGINT      NOT NULL,
    name     VARCHAR(60) NOT NULL,
    slug     VARCHAR(60) NOT NULL,
    PRIMARY KEY (id),
    -- Por espacio, no globales: son vocabulario privado de cada pareja.
    CONSTRAINT uk_tag_space_slug UNIQUE (space_id, slug),
    CONSTRAINT fk_tag_space FOREIGN KEY (space_id)
        REFERENCES space (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE entry_tag (
    entry_id BIGINT NOT NULL,
    tag_id   BIGINT NOT NULL,
    PRIMARY KEY (entry_id, tag_id),
    CONSTRAINT fk_entry_tag_entry FOREIGN KEY (entry_id)
        REFERENCES entry (id) ON DELETE CASCADE,
    CONSTRAINT fk_entry_tag_tag FOREIGN KEY (tag_id)
        REFERENCES tag (id) ON DELETE CASCADE,
    INDEX idx_entry_tag_tag (tag_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
