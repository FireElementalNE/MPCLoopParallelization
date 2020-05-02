/**
 * Very simple class that represents an edge in an ArrayDefUseGraph
 */
class Edge {
    /**
     * the def node
     */
    private final Node def;
    /**
     * the use node
     */
    private final Node use;
    /**
     * true iff this edge is an scc edge
     */
//    private final boolean scc_edge;

    /**
     * constructor for Edge
     * @param node1 the def node
     * @param node2 the use node
//     * @param scc_edge true iff this is an scc edge.
     */
    Edge(Node node1, Node node2) {
        this.def = node1;
        this.use = node2;
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
//    boolean is_scc_edge() {
//        return scc_edge;
//    }

    /**
     * hashing function so we can use a map in ArrayDefUseGraph
     * @return a semi-unique hash (I am sure this is not very good)
     */
    @Override
    public int hashCode() {
        return System.identityHashCode(new DefUseHash(def.get_aug_stmt(), use.get_aug_stmt()));
    }
}
