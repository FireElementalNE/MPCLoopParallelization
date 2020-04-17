/**
 * interface for ArrayVersions (Phi and Single)
 */
interface ArrayVersion {
    /**
     * return phi flag
     * @return true iff the subclass is ArrayVersionPhi, false otherwise
     */
    boolean is_phi();

    /**
     * get the current version
     * @return the current version
     */
    int get_version();

    /**
     * increment the current version by 1
     * @param block the block that is being parsed
     */
    void incr_version(int block);

    /**
     * check if the variables in an ArrayVersionPhi have the same names
     * @return true iff the variables in an ArrayVersionPhi have the same name, false if
     *         called on an ArrayVersionSingle, or if the names in an ArrayVersionPhi are different
     */
    boolean has_diff_ver_match();

    /**
     * get the last block to change this AV
     * @return the last block to change this AV
     */
    int get_block();

    /**
     * getter returning whether array has been written to
     * @return true iff the array var has been written to
     */
    boolean has_been_written_to();

    /**
     * setter to set the has been written to flag
     */
    void toggle_written();
}
