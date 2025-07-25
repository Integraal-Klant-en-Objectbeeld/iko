ALTER TABLE profile
    RENAME TO aggregated_data_profile;

ALTER TABLE aggregated_data_profile
    RENAME COLUMN primary_search TO primary_endpoint;

ALTER TABLE relation
    RENAME COLUMN profile_id TO aggregated_data_profile_id;

ALTER TABLE relation
    RENAME COLUMN search_id TO endpoint_id;
