import soot.jimple.Stmt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Subclass of the ArrayVersion interface
 * Defines logic for phi array variables.
 */
public class ArrayVersionPhi implements ArrayVersion {
    /**
     * the current version
     */
    private int version;
    /**
     * the list of all array versions
     */
    private final List<ArrayVersion> array_versions;
//    /**
//     * flag to tell if we happen to have the same names, for printing mostly
//     */
//    private final boolean diff_ver_match;
    /**
     * TODO: this is not longer needed
     * the block number for the merge
     */
    private int block;
    /**
     * flag to tell if this has been written to
     */
    private boolean has_been_written_to;
    /**
     * flag to tell if this has been read
     */
    private boolean has_been_read;
    /**
     * all the versions
     */
    private final Map<Integer, Stmt> versions;
    /**
     * the line number of the version change
     */
    private final int line_num;

    /**
     * create a new ArrayVersionPhi
     * @param array_versions the individual array versions that compose this phi variable
     * @param block the block that created this AV
     * @param line_num the line number of the version change
     */
    ArrayVersionPhi(List<ArrayVersion> array_versions, int block, int line_num) {
        this.version = array_versions.stream()
                .map(ArrayVersion::get_version)
                .reduce(Integer.MIN_VALUE, Math::max) + 1;
        this.array_versions = array_versions;
        // List<Integer> versions = array_versions.stream().map(ArrayVersion::get_version).collect(Collectors.toList());
        // this.diff_ver_match = versions.size() != new HashSet<>(versions).size();
        this.block = block;
        this.has_been_written_to = false;
        this.versions = new HashMap<>();
        this.line_num = line_num;
    }

    /**
     * Copy constructor for an ArrayVersionPhi
     * @param av_phi the original ArrayVersionPhi
     */
    ArrayVersionPhi(ArrayVersionPhi av_phi) {
        this.version = av_phi.version;
        this.array_versions = av_phi.array_versions;
        // this.diff_ver_match = av_phi.diff_ver_match;
        this.block = av_phi.block;
        this.has_been_written_to = av_phi.has_been_written_to;
        this.versions = new HashMap<>(av_phi.versions);
        this.line_num = av_phi.line_num;
        this.has_been_read = av_phi.has_been_read;
    }

    /**
     * getter for the List of Array versions composing this ArrayVersionPhi
     * @return the List of ArrayVersion in this ArrayVersionPhi
     */
    List<ArrayVersion> get_array_versions() {
        List <ArrayVersion> tmp = new ArrayList<>();
        for(ArrayVersion av : array_versions) {
            tmp.add(Utils.copy_av(av));
        }
        return tmp;
    }

    /**
     * Overridden getter
     * @return true always, this always represents a phi node
     */
    @Override
    public boolean is_phi() {
        return true;
    }

    /**
     * Overridden version incrementer, increases the version by 1
     * @param block the block that changed this AV
     * @param stmt the statement that changed the versioning
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

//    /**
//     * TODO: this is not longer needed
//     * Overridden getter, returns diff_ver_match
//     * @return true iff the variables in the phi node have the same name (this can happen on branching)
//     */
//    @Override
//    public boolean has_diff_ver_match() {
//        return diff_ver_match;
//    }

    /**
     * get the last block to change this AV
     * @return the last block to change this AV
     */
    @Override
    public int get_block() {
        return block;
    }

    /**
     * getter returning whether array has been written to
     * always false for phi (should never use!)
     * @return true iff the array var has been written to
     */
    @Override
    public boolean has_been_written_to() {
        return false;
    }

    /**
     * flag to tell whether an array has been read
     * always false for phi (should never use!)
     * @return true iff the array has been read
     */
    @Override
    public boolean has_been_read() {
        return false;
    }

    /**
     * setter to set the has been written to flag
     */
    @Override
    public void toggle_written() {
        this.has_been_written_to = true;
    }

    /**
     * setter to set the read flag
     */
    @Override
    public void toggle_read() {
        this.has_been_read = true;
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
