package gitlet;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;

import static gitlet.Utils.*;
import static gitlet.Commit.*;

/** Gitlet Repository where Commands are executed.
 * @author Andrew Kaplan*/
class Repository {

    /** Current Working Directory. */
    static final File CWD = new File(".");
    /** Main metadata folder. */
    static final File MAIN_FOLDER = join(CWD, ".gitlet");
    /** Contains updated Branch files. */
    static final File REFS = join(MAIN_FOLDER, "refs");
    /** Contains Commit snapshots. */
    static final File COMMITS = join(MAIN_FOLDER, "commits");
    /** Contains Blob Objects. */
    static final File OBJECTS = join(MAIN_FOLDER, "objects");
    /** Master file, written into REFS. */
    static final File MASTER_FILE = join(REFS, "master");
    /** Head file. */
    static final File HEAD = join(MAIN_FOLDER, "HEAD");
    /** File containing Staging Area information. */
    static final File INDEX = join(MAIN_FOLDER, "INDEX");

    /** Initialize repository. */
    Repository() {
        COMMITS.mkdirs();
        REFS.mkdirs();
        OBJECTS.mkdirs();
        Commit initialCommit = new Commit();
        writeObject(INDEX, new StagingArea());

        File commit = join(COMMITS, initialCommit.shaCode());
        writeObject(commit, initialCommit);

        Branch master = new Branch("master", initialCommit.shaCode());
        writeObject(MASTER_FILE, master);

        _head = new Branch(master.name());
        writeObject(HEAD, _head);
    }

    /** Create blob class object from FILENAME and place blob in StagingArea
     *  write Blob object into file and store in REFS. */
    static void add(String fileName) {
        File file = join(CWD, fileName);
        if (!file.exists()) {
            throw new GitletException
            ("File does not exist.");
        }

        Blob b = new Blob(file, fileName);
        _head = readObject(HEAD, Branch.class);
        Commit commit = getHeadCommit(_head);
        _stagingArea = readObject(INDEX, StagingArea.class);

        boolean staged = _stagingArea.map().containsKey(b.fileName());
        boolean tracked = commit.data().containsKey(b.fileName());

        if (tracked || staged) {
            String commitBlobSha = commit.data().get(b.fileName());
            if (tracked && commitBlobSha.equals(b.shaCode())) {
                if (staged) {
                    _stagingArea.map().remove(b.fileName());
                }
                writeObject(join(OBJECTS, b.shaCode()), b);
            } else if (staged) {
                String stagedBlobSha = _stagingArea.map().get(b.fileName());
                if (b.shaCode().equals(stagedBlobSha)) {
                    return;
                }
                writeObject(join(OBJECTS, b.shaCode()), b);
                _stagingArea.map().put(b.fileName(), b.shaCode());
            } else {
                writeObject(join(OBJECTS, b.shaCode()), b);
                _stagingArea.map().put(b.fileName(), b.shaCode());
            }
        } else {
            writeObject(join(OBJECTS, b.shaCode()), b);
            _stagingArea.map().put(b.fileName(), b.shaCode());
        }
        writeObject(INDEX, _stagingArea);
    }

    /** Create new Commit from ARGS
     * write Commit object into file and Store in COMMITS. */
    static void commit(String[] args) {
        String msg = args[1];
        if (msg.matches("[\\s?]*")) {
            throw new GitletException
            ("Please enter a commit message.");
        }

        _stagingArea = readObject(INDEX, StagingArea.class);
        _head = readObject(HEAD, Branch.class);
        Commit parentCommit = getHeadCommit(_head);
        Commit newCommit;

        if (msg.equals("Merged")) {
            String mergeMsg = "Merged " + args[2] + " into " + args[3];
            newCommit = new Commit(mergeMsg, parentCommit.shaCode());
            newCommit.assignMergeParent(args[2]);
            if (args[4].equals("true")) {
                System.out.println("Encountered a merge conflict. ");
            }
            writeHeadUpdate(newCommit, _head);
            _stagingArea.map().clear();
            writeObject(INDEX, _stagingArea);
        } else {
            newCommit = new Commit(msg, parentCommit.shaCode());
            if (!newCommit.data().equals(parentCommit.data())) {
                writeHeadUpdate(newCommit, _head);
                _stagingArea.map().clear();
                writeObject(INDEX, _stagingArea);
            } else {
                throw new GitletException
                ("No changes added to the commit.");
            }
        }
    }

