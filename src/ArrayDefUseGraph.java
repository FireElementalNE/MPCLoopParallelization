import java.util.*;

class ArrayDefUseGraph {
    private Map<Integer, Edge> edges;
    private Map<String, Node> nodes;


    ArrayDefUseGraph() {
        edges = new HashMap<>();
        nodes = new HashMap<>();
    }

    void add_node(Node node, boolean is_def) {
        nodes.put(node.get_id(), node);
        if(!is_def) {
            add_edge(node);
        }
    }
    @SuppressWarnings("WeakerAccess")
    void add_edge(Node use_node) {
        assert nodes.containsKey(Utils.use_to_def_id(use_node.get_id()));
        Edge edge = new Edge(nodes.get(Utils.use_to_def_id(use_node.get_id())), use_node);
        edges.put(edge.hashCode(), edge);

    }
    Map<Integer, Edge> get_edges() {
        return edges;
    }
    Map<String, Node> get_nodes() {
        return nodes;
    }
}
