import soot.jimple.Stmt;

import java.util.Map;

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
     * @param stmt the statement that changed the versioning
     */
    void incr_version(int block, Stmt stmt);

    /**
     * force a version increase (useful for merges to avoid id conflicts)
     */
    void force_incr_version();

//    /**
//     * TODO: this is not longer needed
//     * check if the variables in an ArrayVersionPhi have the same names
//     * @return true iff the variables in an ArrayVersionPhi have the same name, false if
//     *         called on an ArrayVersionSingle, or if the names in an ArrayVersionPhi are different
//     */
//    boolean has_diff_ver_match();

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
     * flag to tell whether an array has been read
     * @return true iff the array has been read
     */
    boolean has_been_read();

    /**
     * setter to set the has been written to flag
     */
    void toggle_written();

    /**
     * setter to set the read flag
     */
    void toggle_read();

    /**
     * get the versions map
     * @return the versions map
     */
    Map<Integer, Stmt> get_versions();

    /**
     * get the line number of the array version change
     * @return the line number
     */
    int get_line_num();

//    /**
//     * non overridden equals op
//     * @param av the other array version
//     * @return true iff they are "equal"
//     */
//    boolean equals(ArrayVersion av);
}
