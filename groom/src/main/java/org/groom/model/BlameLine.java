package org.groom.model;

/**
 * Created with IntelliJ IDEA.
 * User: tlaukkan
 * Date: 24.9.2013
 * Time: 18:02
 * To change this template use File | Settings | File Templates.
 */
public class BlameLine {
    public String hash;
    public int originalLine;
    public String authorName;
    public String line;

    public BlameLine(String hash, int originalLine, String authorName, String line) {
        this.hash = hash;
        this.originalLine = originalLine;
        this.authorName = authorName;
        this.line = line;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public int getOriginalLine() {
        return originalLine;
    }

    public void setOriginalLine(int originalLine) {
        this.originalLine = originalLine;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getLine() {
        return line;
    }

    public void setLine(String line) {
        this.line = line;
    }
}
