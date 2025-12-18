CREATE TABLE art_entity
(
    id                 BIGSERIAL PRIMARY KEY,
    title              VARCHAR(255) NOT NULL,
    sub_title          VARCHAR(255),
    uploaded_date      TIMESTAMP,
    display_date       DATE,
    width              INTEGER,
    height             INTEGER,
    media              VARCHAR(255),
    description        TEXT,
    caption            VARCHAR(255),
    general_viewable   BOOLEAN      NOT NULL DEFAULT FALSE,
    privilege_viewable BOOLEAN      NOT NULL DEFAULT FALSE,
    version            BIGINT       NOT NULL DEFAULT 0
);

CREATE TABLE art_entity_rendition
(
    art_entity_id BIGINT       NOT NULL,
    rendition_type VARCHAR(255) NOT NULL,
    object_key    VARCHAR(255) NOT NULL,
    content_type  VARCHAR(255),
    size_bytes    BIGINT,
    width         INTEGER,
    height        INTEGER,
    checksum      VARCHAR(255),
    PRIMARY KEY (art_entity_id, rendition_type),
    FOREIGN KEY (art_entity_id) REFERENCES art_entity (id) ON DELETE CASCADE
);

CREATE TABLE category
(
    id                   BIGSERIAL PRIMARY KEY,
    category_name        VARCHAR(255) NOT NULL UNIQUE,
    category_description TEXT,
    version              BIGINT       NOT NULL DEFAULT 0
);

CREATE TABLE art_entity_category
(
    art_entity_id BIGINT NOT NULL,
    category_id   BIGINT NOT NULL,
    PRIMARY KEY (art_entity_id, category_id),
    FOREIGN KEY (art_entity_id) REFERENCES art_entity (id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES category (id) ON DELETE CASCADE
);

CREATE TABLE comment
(
    id            BIGSERIAL PRIMARY KEY,
    comment       TEXT         NOT NULL,
    art_entity_id BIGINT       NOT NULL,
    commented_on  TIMESTAMP  NOT NULL DEFAULT NOW(),
    first_name    VARCHAR(255),
    last_name     VARCHAR(255),
    email_address VARCHAR(255),
    telephone     VARCHAR(255),
    version       BIGINT       NOT NULL DEFAULT 0,
    FOREIGN KEY (art_entity_id) REFERENCES art_entity (id) ON DELETE CASCADE
);