    /** Un-stage FILENAME if currently Staged, mark file for removal,
     *  delete File if in working directory. */
    static void rm(String fileName) {
        _head = readObject(HEAD, Branch.class);
        Commit commit = getHeadCommit(_head);
        _stagingArea = readObject(INDEX, StagingArea.class);

        boolean staged = _stagingArea.map().containsKey(fileName);
        boolean tracked = commit.data().containsKey(fileName);

        if (tracked || staged) {
            if (staged) {
                _stagingArea.map().remove(fileName);
            }
            if (tracked) {
                File blobFile = join(OBJECTS,
                        commit.data().get(fileName));
                Blob b = readObject(blobFile, Blob.class);
                b.changeRmStatus();
                writeObject(blobFile, b);
                File file = join(CWD, fileName);
                if (file.exists()) {
                    Utils.restrictedDelete(file);
                }
            }
        } else {
            throw new GitletException
            ("No reason to remove the file.");
        }
        writeObject(INDEX, _stagingArea);
    }


    /** Log the information of current Commit and all Parent Commits. */
    static void log() {
        _head = readObject(HEAD, Branch.class);
        File currBranchFile = join(REFS, _head.getBranch());
        Branch currBranch = readObject(currBranchFile, Branch.class);
        File currCommitFile = join(COMMITS, currBranch.pointer());
        Commit currCommit = readObject(currCommitFile, Commit.class);
        printLog(currCommit);
        while (!currCommit.parentID().equals("")) {
            File parentFile = join(COMMITS, currCommit.parentID());
            currCommit = readObject(parentFile, Commit.class);
            printLog(currCommit);
        }
    }

    /** Log the information of all Commits. */
    static void globalLog() {
        for (File commitFile : Objects.requireNonNull(COMMITS.listFiles())) {
            Commit commit = readObject(commitFile, Commit.class);
            printLog(commit);
        }
    }

    /** properly formats COMMIT log message. */
    private static void printLog(Commit commit) {
        System.out.println("===");
        System.out.println("commit " + commit.shaCode());
        if (!commit.merge().equals("")) {
            File parentFile = join(COMMITS, commit.parentID());
            Commit parent = readObject(parentFile, Commit.class);
            File mergeFile = join(COMMITS, commit.merge());
            Commit mergeParent = readObject(mergeFile, Commit.class);
            System.out.println("Merge: "
                    + parent.shaCode().substring(0, 6) + " "
                    + mergeParent.shaCode().substring(0, 6));
        }
        System.out.println("Date: " + commit.timestamp());
        System.out.println(commit.message());
        System.out.println();
    }

    /** Print Commit ID of all commits with COMMITMSG. */
    static void find(String commitMsg) {
        boolean found = false;
        for (File commitFile : Objects.requireNonNull(COMMITS.listFiles())) {
            Commit commit = readObject(commitFile, Commit.class);
            if (commit.message().equals(commitMsg)) {
                found = true;
                System.out.println(commit.shaCode());
            }
        }
        if (!found) {
            throw new GitletException
            ("Found no commit with that message.");
        }
    }

