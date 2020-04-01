import org.tinylog.Logger;
import soot.toolkits.graph.Block;

import java.util.HashMap;
import java.util.Map;

class DownwardExposedArrayRef {

    private Block b;
    private Map<String, ArrayVersion> array_ver;

    /**
     * create a new downward exposed array reference
     * This class is to keep track of what array versions are exposed to successor blocks.
     * i.e. what versions do the successor block see/how do they change in this block
     * @param b the block
     */
    DownwardExposedArrayRef(Block b) {
        this.b = b;
        this.array_ver = new HashMap<>();
    }

    /**
     * copies a DownwardExposedArrayRef object
     * @param daf the origin DownwardExposedArrayRef
     */
    DownwardExposedArrayRef(DownwardExposedArrayRef daf) {
        this.b = daf.b;
        this.array_ver = new HashMap<>();
        for(Map.Entry<String, ArrayVersion> entry : daf.array_ver.entrySet()) {
            this.array_ver.put(entry.getKey(), Utils.copy_av(entry.getValue()));
        }
    }

    /**
     * get the version of a given variable
     * @param s the variable name
     * @return the version of the variable or a constant indicating it was not found.
     */
    ArrayVersion get(String s) {
        return array_ver.getOrDefault(s, Constants.VAR_NOT_FOUND);
    }

    /**
     * create a new version or an array variable
     * @param s the variable name
     * @param block_num the block number that is making a new version
     */
    void new_ver(String s, int block_num) {
        // CHECKTHIS: this might need to be more explicit (i.e. new_ver(String s, int i) or something)
        if(array_ver.containsKey(s)) {
            ArrayVersion new_ver = array_ver.get(s);
            new_ver.incr_version(block_num);
            array_ver.put(s, new_ver);
        } else {
            Logger.error("Key " + s + " not found, cannot increment version.");
        }
    }

    /**
     * explicitly put a variable with a variable version in the map
     * @param s the variable name
     * @param av the variable version object
     */
    void put(String s, ArrayVersion av) {
        array_ver.put(s, av);
    }

    /**
     * checks if a variable name exists in the array variable version map
     * @param s the array variable
     * @return true iff the variable name is in the key set of the versioning map
     */
    @SuppressWarnings("unused")
    boolean contains_var(String s) {
        return array_ver.containsKey(s);
    }

    /**
     * get a variable/version name. This is a custom construct
     * @param s the variable name
     * @return the variable/version name iff it is in the name is in the key set otherwise null
     */
    String get_name(String s) {
        if(array_ver.containsKey(s)) {
            return String.format(Constants.ARR_VER_STR, s, array_ver.get(s).get_version(), Constants.EMPTY_STR);
        } else {
            Logger.error("Key " + s + " not found.");
            return null;
        }
    }
}
