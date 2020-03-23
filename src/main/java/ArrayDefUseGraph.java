import org.tinylog.Logger;
import soot.jimple.Stmt;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

class ArrayDefUseGraph {
    private Map<Integer, Edge> edges;
    private Map<String, Node> nodes;


    ArrayDefUseGraph() {
        edges = new HashMap<>();
        nodes = new HashMap<>();
    }

    ArrayDefUseGraph(ArrayDefUseGraph a_graph) {
        this.edges =  a_graph.edges.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        this.nodes =  a_graph.nodes.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    void add_node(Node node, boolean is_def) {
        nodes.put(node.get_id(), node);
        if(!is_def) {
            add_edge(node);
        }
    }

    private void add_edge(Node use_node) {
        assert nodes.containsKey(use_node.get_opposite_id());
        Node def_node = new Node(nodes.get(use_node.get_opposite_id()));
        if(use_node.get_index().equals(def_node.get_index()) || def_node.is_phi()) {
            if(def_node.is_phi()) {
                Logger.info("Adding edge def node is a phi node");
            } else {
                Logger.info("Adding edge, indexes match.");
            }
            Edge edge = new Edge(nodes.get(use_node.get_opposite_id()), use_node);
            edges.put(edge.hashCode(), edge);
        } else {
            Logger.info("Not adding edge, indexes mismatch and def node is not a phi node. ");
            Logger.debug("\tdef_index: " + def_node.get_index().to_str());
            Logger.debug("\tuse_index: " + use_node.get_index().to_str());
            Logger.debug("\tis phi?: " + def_node.is_phi());
        }
    }

    void array_def_rename(String old_name, ArrayVersion old_av, String new_name, ArrayVersion new_av, Stmt new_stmt) {
        String id = Node.make_id(old_name, old_av, DefOrUse.DEF);
        assert nodes.containsKey(id);
        Node n = nodes.remove(id);
        String new_id = Node.make_id(new_name, new_av, DefOrUse.DEF);
        Node new_node = new Node(new_stmt.toString(), new_name, new_av, n.get_index(), DefOrUse.DEF);
        nodes.put(new_id, new_node);
    }

    Map<Integer, Edge> get_edges() {
        return edges;
    }
    Map<String, Node> get_nodes() {
        return nodes;
    }
}
