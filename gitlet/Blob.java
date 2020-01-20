package gitlet;
import static gitlet.Utils.*;
import java.io.Serializable;
import java.io.File;

/** Represents the contents of a file in the working directory.
 * @author Andrew Kaplan */
class Blob implements Serializable {

    /** Blob Constructor.
     * @param file : File object to be represented by BLob
     * @param fileName : Name of File object */
    Blob(File file, String fileName) {
        _fileContents = readContentsAsString(file);
        _fileName = fileName;
        _rm = false;
        _shaCode = sha1(_fileName + _fileContents);
    }

    /** Return _fileContents. */
    String fileContents() {
        return _fileContents;
    }

    /** Return _fileName. */
    String fileName() {
        return _fileName;
    }

    /** Return RM status.  */
    Boolean rm() {
        return _rm;
    }

    /** Return shaCode. */
    String shaCode() {
        return _shaCode;
    }

    /** Mark blob for removal. */
    void changeRmStatus() {
        _rm = !_rm;
    }

    /** String instance representation of the file contents. */
    private final String _fileContents;
    /** File name instance. */
    private final String _fileName;
    /** ShaCode instance. */
    private final String _shaCode;
    /** Boolean instance, tracks removal status. */
    private boolean _rm;
}
