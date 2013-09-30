CREATE TABLE comment (
    commentid character varying(255) NOT NULL,
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
ALTER TABLE public.comment OWNER TO groom;

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
ALTER TABLE public.review OWNER TO groom;

CREATE TABLE review_status (
    reviewstatusid character varying(255) NOT NULL,
    comment character varying(2048) NOT NULL,
    completed boolean NOT NULL,
    coverage text NOT NULL,
    created timestamp without time zone NOT NULL,
    modified timestamp without time zone NOT NULL,
    progress integer NOT NULL,
    review_reviewid character varying(255) NOT NULL,
    reviewer_userid character varying(255) NOT NULL
);
ALTER TABLE public.review_status OWNER TO groom;

ALTER TABLE ONLY comment
    ADD CONSTRAINT comment_pkey PRIMARY KEY (commentid);

ALTER TABLE ONLY review
    ADD CONSTRAINT review_pkey PRIMARY KEY (reviewid);

ALTER TABLE ONLY review_status
    ADD CONSTRAINT review_status_pkey PRIMARY KEY (reviewstatusid);

ALTER TABLE ONLY comment
    ADD CONSTRAINT fk_comment_review_reviewid FOREIGN KEY (review_reviewid) REFERENCES review(reviewid);

ALTER TABLE ONLY comment
    ADD CONSTRAINT fk_comment_reviewer_userid FOREIGN KEY (reviewer_userid) REFERENCES user_(userid);

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

INSERT INTO schemaversion VALUES (NOW(), 'groom', '0002');
