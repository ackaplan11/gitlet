package gitlet;
import static gitlet.Repository.*;


/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Andrew Kaplan */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) {
        try {
            new Main(args);
        } catch (GitletException ex) {
            System.err.print(ex.getMessage());
            System.exit(0);
        }
    }

    /** Parse User ARGS to correct Gitlet command. */
    private Main(String[] args) {
        if (args.length == 0) {
            throw new GitletException("Please enter a command.");
        } else if (args[0].equals("init") && args.length == 1) {
            uninitialized();
            new Repository();
            return;
        }
        initialized();
        switch (args[0]) {
        case "add":
            add(args[1]);
            break;
        case "commit":
            checkArgsLength(args.length, 2);
            commit(args);
            break;
        case "rm":
            checkArgsLength(args.length, 2);
            rm(args[1]);
            break;
        case "log":
            checkArgsLength(args.length, 1);
            log();
            break;
        case "global-log":
            checkArgsLength(args.length, 1);
            globalLog();
            break;
        case "find":
            checkArgsLength(args.length, 2);
            find(args[1]);
            break;
        case "status":
            checkArgsLength(args.length, 1);
            status();
            break;
        case "checkout":
            checkout(args);
            break;
        case "branch":
            checkArgsLength(args.length, 2);
            branch(args[1]);
            break;
        case "rm-branch":
            checkArgsLength(args.length, 2);
            rmBranch(args[1]);
            break;
        case "reset":
            reset(args);
            break;
        case "merge":
            checkArgsLength(args.length, 2);
            checkMerge(args[1]);
            break;
        default:
            throw new GitletException("No command with that name exists.");
        }
    }



    /** Throw GitletException if Repository is not initialized. */
    private static void initialized() {
        if (!MAIN_FOLDER.exists()) {
            throw new
            GitletException("Not in an initialized Gitlet directory.");
        }
    }

    /** Throw GitletException if Repository is already initialized. */
    private static void uninitialized() {
        if (MAIN_FOLDER.exists()) {
            throw new
            GitletException("A Gitlet version-control system "
                    + "already exists in the current directory.");
        }
    }

    /** Throw GitletException for improper
     *  argument LENGTH against CORRECTLENGTH. */
    private static void checkArgsLength(int length, int correctLength) {
        if (length != correctLength) {
            throw new GitletException("Incorrect operands.");
        }
    }
}
