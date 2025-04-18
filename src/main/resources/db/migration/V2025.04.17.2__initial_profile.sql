
INSERT INTO profile (id,name,primary_source,"transform") VALUES
    ('633d0b2a-d9c0-4e7f-a9bb-4928cbd2a97e','TEMP','personenSearch','{ persoon: .left, zaken: .right }');

INSERT INTO relation (id,profile_id,source_id,source_to_search_mapping,search_id,"transform") VALUES
    ('633d0b2a-d9c0-4e7f-a9bb-4928cbd2a97a','633d0b2a-d9c0-4e7f-a9bb-4928cbd2a97e',NULL,'{ "bsn": ".burgerservicenummer"}','zakenSearch_bsn','.');
