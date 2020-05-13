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
     * Enum to tell if this is a read or a write node (matters when creating edges)
     */
    private final ReadWrite rw;
    /**
     * the line number of the statement
     */
    private final int line_num;

    /**
     * the SCC node constructor
     * @param stmt the statement
     * @param basename the base array name
     * @param index the index
     * @param rw enum for a read or a write
     * @param line_num the line number of the statement
     */
    SCCNode(String stmt, String basename, Index index, ReadWrite rw, int line_num) {
        this.stmt = stmt;
        this.basename = basename;
        this.index = index;
        this.rw = rw;
        this.line_num = line_num;
    }

    /**
     * getter for the statement
     * @return the statement
     */
    String get_stmt() {
        return stmt;
    }

    /**
     * getter for the index
     * @return the index
     */
    Index get_index() {
        return index;
    }

    /**
     * getter for the basename (unused)
     * @return the basename
     */
    String get_basename() {
        return basename;
    }

    /**
     * getter for read write enum
     * @return read write enum
     */
    ReadWrite get_rw() {
        return rw;
    }

    /**
     * getter for the line number of the statement
     * @return the line number of the statement
     */
    int get_line_num() {
        return line_num;
    }
}
