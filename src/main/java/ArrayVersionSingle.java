import soot.jimple.Stmt;

import java.util.HashMap;
import java.util.Map;

/**
 * Subclass of the ArrayVersion interface
 * Defines logic for non-phi array variables.
 */
@SuppressWarnings("FieldMayBeFinal")
class ArrayVersionSingle implements ArrayVersion {
    private int version;
    private int block;
    private boolean diff_ver_match;
    private boolean has_been_written_to;
    private Map<Integer, Stmt> versions;

    /**
     * Create a new ArrayVersionSingle
     * @param version create a new ArrayVersionPhi with the passed version
     * @param block the block that created this AV
     * @param stmt the  statement changing this version
     */
    ArrayVersionSingle(int version, int block, Stmt stmt) {
        this.version = version;
        this.diff_ver_match = false;
        this.block = block;
        has_been_written_to = false;
        this.versions = new HashMap<>();
        versions.put(version, stmt);
    }

    /**
     * copy constructor for ArrayVersionSingle
     * @param avs the original ArrayVersionSingle
     */
    ArrayVersionSingle(ArrayVersionSingle avs) {
        this.version = avs.version;
        this.diff_ver_match = avs.diff_ver_match;
        this.block = avs.block;
        has_been_written_to = avs.has_been_written_to;
        this.versions = new HashMap<>(avs.versions);
    }

    /**
     * Overridden getter
     * @return false always, this does not ever represent a phi node
     */
    @Override
    public boolean is_phi() {
        return false;
    }


    /**
     * Overridden getter, returns diff_ver_match
     * @return true iff the variables in the phi node have the same name (this can happen on branching)
     *         NOTE: in this class this should _always_ be false.
     */
    @Override
    public boolean has_diff_ver_match() {
        return diff_ver_match;
    }

    /**
     * get the last block to change this AV
     * @return the last block to change this AV
     */
    @Override
    public int get_block() {
        return block;
    }

    /**
     * Overridden version incrementer, increases the version by 1
     * @param block the block that changed it.
     * @param stmt the assignment statement that changed the versioning
     */
    @Override
    public void incr_version(int block, Stmt stmt) {
        this.block = block;
        version++;
        versions.put(version, stmt);
    }

    /**
     * Overridden getter return version
     * @return the current version
     */
    @Override
    public int get_version() {
        return version;
    }

    /**
     * getter returning whether array has been written to
     * @return true iff the array var has been written to
     */
    @Override
    public boolean has_been_written_to() {
        return has_been_written_to;
    }

    /**
     * setter to set the has been written to flag
     */
    @Override
    public void toggle_written() {
        has_been_written_to = true;
    }

    @Override
    public Map<Integer, Stmt> get_versions() {
        return new HashMap<>(versions);
    }

}
