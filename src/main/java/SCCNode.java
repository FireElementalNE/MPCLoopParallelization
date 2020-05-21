import soot.jimple.Stmt;

/**
 * Node for an SCCGraph
 */
@SuppressWarnings("unused")
class SCCNode {
    /**
     * the statement the node represents
     */
    private final Stmt stmt;
    /**
     * the index of the array reference
     */
    private final ArrayIndex index;
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
    SCCNode(Stmt stmt, String basename, ArrayIndex index, ReadWrite rw, int line_num) {
        this.stmt = stmt;
        this.basename = basename;
        this.index = index;
        this.rw = rw;
        this.line_num = line_num;
    }


    /**
     * copy constructor for SCC node
     * @param n the other SCCNode
     */
    SCCNode(SCCNode n) {
        this.stmt = n.stmt;
        this.basename = n.basename;
        this.index = new ArrayIndex(n.index);
        this.rw = n.rw;
        this.line_num = n.line_num;
    }

    /**
     * getter for the statement
     * @return the statement
     */
    Stmt get_stmt() {
        return stmt;
    }

    /**
     * getter for the index
     * @return the index
     */
    ArrayIndex get_index() {
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
