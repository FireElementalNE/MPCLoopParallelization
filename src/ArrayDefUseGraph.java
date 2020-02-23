import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ArrayDefUseGraph {
    private Map<Integer, Edge> edges;
    private List<Node> nodes;

    ArrayDefUseGraph() {
        edges = new HashMap<>();
        nodes = new ArrayList<>();
    }


    void add_edge(Edge edge) {
        edges.put(edge.hashCode(), edge);
        if(!nodes.contains(edge.get_def())) {
            nodes.add(edge.get_def());
        }
        if(!nodes.contains(edge.get_use())) {
            nodes.add(edge.get_use());
        }
    }

    Map<Integer, Edge> get_edges() {
        return edges;
    }

    List<Node> get_nodes() {
        return nodes;
    }
}
