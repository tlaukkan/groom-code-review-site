package org.groom.model;

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

    public FileDiff(char status, String path) {
        this.status = status;
        this.path = path;
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
}
