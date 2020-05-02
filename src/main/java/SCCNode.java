@SuppressWarnings("unused")
public class SCCNode {
    /**
     * the statement the node represents
     */
    private final String stmt;
    /**
     * the index of the array reference
     */
    private final Index index;
    /**
     * the basename of the array reference
     */
    private final String basename;

    /**
     * the SCC node constructor
     * @param stmt the statement
     * @param basename the base array name
     * @param index the index
     */
    SCCNode(String stmt, String basename, Index index) {
        this.stmt = stmt;
        this.basename = basename;
        this.index = index;
    }

    /**
     * getter for the statement
     * @return the statement
     */
    public String get_stmt() {
        return stmt;
    }

    /**
     * getter for the index
     * @return the index
     */
    public Index get_index() {
        return index;
    }

    /**
     * getter for the basename (unused)
     * @return the basename
     */
    public String get_basename() {
        return basename;
    }
}
