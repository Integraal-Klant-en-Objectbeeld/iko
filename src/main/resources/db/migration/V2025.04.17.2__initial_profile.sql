INSERT INTO profile (id, name, primary_search, "transform")
VALUES ('633d0b2a-d9c0-4e7f-a9bb-4928cbd2a97e', 'TEMP', 'a1b2c3d4-e5f6-7890-ab12-cdef34567890',
        '{ persoon: .left, zaken: .right }');

INSERT INTO relation (id, profile_id, source_id, source_to_search_mapping, search_id, "transform")
VALUES ('633d0b2a-d9c0-4e7f-a9bb-4928cbd2a97a', '633d0b2a-d9c0-4e7f-a9bb-4928cbd2a97e', NULL,
        '{ "postcode": ".personen[0].verblijfplaats.verblijfadres", "huisnummer": ".personen[0].verblijfplaats.huisnummer"}',
        'c3d4e5f6-7890-1234-ab56-ef5678901234', '.');

INSERT INTO profile (id, name, primary_search, "transform")
VALUES ('00000000-0000-0000-0000-000000000001',
        'BagBewoners',
        'c3d4e5f6-7890-1234-ab56-ef5678901234',
        '{ bag: .left, brp: .right }');

INSERT INTO relation (id, profile_id, source_id, source_to_search_mapping, search_id, "transform")
VALUES ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', NULL,
        '{ "type": "\"ZoekMetPostcodeEnHuisnummer\"", "postcode": ".postcode", "huisnummer": ".huisnummer" }',
        'a1b2c3d4-e5f6-7890-ab12-cdef34567890', '.');
