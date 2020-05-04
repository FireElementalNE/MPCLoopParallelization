import guru.nidi.graphviz.attribute.LinkAttr;
import guru.nidi.graphviz.attribute.Style;
import guru.nidi.graphviz.model.MutableGraph;
import org.tinylog.Logger;
import soot.jimple.Stmt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static guru.nidi.graphviz.model.Factory.*;

/**
 * A def/use graph for arrays (turning into an SCC graph eventually)
 */
@SuppressWarnings("ALL")
class ArrayDefUseGraph {
    /**
     * a list of all edges in the graph
     */
    private Map<Integer, Edge> edges;
    /**
     * a list of all nodes in the graph
     */
    private Map<String, Node> nodes;
    /**
     * the graph that is made for the inter loop dependencies
     */
    private MutableGraph array_def_use_graph;

    /**
     * constructor for ArrayDefUseGraph
     * @param class_name the name of the class being analyzed
     */
    ArrayDefUseGraph(String class_name) {
        edges = new HashMap<>();
        nodes = new HashMap<>();
        array_def_use_graph = mutGraph(class_name + "_array_def_use_graph").setDirected(true);
    }

    /**
     * copy constructor for ArrayDefUseGraph
     * @param a_graph the ArrayDefUseGraph being copied
     */
    ArrayDefUseGraph(ArrayDefUseGraph a_graph) {
        this.edges =  a_graph.edges.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        this.nodes =  a_graph.nodes.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        this.array_def_use_graph = a_graph.array_def_use_graph;
    }

    /**
     * add a node (and possibly an edge) to the graph
     * @param node the node
     * @param is_def true iff the node is a def node
     * @param is_phi true iff it is a phi node
     */
    void add_node(Node node, boolean is_def, boolean is_phi) {
        if (!is_def) {
            nodes.put(node.get_id(), node);
            add_edge(node);
        } else {
            if(is_phi) {
                ArrayVersion av = node.get_av();
                if(av instanceof ArrayVersionPhi) {
                    ArrayVersionPhi avp = (ArrayVersionPhi)av;
                    List<ArrayVersion> av_lst = avp.get_array_versions();
                    for(ArrayVersion tmp_av : av_lst) {
                        String id = Node.make_id(node.get_basename(), tmp_av, DefOrUse.DEF);
                        Node use_node = nodes.get(id);
                        Edge edge = new Edge(use_node, node);
                        edges.put(edge.hashCode(), edge);
                    }
                } else {
                    Logger.error("This should never be called on an ArrayVersionSingle");
                    System.exit(0);
                }
             }
            nodes.put(node.get_id(), node);
        }
    }


    /**
     * possibly add an edge to the graph
     * this is only called as nodes are added via the add_node() function
     * @param use_node the node being added
     */
    private void add_edge(Node use_node) {
        Node def_node = new Node(nodes.get(use_node.get_opposite_id()));
        if (use_node.get_index().equals(def_node.get_index()) || def_node.is_phi()) {
            // TODO: this index check is wrong.... indexes could be the same!
            if (def_node.is_phi()) {
                Logger.info("Adding edge def node is a phi node");
            } else {
                Logger.info("Adding edge, indexes match.");
            }
            Edge edge = new Edge(nodes.get(use_node.get_opposite_id()), use_node);
            edges.put(edge.hashCode(), edge);
        } else {
            // TODO: fix if the indexes _are_ the same but just renamed....
            //      Test12 shimple:
            //        i19 = i16_1
            //        ...
            //        r0[i19] = $i2
            //        ...
            //        $i4 = r0[i16_1]
            //  That should be an edge.
            Logger.info("Not adding edge, indexes mismatch and def node is not a phi node. ");
            Logger.debug("\tdef_index: " + def_node.get_index().to_str());
            Logger.debug("\tuse_index: " + use_node.get_index().to_str());
            Logger.debug("\tis phi?: " + def_node.is_phi());
        }
    }


    /**
     * rename an array definition based on a new alias
     * @param old_name the old name
     * @param old_av the old ArrayVersion
     * @param new_name the new name
     * @param new_av the new Array Version
     * @param new_stmt the new definition Statement
     * @param base_def the base def of the old node
     */
    void array_def_rename(String old_name, ArrayVersion old_av,
                          String new_name, ArrayVersion new_av, Stmt new_stmt, boolean base_def) {
        String id = Node.make_id(old_name, old_av, DefOrUse.DEF);
        assert nodes.containsKey(id) : "the id of the old node must be a key in nodes.";
        Node n = nodes.remove(id);
        String new_id = Node.make_id(new_name, new_av, DefOrUse.DEF);
        Node new_node = new Node(new_stmt.toString(), new_name, new_av, n.get_index(), DefOrUse.DEF, base_def);
        nodes.put(new_id, new_node);
    }

    /**
     * getter for all edges
     * @return all edges in the graph
     */
    Map<Integer, Edge> get_edges() {
        return edges;
    }

    /**
     * getter for all nodes
     * @return all nodes in the graph
     */
    Map<String, Node> get_nodes() {
        return nodes;
    }

    /**
     * Make a pretty array def use graph
     */
    void make_graph() {
        for (Map.Entry<Integer, Edge> entry : edges.entrySet()) {
            Edge e = entry.getValue();
            Node def = e.get_def();
            Node use = e.get_use();
            guru.nidi.graphviz.model.Node def_node = node(def.get_aug_stmt());
            guru.nidi.graphviz.model.Node use_node = node(use.get_aug_stmt());
            array_def_use_graph.add(def_node.link(to(use_node).with(Style.ROUNDED, LinkAttr.weight(Constants.GRAPHVIZ_EDGE_WEIGHT))));
        }
        Utils.print_graph(array_def_use_graph, Constants.EMPTY_DEF_USE);
    }

}
