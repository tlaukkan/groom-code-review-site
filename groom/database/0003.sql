ALTER TABLE review DROP COLUMN path;
ALTER TABLE review ADD COLUMN repository_repositoryid character varying(255);

CREATE TABLE repository
(
  repositoryid character varying(255) NOT NULL,
  created timestamp without time zone NOT NULL,
  modified timestamp without time zone NOT NULL,
  path character varying(2048) NOT NULL,
  url character varying(1024) NOT NULL,
  owner_companyid character varying(255) NOT NULL,
  CONSTRAINT repository_pkey PRIMARY KEY (repositoryid),
  CONSTRAINT fk_repository_owner_companyid FOREIGN KEY (owner_companyid)
      REFERENCES company (companyid) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);

ALTER TABLE review
  ADD CONSTRAINT fk_review_repository_repositoryid FOREIGN KEY (repository_repositoryid)
      REFERENCES repository (repositoryid) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION;

INSERT INTO schemaversion VALUES (NOW(), 'groom', '0003');
