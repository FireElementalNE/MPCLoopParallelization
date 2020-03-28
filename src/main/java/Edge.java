class Edge {
    // Very simple class that represents an edge in an ArrayDefUseGraph

    private Node def;
    private Node use;

    /**
     * constructor for Edge
     * @param node1 the def node
     * @param node2 the use node
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
     * hashing function so we can use a map in ArrayDefUseGraph
     * @return a semi-unique hash (I am sure this is not very good)
     */
    @Override
    public int hashCode() {
        return System.identityHashCode(new DefUseHash(def.get_stmt(), use.get_stmt()));
    }
}
