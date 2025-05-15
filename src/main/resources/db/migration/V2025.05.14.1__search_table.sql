CREATE TABLE search
(
    id          UUID PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    route_id    VARCHAR(500) NOT NULL,
    created_on  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_on TIMESTAMP,
    is_primary  BOOLEAN      NOT NULL DEFAULT FALSE
);