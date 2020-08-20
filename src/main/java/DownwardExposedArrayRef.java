import org.tinylog.Logger;
import soot.jimple.Stmt;
import soot.toolkits.graph.Block;

import java.util.Map;

/**
 * Class representing the downward exposed array versions for a given block.
 * A block only knows the versions that are passed to it by predecessor blocks (Part of BFS)
 */
class DownwardExposedArrayRef {
    /**
     * the block
     */
    private final Block b;
    /**
     * the array versions
     */
    private final ArrayVariables array_vars;

    /**
     * create a new downward exposed array reference
     * This class is to keep track of what array versions are exposed to successor blocks.
     * i.e. what versions do the successor block see/how do they change in this block
     * @param b the block
     */
    DownwardExposedArrayRef(Block b) {
        this.b = b;
        this.array_vars = new ArrayVariables();
    }

    /**
     * copies a DownwardExposedArrayRef object
     * @param daf the origin DownwardExposedArrayRef
     */
    DownwardExposedArrayRef(DownwardExposedArrayRef daf) {
        this.b = daf.b;
        this.array_vars = new ArrayVariables(daf.array_vars);
        for(Map.Entry<String, ArrayVersion> entry : daf.array_vars.entry_set()) {
            this.array_vars.put(entry.getKey(), Utils.copy_av(entry.getValue()));
        }
    }

    /**
     * get the version of a given variable
     * @param s the variable name
     * @return the version of the variable or a constant indicating it was not found.
     */
    ArrayVersion get(String s) {
        return array_vars.get(s);
    }

    /**
     * create a new version or an array variable
     * @param s the variable name
     * @param block_num the block number that is making a new version
     * @param stmt the statement that changed the versioning
     */
    void new_ver(String s, int block_num, Stmt stmt) {
        if(array_vars.contains_key(s)) {
            ArrayVersion new_ver = array_vars.get(s);
            new_ver.incr_version(block_num, stmt);
            array_vars.put(s, new_ver);
        } else {
            Logger.error("Key " + s + " not found, cannot increment version.");
        }
    }

    /**
     * force an array version update
     * @param s the variable name
     */
    void force_incr(String s) {
        ArrayVersion av = array_vars.get(s);
        av.force_incr_version();
        array_vars.put(s, av);
    }

    /**
     * explicitly put a variable with a variable version in the map
     * @param s the variable name
     * @param av the variable version object
     */
    void put(String s, ArrayVersion av) {
        array_vars.put(s, av);
    }

    /**
     * checks if a variable name exists in the array variable version map
     * @param s the array variable
     * @return true iff the variable name is in the key set of the versioning map
     */
    @SuppressWarnings("unused")
    boolean contains_var(String s) {
        return array_vars.contains_key(s);
    }

    /**
     * get a variable/version name. This is a custom construct
     * @param s the variable name
     * @return the variable/version name iff it is in the name is in the key set otherwise null
     */
    String get_name(String s) {
        if(array_vars.contains_key(s)) {
            return String.format(Constants.ARR_VER_STR, s, array_vars.get(s).get_version());
        } else {
            Logger.error("Key " + s + " not found.");
            return null;
        }
    }

    /**
     * return whether this block has no array vars
     * @return true iff there are no array variables in array_vars
     */
    boolean is_empty() {
        return array_vars.is_empty();
    }
}
