package org.groom;

import org.groom.model.BlameLine;
import org.groom.model.LineChangeType;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: tlaukkan
 * Date: 24.9.2013
 * Time: 18:01
 * To change this template use File | Settings | File Templates.
 */
public class BlameReader {
    public static final List<BlameLine> read(final String path, final String sinceHash,
                                             final String untilHash, boolean reverse) {
        final Map<String, String> hashAuthorNameMap = new HashMap<String, String>();
        final Map<String, String> hashAuthorEmailMap = new HashMap<String, String>();
        final Map<String, String> hashCommitterNameMap = new HashMap<String, String>();
        final Map<String, String> hashCommitterEmailMap = new HashMap<String, String>();
        final String result = Shell.execute("git blame " + (reverse ? " --reverse" : "")
                + " --p " + sinceHash + ".." + untilHash + " -- " + path );
        final Set<String> boundaryHashes = new HashSet<String>();

        final List<BlameLine> blameLines = new ArrayList<BlameLine>();
        final String[] lines = result.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String[] parts = lines[i].split(" ");
            final String hash = parts[0].substring(0, 7);
            final int originalLineNumber = Integer.parseInt(parts[1]);
            final int finalLineNumber = Integer.parseInt(parts[2]);
            if (parts.length == 4 && !hashAuthorNameMap.containsKey(hash)) {
                final String authorName = lines[i + 1].split(" ")[1];
                final String authorEmail = lines[i + 2].split(" ")[1].replace("<","").replace(">","");
                final String committerName = lines[i + 5].split(" ")[1];
                final String committerEmail = lines[i + 6].split(" ")[1].replace("<","").replace(">","");
                hashAuthorNameMap.put(hash, authorName);
                hashAuthorEmailMap.put(hash, authorEmail);
                hashCommitterNameMap.put(hash, committerName);
                hashCommitterEmailMap.put(hash, committerEmail);
            }
            while (lines[i].charAt(0) != '\t') {
                if (lines[i].equals("boundary")) {
                    boundaryHashes.add(hash);
                }
                i++;
            }
            final LineChangeType type;
            if (reverse && !hash.equals(untilHash)) {
                type = LineChangeType.DELETED;
            } else if (!reverse && !boundaryHashes.contains(hash)) {
                type = LineChangeType.ADDED;
            } else {
                type = LineChangeType.NONE;
            }
            blameLines.add(new BlameLine(hash, originalLineNumber, finalLineNumber,
                    hashAuthorNameMap.get(hash), hashAuthorEmailMap.get(hash),
                    hashCommitterNameMap.get(hash), hashCommitterEmailMap.get(hash),lines[i].substring(1), type));
        }

        return blameLines;
    }
}
