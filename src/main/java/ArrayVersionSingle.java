import soot.jimple.Stmt;

import java.util.HashMap;
import java.util.Map;

/**
 * Subclass of the ArrayVersion interface
 * Defines logic for non-phi array variables.
 */
class ArrayVersionSingle implements ArrayVersion {
    // TODO: the FIRST write should have a _1!!!
    /**
     * the current version
     */
    private int version;
    /**
     * a map of versions coupled with statements
     */
    private final Map<Integer, Stmt> versions;
    /**
     * the number of the block of code being currently analyzed
     */
    private int block;
    /**
     * flag to tell if we happen to have the same names, for printing mostly (only for phi implementation
     * of ArrayVersion)
     */
    private final boolean diff_ver_match;
    /**
     * flag to tell if this has been written to
     */
    private boolean has_been_written_to;
    /**
     * the line number of the version change
     */
    private int line_num;

    /**
     * Create a new ArrayVersionSingle
     * @param version create a new ArrayVersionPhi with the passed version
     * @param block the block that created this AV
     * @param stmt the  statement changing this version
     * @param line_num the line num of the version change
     */
    ArrayVersionSingle(int version, int block, Stmt stmt, int line_num) {
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
        this.line_num = avs.line_num;
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
     * force a version increase (useful for merges to avoid id conflicts)
     */
    @Override
    public void force_incr_version() {
        version++;
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

    /**
     * get the versions map
     * @return the versions map
     */
    @Override
    public Map<Integer, Stmt> get_versions() {
        return new HashMap<>(versions);
    }

    /**
     * get the line number of the array version change
     * @return the line number
     */
    @Override
    public int get_line_num() {
        return line_num;
    }

}
