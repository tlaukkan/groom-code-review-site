ALTER TABLE review DROP COLUMN path;
ALTER TABLE review ADD COLUMN repository_repositoryid character varying(255);

ALTER TABLE review
  ADD CONSTRAINT fk_review_repository_repositoryid FOREIGN KEY (repository_repositoryid)
      REFERENCES repository (repositoryid) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION;

INSERT INTO schemaversion VALUES (NOW(), 'groom', '0003');