    /** Print status of the Repository to the terminal. */
    static void status() {
        _head = readObject(HEAD, Branch.class);
        _stagingArea = readObject(INDEX, StagingArea.class);
        String currBranchName = _head.getBranch();
        ArrayList<String> branches = new ArrayList<>();
        for (File branchFile : Objects.requireNonNull(REFS.listFiles())) {
            String branchName = branchFile.getName();
            if (branchName.equals(currBranchName)) {
                branchName = "*" + branchName;
            }
            branches.add(branchName);
        }
        ArrayList<String> modified = new ArrayList<>();
        ArrayList<String> add = new ArrayList<>();
        for (String blobSha : _stagingArea.map().values()) {
            File blobFile = join(OBJECTS, blobSha);
            Blob b = readObject(blobFile, Blob.class);
            if (checkDeleted(b)) {
                modified.add(b.fileName() + " (deleted)");
            } else if (checkModified(b)) {
                modified.add(b.fileName() + " (modified)");
            } else {
                add.add(b.fileName());
            }
        }
        Commit commit = getHeadCommit(_head);
        ArrayList<String> remove = new ArrayList<>();
        for (String blobSha : commit.data().values()) {
            File blobFile = join(OBJECTS, blobSha);
            Blob b = readObject(blobFile, Blob.class);
            if (b.rm()) {
                remove.add(b.fileName());
            } else if (checkDeleted(b)) {
                modified.add(b.fileName() + " (deleted)");
            } else if (checkModified(b) && (!add.contains(b.fileName()))) {
                modified.add(b.fileName() + " (modified)");
            }
        }
        ArrayList<String> untracked = new ArrayList<>();
        for (File untrackedFile : Objects.requireNonNull(CWD.listFiles())) {
            if (!untrackedFile.isDirectory()) {
                String untrackedName = untrackedFile.getName();
                if (!(commit.data().containsKey(untrackedName)
                        || add.contains(untrackedName)
                        || remove.contains(untrackedName)
                        || modified.contains(untrackedName))) {
                    untracked.add(untrackedName);
                }
            }
        }
        ArrayList<ArrayList<String>> args = new ArrayList<>();
        args.add(branches);
        args.add(add);
        args.add(remove);
        args.add(modified);
        args.add(untracked);
        printStatus(args);
    }

    /** Correctly format status message from ARGS content. */
    private static void printStatus(ArrayList<ArrayList<String>> args) {
        for (ArrayList<String> arg : args) {
            Collections.sort(arg);
        }
        StringBuilder status = new StringBuilder();
        String[] headers = {"=== Branches ===", "=== Staged Files ===",
            "=== Removed Files ===",
            "=== Modifications Not Staged For Commit ===",
            "=== Untracked Files ==="};
        int i = 0;
        for (ArrayList<String> arg : args) {
            status.append(headers[i]).append("\n");
            for (String file : arg) {
                status.append(file).append("\n");
            }
            i += 1;
            if (i < 5) {
                status.append("\n");
            }
        }
        System.out.println(status.toString());
    }

    /** Return true if file represented by blob B has been deleted. */
    private static Boolean checkDeleted(Blob b) {
        File cwdBlobFile = join(CWD, b.fileName());
        return !cwdBlobFile.exists();
    }

    /** Return true if represented by blob B has been modified. */
    private static Boolean checkModified(Blob b) {
        File cwdBlobFile = join(CWD, b.fileName());
        Blob cwdBlob = new Blob(cwdBlobFile, b.fileName());
        return !b.fileContents().equals(cwdBlob.fileContents());
    }



    /** Call checkout class to handle different checkout ARGS. */
    static void checkout(String[] args) {
        new Checkout(args);
    }

    /** Create new branch pointer BRANCHNAME
     *  point branch at current commit. */
    static void branch(String branchName) {
        _head = readObject(HEAD, Branch.class);
        Commit curr = getHeadCommit(_head);

        Branch newBranch = new Branch(branchName, curr.shaCode());
        File file = join(REFS, newBranch.name());
        if (!file.exists()) {
            writeObject(file, newBranch);
        } else {
            throw new GitletException
            ("A branch with that name already exists.");
        }
    }

