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
import org.vaadin.addons.sitekit.model.Group;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * Review.
 *
 * @author Tommi S.E. Laukkanen
 */
@Entity
@Table(name = "review")
public final class Review implements Serializable {
    /** Java serialization version UID. */
    private static final long serialVersionUID = 1L;

    /** Unique UUID of the entity. */
    @Id
    @GeneratedValue(generator = "uuid")
    private String reviewId;

    /** Owning company. */
    @JoinColumn(nullable = false)
    @ManyToOne(cascade = { CascadeType.DETACH, CascadeType.MERGE, CascadeType.REFRESH }, optional = false)
    private Company owner;

    /** Reviewer group. */
    @JoinColumn(nullable = false)
    @ManyToOne(cascade = { CascadeType.DETACH, CascadeType.MERGE, CascadeType.REFRESH }, optional = false)
    private Group reviewGroup;

    /** Content. */
    @Column(length = 2048, nullable = false)
    private String path;

    /** Since hash. */
    @Column(length = 7, nullable = false)
    private String sinceHash;

    /** Until hash. */
    @Column(length = 7, nullable = false)
    private String untilHash;

    /** Until hash. */
    @Column(length = 7, nullable = false)
    private String title;

    /** Until hash. */
    @Column(nullable = false)
    private int diffCount;

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
    public Review() {
        super();
    }

    public Review(Company owner, Group reviewGroup, String path, String sinceHash, String untilHash,
                  String title, int diffCount, Date created, Date modified) {
        this.owner = owner;
        this.reviewGroup = reviewGroup;
        this.path = path;
        this.sinceHash = sinceHash;
        this.untilHash = untilHash;
        this.title = title;
        this.diffCount = diffCount;
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

    /**
     * @return the owner
     */
    public Company getOwner() {
        return owner;
    }

    /**
     * @param owner the owner to set
     */
    public void setOwner(final Company owner) {
        this.owner = owner;
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public String getReviewId() {
        return reviewId;
    }

    public void setReviewId(String reviewId) {
        this.reviewId = reviewId;
    }

    public Group getReviewGroup() {
        return reviewGroup;
    }

    public void setReviewGroup(Group reviewGroup) {
        this.reviewGroup = reviewGroup;
    }

    public String getSinceHash() {
        return sinceHash;
    }

    public void setSinceHash(String sinceHash) {
        this.sinceHash = sinceHash;
    }

    public String getUntilHash() {
        return untilHash;
    }

    public void setUntilHash(String untilHash) {
        this.untilHash = untilHash;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public int getDiffCount() {
        return diffCount;
    }

    public void setDiffCount(int diffCount) {
        this.diffCount = diffCount;
    }

    @Override
    public String toString() {
        return "review-" + reviewId.toString();
    }

    @Override
    public int hashCode() {
        return reviewId.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        return obj != null && obj instanceof Review && reviewId.equals(((Review) obj).getReviewId());
    }

}
