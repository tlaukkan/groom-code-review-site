package org.groom;

import org.apache.log4j.Logger;
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
    /** The logger. */
    private static final Logger LOGGER = Logger.getLogger(BlameReader.class);

    public static final List<BlameLine> readBlameLines(final String repositoryPath, final String path, char status, String sinceHash, String untilHash) {
        if (status != 'A'&& status != 'M') {
            return new ArrayList<BlameLine>();
        }
        final List<BlameLine> blames = BlameReader.read(repositoryPath, path, sinceHash, untilHash, false);
        final List<BlameLine> reverseBlames;
        if (status == 'A') {
            reverseBlames = new ArrayList<BlameLine>();
        } else {
            reverseBlames = BlameReader.read(repositoryPath, path, sinceHash, untilHash, true);
        }

        // Inserting deletes among forward blames
        for (final BlameLine reverseBlame : reverseBlames) {
            if (reverseBlame.getType() == LineChangeType.DELETED) {
                boolean inserted = false;
                for (int i = 0; i < blames.size(); i++) {
                    final BlameLine forwardBlame = blames.get(i);
                    if ((forwardBlame.getType() == LineChangeType.NONE || forwardBlame.getType() == LineChangeType.DELETED)
                            && forwardBlame.getOriginalLine() >= reverseBlame.getOriginalLine()) {
                        blames.add(i, reverseBlame);
                        inserted = true;
                        break;
                    }
                }
                if (!inserted) {
                    blames.add(reverseBlame);
                }
            }
        }
        return blames;
    }

    private static final List<BlameLine> read(final String repositoryPath, final String path, final String sinceHash,
                                             final String untilHash, boolean reverse) {



        try {
            final Map<String, String> hashAuthorNameMap = new HashMap<String, String>();
            final Map<String, String> hashAuthorEmailMap = new HashMap<String, String>();
            final Map<String, String> hashCommitterNameMap = new HashMap<String, String>();
            final Map<String, String> hashCommitterEmailMap = new HashMap<String, String>();
            final Map<String, String> hashSummaryMap = new HashMap<String, String>();

            // If review is against git special empty tree then define --root and only until hash
            final String result;
            if ("4b825dc642cb6eb9a060e54bf8d69288fbee4904".startsWith(sinceHash)) {
                result = Shell.execute("git blame " + (reverse ? " --reverse" : "")
                        + " --root --p -w " + untilHash + " -- " + path, repositoryPath);
            } else {
                result = Shell.execute("git blame " + (reverse ? " --reverse" : "")
                        + " --p -w " + sinceHash + ".." + untilHash + " -- " + path, repositoryPath);
            }
            final Set<String> boundaryHashes = new HashSet<String>();

            final List<BlameLine> blameLines = new ArrayList<BlameLine>();
            final String[] lines = result.split("\n");
            for (int i = 0; i < lines.length; i++) {
                String[] parts = lines[i].split(" ");
                final String hash = parts[0];
                final int originalLineNumber = Integer.parseInt(parts[1]);
                final int finalLineNumber = Integer.parseInt(parts[2]);
                if (parts.length == 4 && !hashAuthorNameMap.containsKey(hash)) {
                    final String authorName = lines[i + 1].substring("author ".length());
                    final String authorEmail = lines[i + 2].substring("author-mail ".length()).replace("<","").replace(">","");
                    final String committerName = lines[i + 5].substring("committer ".length());
                    final String committerEmail = lines[i + 6].substring("committer-mail ".length()).replace("<","").replace(">","");
                    final String summary = lines[i + 9].substring("summary ".length());
                    hashAuthorNameMap.put(hash, authorName);
                    hashAuthorEmailMap.put(hash, authorEmail);
                    hashCommitterNameMap.put(hash, committerName);
                    hashCommitterEmailMap.put(hash, committerEmail);
                    hashSummaryMap.put(hash, summary);
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
                        hashCommitterNameMap.get(hash), hashCommitterEmailMap.get(hash),lines[i].substring(1),
                        hashSummaryMap.get(hash), type));
            }

            return blameLines;
        } catch (final Throwable t) {
            LOGGER.error("Error reading blame for " + path, t);
            return new ArrayList<BlameLine>();
        }
    }
}