    /** Delete BRANCHNAME if HEAD does not point to branch. */
    static void rmBranch(String branchName) {
        File file = join(REFS, branchName);
        if (file.exists()) {
            _head = readObject(HEAD, Branch.class);
            if (_head.getBranch().equals(branchName)) {
                throw new GitletException
                ("Cannot remove the current branch.");
            }
            file.delete();
        } else {
            throw new GitletException
            ("A branch with that name does not exist.");
        }
    }

    /** Call checkout class to handle reset ARGS. */
    static void reset(String[] args) {
        new Checkout(args);
    }

    /** Check conditions to merge BRANCHNAME to current branch. */
    static void checkMerge(String branchName) {
        _stagingArea = readObject(INDEX, StagingArea.class);
        _head = readObject(HEAD, Branch.class);
        File currBranchFile = join(REFS, _head.getBranch());
        Branch currBranch = readObject(currBranchFile, Branch.class);
        File currCommitFile = join(COMMITS, currBranch.pointer());
        Commit currCommit = readObject(currCommitFile, Commit.class);
        File givenBranchFile = join(REFS, branchName);
        if (!givenBranchFile.exists()) {
            throw new GitletException
            ("A branch with that name does not exist.");
        }

        Branch givenBranch = readObject(givenBranchFile, Branch.class);
        File givenCommitFile = join(COMMITS, givenBranch.pointer());
        Commit givenCommit = readObject(givenCommitFile, Commit.class);
        File parentFile = join(COMMITS, currCommit.parentID());
        Commit parent = readObject(parentFile, Commit.class);
        if (!parent.data().isEmpty()) {
            for (String blobSha : currCommit.data().values()) {
                File blobFile = join(OBJECTS, blobSha);
                Blob b = readObject(blobFile, Blob.class);
                if (b.rm()) {
                    throw new GitletException
                    ("You have uncommitted changes.");
                }
            }
        }

        if (!_stagingArea.map().isEmpty()) {
            throw new GitletException
            ("You have uncommitted changes.");
        }

        for (File file : Objects.requireNonNull(CWD.listFiles())) {
            if (!file.isDirectory()) {
                String fileName = file.getName();
                Checkout.untrackedFile(currCommit, givenCommit, fileName);
            }
        }

        if (currBranch.name().equals(branchName)) {
            throw new GitletException
            ("Cannot merge a branch with itself.");
        }
        splitPoint(currCommit, givenCommit, givenBranch, currBranch);
    }

    /** Find the splitpoint of CURR and GIVEN from ancestors on
     * CURRBRANCH and GIVENBRANCH. call merge on all 3 commits. */
    private static void splitPoint(Commit curr, Commit given,
                                   Branch givenBranch, Branch currBranch) {

        HashMap<String, Integer> givenPath =
                givenAncestors(given, 0, new HashMap<>());
        assert (givenPath != null);
        String splitID = findSplitPoint(givenPath, curr);

        if (splitID.equals(given.shaCode())) {
            System.out.println("Given branch is an ancestor "
                    + "of the current branch.");
            return;
        } else if (splitID.equals(curr.shaCode())) {
            currBranch.updatePointer(given.shaCode());
            writeObject(join(REFS, currBranch.name()), currBranch);
            System.out.println("Current branch fast-forwarded.");
            return;
        }
        File splitFile = join(COMMITS, splitID);
        Commit split = readObject(splitFile, Commit.class);

        ArrayList<String> allFileNames = new ArrayList<>(split.data().keySet());
        allFileNames.addAll(curr.data().keySet());
        allFileNames.addAll(given.data().keySet());
        merge(allFileNames, split, curr, given);
        commit(new String[] {"commit", "Merged", givenBranch.name(),
                currBranch.name(), _conflict});
    }

