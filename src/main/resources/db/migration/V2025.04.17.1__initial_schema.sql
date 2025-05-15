CREATE TABLE profile
(
    id             UUID NOT NULL,
    name           VARCHAR(255),
    primary_search VARCHAR(255),
    transform      TEXT,
    CONSTRAINT pk_profile PRIMARY KEY (id)
);

CREATE TABLE relation
(
    id                       UUID NOT NULL,
    profile_id               UUID NOT NULL,
    source_id                UUID NULL,
    source_to_search_mapping TEXT,
    search_id                VARCHAR(255),
    transform                TEXT,
    CONSTRAINT pk_relation PRIMARY KEY (id)
);

ALTER TABLE relation
    ADD CONSTRAINT fk_relation_on_profile FOREIGN KEY (profile_id) REFERENCES profile (id);

ALTER TABLE profile
    ADD CONSTRAINT uq_profile_name UNIQUE (name);