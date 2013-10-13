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
        final Map<Integer, String> originalLines = new HashMap<Integer, String>();
        for (int i = 0; i < blames.size(); i++) {
            final BlameLine forwardBlame = blames.get(i);
            if (forwardBlame.getType() == LineChangeType.NONE) {
                originalLines.put(forwardBlame.getOriginalLine(), forwardBlame.getLine());
            }
        }

        final List<BlameLine> reverseBlames;
        if (status == 'A') {
            reverseBlames = new ArrayList<BlameLine>();
        } else {
            reverseBlames = BlameReader.read(repositoryPath, path, sinceHash, untilHash, true);
        }

        // Inserting deletes among forward blames
        for (final BlameLine reverseBlame : reverseBlames) {
            if (reverseBlame.getType() == LineChangeType.DELETED) {
                if (originalLines.containsKey(reverseBlame.getOriginalLine())) {
                    if (originalLines.get(reverseBlame.getOriginalLine()).equals(reverseBlame.getLine())) {
                        LOGGER.warn("Reverse blame incorrectly reported deleted line: " +
                                reverseBlame.getOriginalLine() + ": " + reverseBlame.getLine());
                    } else {
                        LOGGER.error("Forward and reverse blame reported different original line: " +
                                reverseBlame.getOriginalLine()
                                + ": F=" + originalLines.get(reverseBlame.getOriginalLine())
                                + " R=" + reverseBlame.getLine());
                    }
                    continue; // Forward blames already listed this original line as being not changed.
                }
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
            final Map<String, String> reversePreviousHashMap = new HashMap<String, String>();

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
                String previousHash = null;
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
                    if (lines[i].startsWith("previous")) {
                        String[] previousParts = lines[i].split(" ");
                        previousHash = previousParts[1];
                    }
                    i++;
                }
                final LineChangeType type;
                if (reverse && !hash.startsWith(untilHash)) {
                    type = LineChangeType.DELETED;
                } else if (!reverse && !boundaryHashes.contains(hash)) {
                    type = LineChangeType.ADDED;
                } else {
                    type = LineChangeType.NONE;
                }
                final String actualHash;
                if (reverse && previousHash != null) {
                    actualHash = previousHash;
                    reversePreviousHashMap.put(hash, previousHash);
                } else {
                    if (reversePreviousHashMap.containsKey(hash)) {
                        actualHash = reversePreviousHashMap.get(hash);
                    } else {
                        actualHash = hash;
                    }
                }
                if (!hashAuthorNameMap.containsKey(actualHash)) {
                    final String commitDetails[] =
                            Shell.execute("git show -s --format=\"%an%n%aE%n%cn%n%cE%n%s\" " + actualHash,
                            repositoryPath).split("\n");
                    if (commitDetails.length == 5) {
                        hashAuthorNameMap.put(actualHash, commitDetails[0]);
                        hashAuthorEmailMap.put(actualHash, commitDetails[1]);
                        hashCommitterNameMap.put(actualHash, commitDetails[2]);
                        hashCommitterEmailMap.put(actualHash, commitDetails[3]);
                        hashSummaryMap.put(actualHash, commitDetails[4]);
                    } else {
                        hashAuthorNameMap.put(actualHash, "?");
                        hashAuthorEmailMap.put(actualHash, "?");
                        hashCommitterNameMap.put(actualHash, "?");
                        hashCommitterEmailMap.put(actualHash, "?");
                        hashSummaryMap.put(actualHash, "?");
                    }
                }
                blameLines.add(new BlameLine(actualHash,
                        (!reverse ? originalLineNumber : finalLineNumber),
                        (!reverse ? finalLineNumber : originalLineNumber),
                        hashAuthorNameMap.get(actualHash), hashAuthorEmailMap.get(actualHash),
                        hashCommitterNameMap.get(actualHash), hashCommitterEmailMap.get(actualHash),
                        lines[i].substring(1),
                        hashSummaryMap.get(actualHash), type));
            }

            return blameLines;
        } catch (final Throwable t) {
            LOGGER.error("Error reading blame for " + path, t);
            return new ArrayList<BlameLine>();
        }
    }
}
