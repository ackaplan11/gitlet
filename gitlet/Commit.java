package gitlet;

import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.HashMap;
import java.util.Date;

import static gitlet.Utils.*;
import static gitlet.Repository.*;

/** Represents the snapshot of the current project state.
 * @author Andrew Kaplan */

class Commit implements Serializable {

    /** Initial Commit Constructor. */
    Commit() {
        _message = "initial commit";
        _timestamp = formatter.format(Date.from(Instant.EPOCH));
        _parentID = "";
        _mergeID = "";
        _data = new StagingArea().map();
        _shaCode = createShaCode();
    }

    /** Commit Constructor.
     * @param msg : User created _message describing commit
     * @param parentID : Parent of constructed commit */
    Commit(String msg, String parentID) {
        _message = msg;
        _timestamp = formatter.format(Date.from(Instant.now()));
        _parentID = parentID;
        _mergeID = "";
        _data = updateData(new HashMap<>(stagingArea().map()));
        _shaCode = createShaCode();
    }

    /** Return Message. */
    String message() {
        return _message;
    }

    /** Return Parent Commit ID. */
    String parentID() {
        return _parentID;
    }

    /** Return Merge Parent Commit ID. */
    String merge() {
        return _mergeID;
    }

    /** Assign merge parent from BRANCHNAME. */
    void assignMergeParent(String branchName) {
        File mergeBranchFile = join(REFS, branchName);
        Branch mergeBranch = readObject(mergeBranchFile, Branch.class);
        File mergeCommit = join(COMMITS, mergeBranch.pointer());
        _mergeID = readObject(mergeCommit, Commit.class).shaCode();
    }

    /** Return _data HashMap. */
    HashMap<String, String> data() {
        return _data;
    }

    /** Return _timestamp. */
    String timestamp() {
        return _timestamp;
    }

    /** Return ShaCode. */
    String shaCode() {
        return _shaCode;
    }

    /** Create and Return Commit ShaCode. */
    private String createShaCode() {
        byte[] com = serialize(this);
        return sha1((Object) com);
    }

    /** Return _data HashMap with changes staged in StagingArea MAP. */
    HashMap<String, String> updateData(HashMap<String, String> map) {
        File parentFile = join(COMMITS, parentID());
        if (parentFile.exists()) {
            Commit parent = readObject(parentFile, Commit.class);
            if (!parent.data().isEmpty()) {
                HashMap<String, String> newData = new HashMap<>();
                for (String fileName : parent.data().keySet()) {
                    File blobFile = join(OBJECTS, parent.data().get(fileName));
                    Blob b = readObject(blobFile, Blob.class);
                    if (b.rm()) {
                        b.changeRmStatus();
                        writeObject(blobFile, b);
                        break;
                    } else {
                        newData.put(fileName, parent.data().get(fileName));
                    }
                }
                for (String fileName : map.keySet()) {
                    newData.put(fileName, map.get(fileName));
                }
                return newData;
            }
            return map;
        }
        return map;
    }

    /** Return HashMap PATHWAY containing ancestors of GIVEN based
     *  and the length of the PATHLEN from GIVEN to ancestor. */
    static HashMap<String, Integer> givenAncestors(Commit given, int pathLen,
                                            HashMap<String, Integer> pathway) {
        pathway.put(given.shaCode(), pathLen);
        File ancestorFile = join(COMMITS, given.parentID());
        if (ancestorFile.exists()  && !ancestorFile.isDirectory()) {
            Commit ancestor = readObject(ancestorFile, Commit.class);
            if (pathway.containsKey(ancestor._shaCode)) {
                return null;
            } else {
                givenAncestors(ancestor, pathLen + 1, pathway);
                if (!given.merge().equals("")) {
                    File mergeFile = join(COMMITS, given.merge());
                    Commit mergeAncestor = readObject(mergeFile, Commit.class);
                    givenAncestors(mergeAncestor, pathLen + 1, pathway);
                }
            }
            return pathway;
        } else {
            return pathway;
        }
    }

    /** Return the SPLITID of the common ancestor of CURR and GIVEN in PATH with
     *  the minimum path length. */
    static String findSplitPoint(HashMap<String, Integer> path, Commit curr) {
        if (path.containsKey(curr.shaCode())) {
            if (path.get(curr.shaCode()) < _min) {
                _min = path.get(curr.shaCode());
                _splitID = curr.shaCode();
            }
        }
        File ancestorFile = join(COMMITS, curr.parentID());
        if (ancestorFile.exists() && !ancestorFile.isDirectory()) {
            Commit ancestor = readObject(ancestorFile, Commit.class);
            findSplitPoint(path, ancestor);
        }
        if (!curr.merge().equals("")) {
            File mergeFile = join(COMMITS, curr.merge());
            Commit mergeAncestor = readObject(mergeFile, Commit.class);
            findSplitPoint(path, mergeAncestor);
        }
        return _splitID;
    }

    /** Message instance. */
    private final String _message;
    /** Timestamp instance. */
    private final String _timestamp;
    /** String instance, points to the parent of current Commit. */
    private final String _parentID;
    /** String instance, points to the merge parent of current Commit
     *  null unless commit is a merge commit. */
    private String _mergeID;
    /** Int instance used in to find min path distance to split point. */
    private static int _min = Integer.MAX_VALUE;
    /** String instance of splitID. */
    private static String _splitID = "";
    /** HashMap instance of Blob fileNames --> Blob shaCodes. */
    private final HashMap<String, String> _data;
    /** ShaCode instance. */
    private final String _shaCode;
    /** SimpleDateFormat used to construct Timestamp. */
    private SimpleDateFormat formatter =
            new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z");
}
