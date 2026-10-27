CREATE TABLE collections
(
    id          UUID PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    slug        VARCHAR(255) NOT NULL UNIQUE,
    image_url   VARCHAR(255),
    description TEXT,
    parent_id   UUID,
    position INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT fk_collections_parent
        FOREIGN KEY (parent_id)
            REFERENCES collections (id)
);

CREATE INDEX idx_collections_slug
    ON collections (slug);

CREATE INDEX idx_collections_parent_id
    ON collections (parent_id);
