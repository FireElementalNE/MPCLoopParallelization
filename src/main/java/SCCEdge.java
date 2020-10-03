import org.apache.commons.lang3.tuple.ImmutablePair;

/**
 * class representing an SCCEdge
 */
class SCCEdge {
    private SCCNode src;
    private SCCNode dest;
    private int d;

    /**
     * constructor
     * @param src the source node
     * @param dest the destination node
     * @param d the d value
     */
    SCCEdge(SCCNode src, SCCNode dest, int d) {
        this.src = src;
        this.dest = dest;
        this.d = d;
    }

    /**
     * getter for src node
     * @return the src node
     */
    SCCNode get_src() {
        return src;
    }

    /**
     * getter for the dest node
     * @return the dest node
     */
    SCCNode get_dest() {
        return dest;
    }

    /**
     * getter for the d value
     * @return the d value
     */
    int get_d() {
        return d;
    }

    /**
     * get an id pair for the edge
     * @return the id pair for the edge
     */
    ImmutablePair<String, String> get_id_pair() {
        return new ImmutablePair<>(src.get_stmt().toString(), dest.get_stmt().toString());
    }


}
