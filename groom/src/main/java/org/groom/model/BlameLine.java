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
    private String committerName;
    private String line;
    private LineChangeType type;

    public BlameLine(String hash, int originalLine, int finalLine, String authorName, final String committerName,
                     String line, LineChangeType type) {
        this.hash = hash;
        this.originalLine = originalLine;
        this.finalLine = finalLine;
        this.authorName = authorName;
        this.committerName = committerName;
        this.line = line;
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

    public String getLine() {
        return line;
    }

    public LineChangeType getType() {
        return type;
    }
}