    /** Merge ALLFILENAMES included in SPLIT, CURR, & GIVEN into new Commit. */
    private static void merge(ArrayList<String> allFileNames,
                              Commit split, Commit curr, Commit given) {
        for (String fileName : allFileNames) {
            if (split.data().containsKey(fileName)
                    && curr.data().containsKey(fileName)
                    && given.data().containsKey(fileName)) {
                String splitBlob = split.data().get(fileName);
                String currBlob = curr.data().get(fileName);
                String givenBlob = given.data().get(fileName);

                if (splitBlob.equals(currBlob) && splitBlob.equals(givenBlob)) {
                    break;
                } else if (currBlob.equals(givenBlob)) {
                    break;
                } else {
                    mergeConflict(currBlob, givenBlob, fileName);
                }
            } else if (split.data().containsKey(fileName)
                    && curr.data().containsKey(fileName)) {
                String splitBlob = split.data().get(fileName);
                String currBlob = curr.data().get(fileName);

                if (splitBlob.equals(currBlob)) {
                    rm(fileName);
                    break;
                }
                mergeConflict(currBlob, "", fileName);
            } else if (split.data().containsKey(fileName)
                    && given.data().containsKey(fileName)) {
                String splitBlob = split.data().get(fileName);
                String givenBlob = given.data().get(fileName);

                if (splitBlob.equals(givenBlob)) {
                    break;
                }
                mergeConflict("", givenBlob, fileName);
            } else if (curr.data().containsKey(fileName)
                    && given.data().containsKey(fileName)) {
                String currBlob = curr.data().get(fileName);
                String givenBlob = given.data().get(fileName);

                if (currBlob.equals(givenBlob)) {
                    break;
                }
                mergeConflict(currBlob, givenBlob, fileName);
            } else if (given.data().containsKey(fileName)) {
                checkout(new String[]
                    {"checkout", given.shaCode(), "--", fileName});
                add(fileName);
            }
        }
    }

    /** Write file with FILENAME with contents from
     *  blobs with CURRSHA and GIVENSHA into Working Directory. */
    private static void mergeConflict(String currSha,
                                      String givenSha, String fileName) {

        File currFile = join(OBJECTS, currSha);
        File givenFile = join(OBJECTS, givenSha);

        String currFileContents = "";
        String givenFileContents = "";
        if (currFile.exists() && givenFile.exists()) {
            Blob curr = readObject(currFile, Blob.class);
            Blob given = readObject(givenFile, Blob.class);
            currFileContents = curr.fileContents();
            givenFileContents = given.fileContents();
        } else if (currFile.exists()) {
            Blob curr = readObject(currFile, Blob.class);
            currFileContents = curr.fileContents();
        } else if (givenFile.exists()) {
            Blob given = readObject(givenFile, Blob.class);
            givenFileContents = given.fileContents();
        }
        String mergeFileContents = "<<<<<<< HEAD" + "\n" + currFileContents
                + "=======" + "\n" + givenFileContents + ">>>>>>>";
        File mergeFile = join(CWD, fileName);
        writeContents(mergeFile, mergeFileContents);
        add(fileName);
        _conflict = "true";
    }

    /** Return Commit of HEAD pointer. */
    static Commit getHeadCommit(Branch head) {
        File headBranchFile = join(REFS, head.getBranch());
        Branch headBranch = readObject(headBranchFile, Branch.class);
        File commitFile = join(COMMITS, headBranch.pointer());
        return readObject(commitFile, Commit.class);
    }

    /** Write COMMIT, current branch, and HEAD files. */
    static void writeHeadUpdate(Commit commit, Branch head) {
        File commitFile = join(COMMITS, commit.shaCode());
        writeObject(commitFile, commit);
        File headBranchFile = join(REFS, head.getBranch());
        Branch headBranch = readObject(headBranchFile, Branch.class);
        headBranch.updatePointer(commit.shaCode());
        writeObject(headBranchFile, headBranch);
        head.updateHead(headBranch.name());
        writeObject(HEAD, head);
    }

    /** Return current Staging Area. */
    static StagingArea stagingArea() {
        return _stagingArea;
    }

    /** Branch instance of HEAD. */
    private static Branch _head;
    /** StagingArea instance. */
    private static StagingArea _stagingArea;
    /** String to track whether merge involved conflict. */
    private static String _conflict = "false";
}
