
-- =====================================================
-- 1. Extend collections
-- =====================================================

ALTER TABLE collections
    ADD COLUMN collection_type VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    ADD COLUMN match_condition VARCHAR(10) NOT NULL DEFAULT 'ALL';

ALTER TABLE collections
    ADD CONSTRAINT chk_collections_type
        CHECK (collection_type IN ('MANUAL', 'AUTOMATED')),
    ADD CONSTRAINT chk_collections_match_condition
        CHECK (match_condition IN ('ALL', 'ANY'));


-- =====================================================
-- 2. Product types
-- =====================================================

CREATE TABLE product_types
(
    id         UUID PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    slug       VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE products
    ADD COLUMN product_type_id UUID;

ALTER TABLE products
    ADD CONSTRAINT fk_products_product_type
        FOREIGN KEY (product_type_id)
            REFERENCES product_types (id)
            ON DELETE SET NULL;

CREATE INDEX idx_products_product_type_id
    ON products (product_type_id);


-- =====================================================
-- 3. Tags
-- =====================================================

CREATE TABLE tags
(
    id         UUID PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    slug       VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- =====================================================
-- 4. Product tags (many-to-many)
-- =====================================================

CREATE TABLE product_tags
(
    product_id UUID NOT NULL,
    tag_id     UUID NOT NULL,

    CONSTRAINT pk_product_tags
        PRIMARY KEY (product_id, tag_id),

    CONSTRAINT fk_product_tags_product
        FOREIGN KEY (product_id)
            REFERENCES products (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_product_tags_tag
        FOREIGN KEY (tag_id)
            REFERENCES tags (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_product_tags_tag_id
    ON product_tags (tag_id);


-- =====================================================
-- 5. Collection items (manual mapping)
-- =====================================================

CREATE TABLE collection_items
(
    id            UUID PRIMARY KEY,
    collection_id UUID NOT NULL,
    product_id    UUID NOT NULL,
    position      INTEGER NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_collection_items_collection
        FOREIGN KEY (collection_id)
            REFERENCES collections (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_collection_items_product
        FOREIGN KEY (product_id)
            REFERENCES products (id)
            ON DELETE CASCADE,

    CONSTRAINT uq_collection_items_product
        UNIQUE (collection_id, product_id)
);

CREATE INDEX idx_collection_items_product
    ON collection_items (product_id);

CREATE INDEX idx_collection_items_order
    ON collection_items (collection_id, position);


-- =====================================================
-- 6. Collection rules (automated mapping)
-- =====================================================

CREATE TABLE collection_rules
(
    id            UUID PRIMARY KEY,
    collection_id UUID NOT NULL,
    attribute     VARCHAR(100) NOT NULL,
    operator      VARCHAR(20) NOT NULL,
    position      INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT fk_collection_rules_collection
        FOREIGN KEY (collection_id)
            REFERENCES collections (id)
            ON DELETE CASCADE,

    CONSTRAINT chk_collection_rules_operator
        CHECK (
            operator IN (
                         'EQUALS',
                         'NOT_EQUALS',
                         'CONTAINS',
                         'NOT_CONTAINS',
                         'STARTS_WITH',
                         'ENDS_WITH'
                )
            )
);

CREATE INDEX idx_collection_rules_collection
    ON collection_rules (collection_id, position);


-- =====================================================
-- 7. Collection rule values
-- =====================================================

CREATE TABLE collection_rule_values
(
    id      UUID PRIMARY KEY,
    rule_id UUID NOT NULL,
    value   VARCHAR(255) NOT NULL,

    CONSTRAINT fk_collection_rule_values_rule
        FOREIGN KEY (rule_id)
            REFERENCES collection_rules (id)
            ON DELETE CASCADE,

    CONSTRAINT uq_collection_rule_value
        UNIQUE (rule_id, value)
);

CREATE INDEX idx_collection_rule_values_rule
    ON collection_rule_values (rule_id);
