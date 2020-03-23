/**
 * Subclass of the ArrayVersion interface
 * Defines logic for non-phi array variables.
 */
class ArrayVersionSingle implements ArrayVersion {
    private int version;
    private boolean diff_ver_match;

    /**
     * Create a new ArrayVersionSinlge
     * @param version create a new ArrayVersionPhi with the passed version
     */
    ArrayVersionSingle(int version) {
        this.version = version;
        this.diff_ver_match = false;
    }

    /**
     * copy constructor for ArrayVersionSingle
     * @param avs the original ArrayVersionSingle
     */
    ArrayVersionSingle(ArrayVersionSingle avs) {
        this.version = avs.version;
        this.diff_ver_match = avs.diff_ver_match;
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
     * Overridden version incrementer, increases the version by 1
     */
    @Override
    public void incr_version() {
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
}
