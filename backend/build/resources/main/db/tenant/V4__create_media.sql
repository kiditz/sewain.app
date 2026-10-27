CREATE TABLE media
(
    id           UUID PRIMARY KEY      DEFAULT gen_random_uuid(),

    filename     VARCHAR(255) NOT NULL,
    object_key   VARCHAR(500) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size         BIGINT       NOT NULL,
    url          VARCHAR(500) NOT NULL,

    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_media_created_at
    ON media (created_at);

CREATE INDEX idx_media_filename
    ON media (filename);