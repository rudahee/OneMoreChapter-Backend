-- Catalogo canonico de obras, compartido entre espacios.

CREATE TABLE media_work (
                            id               BIGINT       NOT NULL AUTO_INCREMENT,
                            type             VARCHAR(20)  NOT NULL,
                            title            VARCHAR(500) NOT NULL,
                            normalized_title VARCHAR(500) NOT NULL,
                            original_title   VARCHAR(500),
                            release_year     INT,
                            cover_url        VARCHAR(1000),
                            episodic         BOOLEAN      NOT NULL DEFAULT FALSE,
                            total_units      INT,
                            status           VARCHAR(20)  NOT NULL DEFAULT 'UNKNOWN',
                            units_synced_at  DATE,
                            version          BIGINT       NOT NULL DEFAULT 0,
                            created_at       DATETIME(6)  NOT NULL,
                            updated_at       DATETIME(6)  NOT NULL,
                            PRIMARY KEY (id),
    -- Indice de prefijo: con utf8mb4 cada caracter ocupa hasta 4 bytes y el
    -- limite de InnoDB en formato DYNAMIC son 3072. VARCHAR(500) completo se pasa.
                            INDEX idx_media_work_normalized_title (normalized_title(191)),
                            INDEX idx_media_work_type_status (type, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Busqueda por relevancia en el catalogo local. El LIKE del repositorio sirve
-- para prefijos; para consultas de varias palabras, MATCH ... AGAINST.
ALTER TABLE media_work
    ADD FULLTEXT INDEX ft_media_work_title (title, original_title);

CREATE TABLE media_work_external_id (
                                        work_id     BIGINT       NOT NULL,
                                        source      VARCHAR(32)  NOT NULL,
                                        external_id VARCHAR(128) NOT NULL,
    -- Un identificador de proveedor apunta como maximo a una obra. Es la
    -- restriccion que frena la insercion duplicada cuando los dos anyadis la
    -- misma serie a la vez.
                                        CONSTRAINT uk_external_id_source_value UNIQUE (source, external_id),
                                        CONSTRAINT fk_external_id_work FOREIGN KEY (work_id)
                                            REFERENCES media_work (id) ON DELETE CASCADE,
                                        INDEX idx_external_id_work (work_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE media_unit (
                            id              BIGINT      NOT NULL AUTO_INCREMENT,
                            work_id         BIGINT      NOT NULL,
    -- 0 cuando la obra no tiene temporadas. NO usar NULL: en MariaDB los NULL
    -- no colisionan dentro de un indice UNIQUE, asi que permitiria insertar
    -- dos veces el mismo capitulo.
                            season_number   INT         NOT NULL DEFAULT 0,
                            unit_number     INT         NOT NULL,
                            title           VARCHAR(500),
                            air_date        DATE,
                            runtime_minutes INT,
                            PRIMARY KEY (id),
                            CONSTRAINT uk_media_unit_work_season_number
                                UNIQUE (work_id, season_number, unit_number),
                            CONSTRAINT fk_media_unit_work FOREIGN KEY (work_id)
                                REFERENCES media_work (id) ON DELETE CASCADE,
                            INDEX idx_media_unit_air_date (air_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
