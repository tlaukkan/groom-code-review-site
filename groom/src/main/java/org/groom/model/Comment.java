/**
 * Copyright 2013 Tommi S.E. Laukkanen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.groom.model;

import org.vaadin.addons.sitekit.model.Company;
import org.vaadin.addons.sitekit.model.User;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * ReviewStatus.
 *
 * @author Tommi S.E. Laukkanen
 */
@Entity
@Table(name = "comment")
public final class Comment implements Serializable {
    /** Java serialization version UID. */
    private static final long serialVersionUID = 1L;

    /** Unique UUID of the entity. */
    @Id
    @GeneratedValue(generator = "uuid")
    private String commentId;

    /** Review this status belongs to. */
    @JoinColumn(nullable = false)
    @ManyToOne(cascade = { CascadeType.DETACH, CascadeType.MERGE, CascadeType.REFRESH }, optional = false)
    private Review review;

    /** Reviewer. */
    @JoinColumn(nullable = false)
    @ManyToOne(cascade = { CascadeType.DETACH, CascadeType.MERGE, CascadeType.REFRESH }, optional = false)
    private User reviewer;

    /** Hash. */
    @Column(length = 7, nullable = false)
    private String hash;

    /** Path. */
    @Column(length = 2048, nullable = false)
    private String path;

    /** Line. */
    @Column(nullable = false)
    private int line;

    /** Diff Line. */
    @Column(nullable = false)
    private int diffLine;

    /** Severity. */
    @Column(nullable = false)
    private int severity;

    /** Message. */
    @Column(length = 2048, nullable = false)
    private String message;

    /** Author. */
    @Column(length = 128, nullable = false)
    private String author;

    /** Committer. */
    @Column(length = 128, nullable = false)
    private String committer;

    /** Created time of the event. */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date created;

    /** Created time of the event. */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date modified;

    /**
     * The default constructor for JPA.
     */
    public Comment() {
        super();
    }

    public Comment(Review review, User reviewer, String hash, String path, int line, int diffLine,
                   int severity, String message,
                   String author, String committer, Date created, Date modified) {
        this.review = review;
        this.reviewer = reviewer;
        this.hash = hash;
        this.path = path;
        this.line = line;
        this.diffLine = diffLine;
        this.severity = severity;
        this.message = message;
        this.author = author;
        this.committer = committer;
        this.created = created;
        this.modified = modified;
    }

    /**
     * @return the path
     */
    public String getPath() {
        return path;
    }

    /**
     * @param path the path
     */
    public void setPath(final String path) {
        this.path = path;
    }

    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    public Review getReview() {
        return review;
    }

    public void setReview(Review review) {
        this.review = review;
    }

    public User getReviewer() {
        return reviewer;
    }

    public void setReviewer(User reviewer) {
        this.reviewer = reviewer;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public int getDiffLine() {
        return diffLine;
    }

    public void setDiffLine(int diffLine) {
        this.diffLine = diffLine;
    }

    public int getSeverity() {
        return severity;
    }

    public void setSeverity(int severity) {
        this.severity = severity;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getCommitter() {
        return committer;
    }

    public void setCommitter(String committer) {
        this.committer = committer;
    }

    /**
     * @return the created
     */
    public Date getCreated() {
        return created;
    }

    /**
     * @param created the created to set
     */
    public void setCreated(final Date created) {
        this.created = created;
    }

    /**
     * @return the modified
     */
    public Date getModified() {
        return modified;
    }

    /**
     * @param modified the modified to set
     */
    public void setModified(final Date modified) {
        this.modified = modified;
    }

    @Override
    public String toString() {
        return "comment-" + commentId.toString();
    }

    @Override
    public int hashCode() {
        return commentId.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        return obj != null && obj instanceof Comment
                && commentId.equals(((Comment) obj).getCommentId());
    }

}
