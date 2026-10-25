CREATE TABLE administrator_account
(
    id        BIGSERIAL PRIMARY KEY,
    username  VARCHAR(255) NOT NULL UNIQUE,
    password  VARCHAR(255) NOT NULL,
    authority VARCHAR(255) NOT NULL,
    version   BIGINT       NOT NULL DEFAULT 0
);
