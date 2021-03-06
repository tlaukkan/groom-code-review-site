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
package org.groom.review.model;

import org.apache.xerces.impl.dv.util.HexBin;
import org.bubblecloud.ilves.model.User;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * ReviewStatus.
 *
 * @author Tommi S.E. Laukkanen
 */
@Entity
@Table(name = "review_status")
public final class ReviewStatus implements Serializable {
    /** Java serialization version UID. */
    private static final long serialVersionUID = 1L;

    /** Unique UUID of the entity. */
    @Id
    @GeneratedValue(generator = "uuid")
    private String reviewStatusId;

    /** Review this status belongs to. */
    @JoinColumn(nullable = false)
    @ManyToOne(cascade = { CascadeType.DETACH, CascadeType.MERGE, CascadeType.REFRESH }, optional = false)
    private Review review;

    /** Reviewer. */
    @JoinColumn(nullable = false)
    @ManyToOne(cascade = { CascadeType.DETACH, CascadeType.MERGE, CascadeType.REFRESH }, optional = false)
    private User reviewer;

    /** Content. */
    @Column(length = 2048, nullable = false)
    private String reviewComment;

    /** Completed. */
    @Column(nullable = false)
    private boolean completed;

    /** Coverage of the review. */
    @Lob
    @Column(nullable=false)
    private String coverage;

    @Transient
    private byte[] coverageCache = null;

    @Column(nullable=false)
    private int progress;

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
    public ReviewStatus() {
        super();
    }

    public ReviewStatus(Review review, User reviewer, String reviewComment, boolean completed, byte[] coverage, Date created, Date modified) {
        this.review = review;
        this.reviewer = reviewer;
        this.reviewComment = reviewComment;
        this.completed = completed;
        this.coverage = HexBin.encode(coverage);
        this.created = created;
        this.modified = modified;
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

    public String getReviewStatusId() {
        return reviewStatusId;
    }

    public void setReviewStatusId(String reviewStatusId) {
        this.reviewStatusId = reviewStatusId;
    }

    public String getReviewComment() {
        return reviewComment;
    }

    public void setReviewComment(String reviewComment) {
        this.reviewComment = reviewComment;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public byte[] getCoverage() {
        if (coverageCache == null) {
            coverageCache = HexBin.decode(coverage);
        }
        return coverageCache;
    }

    public void setCoverage(byte[] coverage) {
        coverageCache = coverage;
        this.coverage = HexBin.encode(coverage);
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
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
        return "review-status-" + reviewStatusId.toString();
    }

    @Override
    public int hashCode() {
        return reviewStatusId.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        return obj != null && obj instanceof ReviewStatus
                && reviewStatusId.equals(((ReviewStatus) obj).getReviewStatusId());
    }

}
