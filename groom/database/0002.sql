ALTER TABLE company ADD COLUMN url character varying(255);

INSERT INTO schemaversion VALUES (NOW(), 'groom', '0002');

UPDATE COMPANY SET url = 'http://127.0.0.1:8083/groom';