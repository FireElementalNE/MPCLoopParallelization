/**
 * Enum to determine the type of statement in a node.
 */
public enum NodeStmtType {
    IF_STMT("IF_STMT"),
    ASSIGNMENT_STMT("ASSIGNMENT_STMT"),
    PHI_STMT("PHI_STMT");

    private final String text;

    /**
     * @param text convert to string
     */
    NodeStmtType(final String text) {
        this.text = text;
    }
    @Override
    public String toString() {
        return text;
    }
}
