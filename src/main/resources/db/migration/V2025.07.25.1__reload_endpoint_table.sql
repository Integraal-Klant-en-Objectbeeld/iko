TRUNCATE TABLE endpoint;

INSERT INTO endpoint (id, name, route_id, created_on, modified_on, is_primary)
VALUES
    ('a1b2c3d4-e5f6-7890-ab12-cdef34567890', 'BRP Personen', 'brpPersonenEndpoint', CURRENT_TIMESTAMP, NULL, TRUE),
    ('b2c3d4e5-f678-9012-ab34-def456789012', 'OpenZaak Zaken', 'openZaakEndpointZaken', CURRENT_TIMESTAMP, NULL, TRUE),
    ('c3d4e5f6-7890-1234-ab56-ef5678901234', 'BAG adressen', 'bagEndpointAdressen', CURRENT_TIMESTAMP, NULL, TRUE),
    ('d4e5f678-9012-3456-ab78-f67890123456', 'BAG Adresseerbare Objecten', 'bagEndpointAdresseerbareObjecten', CURRENT_TIMESTAMP, NULL, FALSE),
    ('e5f67890-1234-5678-ab90-678901234567', 'BAG Bronhouders', 'bagEndpointBronhouders', CURRENT_TIMESTAMP, NULL, FALSE),
    ('f6789012-3456-7890-ab12-789012345678', 'BAG Ligplaatsen', 'bagEndpointLigplaatsen', CURRENT_TIMESTAMP, NULL, FALSE),
    ('07890123-4567-8901-ab34-890123456789', 'BAG Nummeraanduidingen', 'bagEndpointNummeraanduidingen', CURRENT_TIMESTAMP, NULL, FALSE),
    ('18901234-5678-9012-ab56-901234567890', 'BAG Openbare Ruimten', 'bagEndpointOpenbareRuimten', CURRENT_TIMESTAMP, NULL, FALSE),
    ('29012345-6789-0123-ab78-012345678901', 'BAG Panden', 'bagEndpointPanden', CURRENT_TIMESTAMP, NULL, FALSE),
    ('39012345-6789-0123-ab90-123456789012', 'BAG Standplaatsen', 'bagEndpointStandplaatsen', CURRENT_TIMESTAMP, NULL, FALSE),
    ('49012345-6789-0123-ab12-234567890123', 'BAG Verblijfsobjecten', 'bagEndpointVerblijfsobjecten', CURRENT_TIMESTAMP, NULL, FALSE),
    ('59012345-6789-0123-ab34-345678901234', 'BAG Woonplaatsen', 'bagEndpointWoonplaatsen', CURRENT_TIMESTAMP, NULL, FALSE),
    ('69012345-6789-0123-ab56-456789012345', 'ObjectsAPI Objecten', 'objectenApiObjecten', CURRENT_TIMESTAMP, NULL, FALSE);