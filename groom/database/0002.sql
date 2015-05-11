CREATE TABLE reviewcomment (
    reviewcommentid character varying(255) NOT NULL,
    author character varying(128) NOT NULL,
    committer character varying(128) NOT NULL,
    created timestamp without time zone NOT NULL,
    diffline integer NOT NULL,
    hash character varying(7) NOT NULL,
    line integer NOT NULL,
    message character varying(2048) NOT NULL,
    modified timestamp without time zone NOT NULL,
    path character varying(2048) NOT NULL,
    severity integer NOT NULL,
    review_reviewid character varying(255) NOT NULL,
    reviewer_userid character varying(255) NOT NULL
);

CREATE TABLE review (
    reviewid character varying(255) NOT NULL,
    completed boolean NOT NULL,
    created timestamp without time zone NOT NULL,
    diffcount integer NOT NULL,
    modified timestamp without time zone NOT NULL,
    path character varying(2048) NOT NULL,
    sincehash character varying(100) NOT NULL,
    title character varying(128) NOT NULL,
    untilhash character varying(100) NOT NULL,
    author_userid character varying(255) NOT NULL,
    owner_companyid character varying(255) NOT NULL,
    reviewgroup_groupid character varying(255) NOT NULL
);

CREATE TABLE review_status (
    reviewstatusid character varying(255) NOT NULL,
    reviewcomment character varying(2048) NOT NULL,
    completed boolean NOT NULL,
    coverage text NOT NULL,
    created timestamp without time zone NOT NULL,
    modified timestamp without time zone NOT NULL,
    progress integer NOT NULL,
    review_reviewid character varying(255) NOT NULL,
    reviewer_userid character varying(255) NOT NULL
);

ALTER TABLE ONLY reviewcomment
    ADD CONSTRAINT reviewcomment_pkey PRIMARY KEY (reviewcommentid);

ALTER TABLE ONLY review
    ADD CONSTRAINT review_pkey PRIMARY KEY (reviewid);

ALTER TABLE ONLY review_status
    ADD CONSTRAINT review_status_pkey PRIMARY KEY (reviewstatusid);

ALTER TABLE ONLY reviewcomment
    ADD CONSTRAINT fk_reviewcomment_review_reviewid FOREIGN KEY (review_reviewid) REFERENCES review(reviewid);

ALTER TABLE ONLY reviewcomment
    ADD CONSTRAINT fk_reviewcomment_reviewer_userid FOREIGN KEY (reviewer_userid) REFERENCES user_(userid);

ALTER TABLE ONLY review
    ADD CONSTRAINT fk_review_author_userid FOREIGN KEY (author_userid) REFERENCES user_(userid);

ALTER TABLE ONLY review
    ADD CONSTRAINT fk_review_owner_companyid FOREIGN KEY (owner_companyid) REFERENCES company(companyid);

ALTER TABLE ONLY review
    ADD CONSTRAINT fk_review_reviewgroup_groupid FOREIGN KEY (reviewgroup_groupid) REFERENCES group_(groupid);

ALTER TABLE ONLY review_status
    ADD CONSTRAINT fk_review_status_review_reviewid FOREIGN KEY (review_reviewid) REFERENCES review(reviewid);

ALTER TABLE ONLY review_status
    ADD CONSTRAINT fk_review_status_reviewer_userid FOREIGN KEY (reviewer_userid) REFERENCES user_(userid);

