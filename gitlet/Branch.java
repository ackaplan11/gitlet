package gitlet;

import java.io.Serializable;

/** This class acts as a pointer to commits on the commit tree.
 *  HEAD is a special branch that points to a branch instead of a commit.
 *  @author Andrew Kaplan*/
class Branch implements Serializable {

    /** Branch Constructor.
     * @param name : Branch name inputted by User
     * @param commitID : CommitID of Commit that branch will point to. */
    Branch(String name, String commitID) {
        _name = name;
        _pointer = commitID;
    }

    /** HEAD Constructor.
     * @param branchName : Branch name where HEAD will point. */
    Branch(String branchName) {
        _name = "HEAD";
        _currentBranch = branchName;
    }

    /** Return Name. */
    String name() {
        return _name;
    }

    /** Return Pointer. */
    String pointer() {
        return _pointer;
    }

    /** Return HEAD branch pointer. */
    String getBranch() {
        return _currentBranch;
    }

    /** Update branch pointer to CURRID. */
    void updatePointer(String currID) {
        _pointer = currID;
    }

    /** Update HEAD branch pointer to BRANCHNAME. */
    void updateHead(String branchName) {
        _currentBranch = branchName;
    }

    /** Branch name instance. */
    private String _name;
    /** String pointer instance to Commit ID. */
    private String _pointer;
    /** String pointer instance to Branch Name. */
    private String _currentBranch;
}
