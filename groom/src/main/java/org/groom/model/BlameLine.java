package org.groom.model;

/**
 * Created with IntelliJ IDEA.
 * User: tlaukkan
 * Date: 24.9.2013
 * Time: 18:02
 * To change this template use File | Settings | File Templates.
 */
public class BlameLine {
    private String hash;
    private int originalLine;
    private int finalLine;
    private String authorName;
    private String authorEmail;
    private String committerName;
    private String committerEmail;
    private String line;
    private String summary;
    private LineChangeType type;

    public BlameLine(String hash, int originalLine, int finalLine, String authorName, String authorEmail,
                     final String committerName, String committerEmail,
                     String line, String summary, LineChangeType type) {
        this.hash = hash;
        this.originalLine = originalLine;
        this.finalLine = finalLine;
        this.authorName = authorName;
        this.authorEmail = authorEmail;
        this.committerName = committerName;
        this.committerEmail = committerEmail;
        this.line = line;
        this.summary = summary;
        this.type = type;
    }

    public String getHash() {
        return hash;
    }

    public int getOriginalLine() {
        return originalLine;
    }

    public int getFinalLine() {
        return finalLine;
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getCommitterName() {
        return committerName;
    }

    public String getAuthorEmail() {
        return authorEmail;
    }

    public String getCommitterEmail() {
        return committerEmail;
    }

    public String getLine() {
        return line;
    }

    public String getSummary() {
        return summary;
    }

    public LineChangeType getType() {
        return type;
    }
}
