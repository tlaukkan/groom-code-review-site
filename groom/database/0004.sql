ALTER TABLE reviewcomment
   ALTER COLUMN hash TYPE character varying(100);


INSERT INTO schemaversion VALUES (NOW(), 'groom', '0004');
