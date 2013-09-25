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

import com.sun.org.apache.xerces.internal.impl.dv.util.HexBin;
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
    private String comment;

    /** Completed. */
    @Column(nullable = false)
    private boolean completed;

    /** Coverage of the review. */
    @Lob
    @Column(nullable=false)
    private String coverage;

    private byte[] coverageCache = null;

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

    public ReviewStatus(Review review, User reviewer, String comment, boolean completed, byte[] coverage, Date created, Date modified) {
        this.review = review;
        this.reviewer = reviewer;
        this.comment = comment;
        this.completed = completed;
        this.coverage = HexBin.encode(coverage);
        this.created = created;
        this.modified = modified;
    }

    public String getReviewStatusId() {
        return reviewStatusId;
    }

    public void setReviewStatusId(String reviewStatusId) {
        this.reviewStatusId = reviewStatusId;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
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
