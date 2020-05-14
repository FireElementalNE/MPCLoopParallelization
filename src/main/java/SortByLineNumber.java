import java.util.Comparator;

/**
 * sorting class for SCC Node
 */
class SortByLineNumber implements Comparator<SCCNode> {
    /**
     * sorting method
     * @param o1 an SCCNode
     * @param o2 an SCCNode
     * @return the sorting information (based on linenumber)
     */
    @Override
    public int compare(SCCNode o1, SCCNode o2) {
        return o1.get_line_num() - o2.get_line_num();
    }
}