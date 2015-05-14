package org.groom.review.model;

import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: tlaukkan
 * Date: 23.9.2013
 * Time: 18:49
 * To change this template use File | Settings | File Templates.
 */
public class Commit {
    private Date committerDate;
    private String committer;
    private Date authorDate;
    private String author;
    private String hash;
    private String subject;
    private String tags;

    public Commit(Date committerDate, String committer, Date authorDate, String author, String hash, String tags, String subject) {
        this.committerDate = committerDate;
        this.committer = committer;
        this.authorDate = authorDate;
        this.author = author;
        this.hash = hash;
        this.tags = tags;
        this.subject = subject;
    }

    public Date getCommitterDate() {
        return committerDate;
    }

    public void setCommitterDate(Date committerDate) {
        this.committerDate = committerDate;
    }

    public String getCommitter() {
        return committer;
    }

    public void setCommitter(String committer) {
        this.committer = committer;
    }

    public Date getAuthorDate() {
        return authorDate;
    }

    public void setAuthorDate(Date authorDate) {
        this.authorDate = authorDate;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }
}
