package org.groom;

import org.groom.model.BlameLine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: tlaukkan
 * Date: 24.9.2013
 * Time: 18:01
 * To change this template use File | Settings | File Templates.
 */
public class BlameReader {
    public static final List<BlameLine> read(final String path, final String sinceHash, final String untilHash, boolean reverse) {
        final Map<String, String> hashAuthorMap = new HashMap<String, String>();
        final String result = Shell.execute("git blame " + (reverse ? " --reverse" : "")
                + " --p " + sinceHash + ".." + untilHash + " -- " + path );

        final List<BlameLine> blameLines = new ArrayList<BlameLine>();
        final String[] lines = result.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String[] parts = lines[i].split(" ");
            final String hash = parts[0].substring(0, 8);
            final int originalLineNumber = Integer.parseInt(parts[1]);
            if (parts.length == 4 && !hashAuthorMap.containsKey(hash)) {
                final String authorName = lines[i + 1].split(" ")[1];
                hashAuthorMap.put(hash, authorName);
            }
            while (lines[i].charAt(0) != '\t') {
                i++;
            }
            blameLines.add(new BlameLine(hash, originalLineNumber, hashAuthorMap.get(hash), lines[i]));
        }

        return blameLines;
    }
}
