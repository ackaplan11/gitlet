package gitlet;

import java.io.File;
import java.util.HashMap;
import java.util.Objects;

import static gitlet.Repository.*;
import static gitlet.Utils.*;

/** Handle calls to Checkout and Reset from Repository.
 * @author Andrew Kaplan */
class Checkout {

    /** Parse different checkout calls into correct command.
     * @param args include [filename], [commitID] -- [filename], [branch] */
    Checkout(String[] args) {
        _head = readObject(HEAD, Branch.class);
        File headBranchFile = join(REFS, _head.getBranch());
        Branch headBranch = readObject(headBranchFile, Branch.class);
        switch (args.length) {
        case 2:
            if (args[0].equals("reset")) {
                reset(args[1]);
                break;
            }
            File branchFile = join(REFS, args[1]);
            if (!branchFile.exists()) {
                throw new GitletException
                ("No such branch exists.");
            }
            Branch branch = readObject(branchFile, Branch.class);
            if (branch.name().equals(headBranch.name())) {
                throw new GitletException
                ("No need to checkout the current branch.");
            }
            checkoutBranch(branch, headBranch);
            break;
        case 3:
            if (!args[1].equals("--")) {
                throw new GitletException("Incorrect operands.");
            }

            File currCommitFile = join(COMMITS, headBranch.pointer());
            Commit curr = readObject(currCommitFile, Commit.class);
            checkoutFile(curr, args[2]);
            break;
        case 4:
            if (!args[2].equals("--")) {
                throw new GitletException("Incorrect operands.");
            }
            String commitID = args[1];
            int idLength = commitID.length();
            commitID = getCommitID(commitID, idLength);
            File commitFile = join(COMMITS, commitID);
            if (!commitFile.exists()) {
                throw new GitletException
                ("No commit with that id exists.");
            }
            Commit commit = readObject(commitFile, Commit.class);
            checkoutFile(commit, args[3]);
            break;
        default:
            throw new GitletException("Incorrect operands.");
        }
    }

    /** Checkout all files of the CHECKBRANCH pointer Commit,
     *  Create current commit from HEADBRANCH. */
    static void checkoutBranch(Branch checkBranch, Branch headBranch) {
        File checkCommitFile = join(COMMITS, checkBranch.pointer());
        File currCommitFile = join(COMMITS, headBranch.pointer());
        Commit currCommit = readObject(currCommitFile, Commit.class);
        Commit checkCommit = readObject(checkCommitFile, Commit.class);
        checkoutCommit(currCommit, checkCommit);
        _head.updateHead(checkBranch.name());
        writeObject(HEAD, _head);
    }

    /** Checkout file to Working Directory from
     *  CHECKOUT if file tracked in CURR. */
    static void checkoutCommit(Commit curr, Commit checkout) {
        for (String key : curr.data().keySet()) {
            if (!checkout.data().containsKey(key)) {
                restrictedDelete(key);
            }
        }
        for (String fileName : checkout.data().keySet()) {
            untrackedFile(checkout, curr, fileName);
            checkoutFile(checkout, fileName);
        }
    }

    /** Take file with FILENAME from COMMIT and write into Working Directory. */
    static void checkoutFile(Commit commit, String fileName) {
        if (commit.data().containsKey(fileName)) {
            String blobSha = commit.data().get(fileName);
            File blobFile = join(OBJECTS, blobSha);
            Blob b = readObject(blobFile, Blob.class);
            File file = join(CWD, b.fileName());
            writeContents(file, b.fileContents());
        } else {
            throw new GitletException
            ("File does not exist in that commit.");
        }
    }

    /** Return COMMITID of length IDLENGTH if Commit exists. */
    static String getCommitID(String commitID, int idLength) {
        if (idLength == FULL_LENGTH) {
            return commitID;
        }
        HashMap<String, String> commitIDMap = new HashMap<>();
        for (File commit : Objects.requireNonNull
            (Repository.COMMITS.listFiles())) {
            commitIDMap.put(commit.getName().substring(0, idLength),
                    commit.getName());
        }
        if (!commitIDMap.containsKey(commitID)) {
            throw new GitletException
            ("No commit with that id exists.");
        }
        return commitIDMap.get(commitID);
    }

    /** Throw Untracked File exception if file with FILENAME
     *  exists in OTHER and does not exist in CURR. */
    static void untrackedFile(Commit curr, Commit other, String fileName) {
        if (!curr.data().containsKey(fileName)) {
            if (other.data().containsKey(fileName)) {
                if (join(Repository.CWD, fileName).exists()) {
                    throw new GitletException
                    ("There is an untracked file in the way; "
                            + "delete it or add it first.");
                }
            }
        }
    }

    /** Reset Working Directory to state of Commit with COMMITID. */
    static void reset(String commitID) {
        int idLength = commitID.length();
        commitID = getCommitID(commitID, idLength);
        File commitFile = join(COMMITS, commitID);
        if (!commitFile.exists()) {
            throw new GitletException
            ("No commit with that id exists.");
        }
        Commit resetCommit = readObject(commitFile, Commit.class);
        _stagingArea = readObject(INDEX, StagingArea.class);
        _head = readObject(HEAD, Branch.class);
        Commit currCommit = getHeadCommit(_head);
        checkoutCommit(currCommit, resetCommit);

        writeHeadUpdate(resetCommit, _head);
        _stagingArea.map().clear();
        writeObject(INDEX, _stagingArea);
    }

    /** Branch instance of HEAD. */
    private static Branch _head;
    /** StagingArea instance. */
    private static StagingArea _stagingArea;
    /** Full length of Commit IDs. */
    private static final int FULL_LENGTH = 40;
}
