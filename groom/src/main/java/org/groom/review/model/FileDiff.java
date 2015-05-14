package org.groom.review.model;

/**
 * Created with IntelliJ IDEA.
 * User: tlaukkan
 * Date: 24.9.2013
 * Time: 16:03
 * To change this template use File | Settings | File Templates.
 */
public class FileDiff {
    private char status;
    private String path;
    private int index;
    private ReviewStatus reviewStatus;

    public FileDiff(char status, String path, int index, ReviewStatus reviewStatus) {
        this.status = status;
        this.path = path;
        this.index = index;
        this.reviewStatus = reviewStatus;
    }

    public char getStatus() {
        return status;
    }

    public void setStatus(char status) {
        this.status = status;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public ReviewStatus getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(ReviewStatus reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public boolean isReviewed() {
        if (reviewStatus != null) {
            int byteIndex = index / 8;
            int bitIndex = index % 8;
            return ((reviewStatus.getCoverage()[byteIndex] >> bitIndex) & 1) > 0;
        } else {
            return false;
        }
    }

    public void setReviewed(final boolean reviewed) {
        if (reviewStatus != null) {
            int byteIndex = index / 8;
            int bitIndex = index % 8;
            byte[] coverage = reviewStatus.getCoverage();
            coverage[byteIndex] = (byte) (coverage[byteIndex] | (1 << bitIndex));
            reviewStatus.setCoverage(coverage);
            updateProgress();
        }
    }

    private void updateProgress() {
        int reviewed = 0;
        for (int i = 0; i < reviewStatus.getReview().getDiffCount(); i++) {
            int byteIndex = i / 8;
            int bitIndex = i % 8;
            if (((reviewStatus.getCoverage()[byteIndex] >> bitIndex) & 1) > 0) {
                reviewed ++;
            }
        }
        reviewStatus.setProgress(100 * reviewed / reviewStatus.getReview().getDiffCount());
    }
}
