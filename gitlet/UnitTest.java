package gitlet;
import ucb.junit.textui;
import org.junit.Test;

import java.io.File;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.Instant;

import static gitlet.Utils.*;
import static gitlet.Repository.*;
import static org.junit.Assert.*;

/** The suite of all JUnit tests for the gitlet package.
 *  @author Andrew Kaplan */
public class UnitTest {

    /** Run the JUnit tests in the loa package. Add xxxTest.class entries to
     *  the arguments of runClasses to run other JUnit tests. */
    public static void main(String[] ignored) {
        System.exit(textui.runClasses(UnitTest.class));
    }

    /** Test correct formatting of timestamp. */
    @Test
    public void initialTimeStamp() {
        Commit commit = new Commit();
        SimpleDateFormat formatter =
                new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z");
        String initialTime = formatter.format(Date.from(Instant.EPOCH));
        assertEquals(initialTime, commit.timestamp());
    }


    /** Blobs get the correct file name. */
    @Test
    public void testBlobName() {
        Blob b = new Blob(join(CWD, "junit.txt"), "junit.txt");
        File blobFile = join(CWD, b.fileName());
        assertEquals(blobFile.getName(), b.fileName());
    }

    /** Blobs with different  names
     * but the same contents have the same content. */
    @Test
    public void testBlobContents() {
        Blob testBlob = new Blob(join(CWD, "junit.txt"), "junit.txt");
        Blob otherTestBlob = new Blob(join(CWD, "junit.txt"), "junit.txt");
        assertEquals(testBlob.fileContents(),
                otherTestBlob.fileContents());
    }

    /** Blobs with different file names
     * but the same contents have the same content. */
    @Test
    public void testSameBlobContents() {
        Blob junit1 = new Blob(join(CWD, "junit.txt"), "junit.txt");
        Blob junit2 = new Blob(join(CWD, "junit2.txt"), "junit.txt");
        assertEquals(junit1.fileContents(), junit2.fileContents());
    }

    /** Continued... but have different ShaCode. */
    @Test
    public void testDifferentBlobShaCode() {
        Blob b1 = new Blob(join(CWD, "junit.txt"), "junit.txt");
        Blob b2 = new Blob(join(CWD, "junit2.txt"), "junit.txt");
        assertNotEquals(b1.shaCode(), b2.shaCode());
    }

    /** Blobs of Files with different contents have different contents */
    @Test
    public void testDifferentBlobContents() {
        Blob junit1 = new Blob(join(CWD, "junit.txt"), "junit.txt");
        Blob junit3 = new Blob(join(CWD, "junit3.txt"), "junit.txt");
        assertNotEquals(junit1.fileContents(), junit3.fileContents());
    }

    @Test
    public void testHeadBranchPointer() {
        Branch newBranch = new Branch("new", "testCommitID");
        Branch head = new Branch(newBranch.name());
        assertEquals(head.getBranch(), newBranch.name());
    }

    @Test
    public void testBranchPointer() {
        Commit commit = new Commit();
        Branch newBranch = new Branch("new", commit.shaCode());
        assertEquals(newBranch.pointer(), commit.shaCode());
    }

    /** Can't restrictedDelete a final without initializing Gitlet. */
    @Test (expected = IllegalArgumentException.class)
    public void testRestrictedDelete() {
        File junit = join(CWD, "junit.txt");
        assert (junit.exists());
        restrictedDelete(junit);
    }

    @Test
    public void mergePrep() {
        Main.main("init");
        Main.main("add", "a.txt");
        Main.main("add", "b.txt");
        Main.main("commit", "'two files'");
        Main.main("branch", "other");
        Main.main("add", "c.txt");
        Main.main("rm", "b.txt");
        Main.main("commit", "'add c.txt, remove b.txt'");
        Main.main("checkout", "other");
        Main.main("rm", "a.txt");
        Main.main("add", "d.txt");
        Main.main("commit", "'add d.txt, remove a.txt'");

    }
    @Test
    public void merge() {
        Main.main("checkout", "master");
        Main.main("merge", "other");
    }


}


