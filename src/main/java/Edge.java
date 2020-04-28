/**
 * Very simple class that represents an edge in an ArrayDefUseGraph
 */
@SuppressWarnings("FieldMayBeFinal")
class Edge {

    private Node def;
    private Node use;
    private boolean scc_edge;

    /**
     * constructor for Edge
     * @param node1 the def node
     * @param node2 the use node
     * @param scc_edge true iff we are making an scc edge.
     */
    Edge(Node node1, Node node2, boolean scc_edge) {
        this.def = node1;
        this.use = node2;
        this.scc_edge = scc_edge;
    }

    /**
     * getter for the def node
     * @return the def node
     */
    Node get_def() {
        return def;
    }

    /**
     * getter for the use node
     * @return the use node
     */
    Node get_use() {
        return use;
    }

    /**
     * get scc edge flag
     * @return true iff we are dealing with an scc edge
     */
    boolean is_scc_edge() {
        return scc_edge;
    }

    /**
     * hashing function so we can use a map in ArrayDefUseGraph
     * @return a semi-unique hash (I am sure this is not very good)
     */
    @Override
    public int hashCode() {
        return System.identityHashCode(new DefUseHash(def.get_stmt(), use.get_stmt()));
    }
}
