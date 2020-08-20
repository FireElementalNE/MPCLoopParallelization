import soot.jimple.IfStmt;

/**
 * class representation of a MUX gate
 */
class MUXNode {
    /**
     * if statement that is transformed into MUX node
     */
    private final IfStmt ifStmt;

    /**
     * Constructor
     * @param ifStmt the equivalent if statement
     */
    MUXNode(IfStmt ifStmt) {
        this.ifStmt = ifStmt;
    }

    /**
     * getter for ifstmt
     * @return the ifstmt
     */
    IfStmt get_if_stmt() {
        return ifStmt;
    }
}
