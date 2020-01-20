package gitlet;

import java.io.Serializable;
import java.util.HashMap;

/** Staging Area for added Blob Objects.
 * @author Andrew Kaplan */
class StagingArea implements Serializable {

    /** StagingArea constructor. */
    StagingArea() {
        _map = new HashMap<>();
    }

    /** Return Map. */
    HashMap<String, String> map() {
        return _map;
    }

    /** HashMap instance of Blob fileNames --> Blob shaCodes. */
    private HashMap<String, String> _map;
}
