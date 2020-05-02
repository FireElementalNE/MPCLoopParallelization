import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.attribute.LinkAttr;
import guru.nidi.graphviz.attribute.Style;
import guru.nidi.graphviz.model.MutableGraph;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.tinylog.Logger;
import soot.jimple.AssignStmt;
import soot.jimple.Stmt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private MutableGraph inter_loop_dep_graph;
    /**
     * the SCC graph
     */
    private MutableGraph SCC_graph;

    /**
     * constructor for ArrayDefUseGraph
     * @param class_name the name of the class being analyzed
     */
    ArrayDefUseGraph(String class_name) {
        edges = new HashMap<>();
        nodes = new HashMap<>();
        inter_loop_dep_graph = mutGraph(class_name + "_inter_loop_deps").setDirected(true);
        SCC_graph = mutGraph(class_name + "_scc_final").setDirected(true);
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
        this.inter_loop_dep_graph = a_graph.inter_loop_dep_graph;
        this.SCC_graph = a_graph.SCC_graph;
    }

    /**
     * add a node (and possibly an edge) to the graph
     * @param node the node
     * @param is_def true iff the node is a def node
     */
    void add_node(Node node, boolean is_def) {
        if (!is_def) {
            nodes.put(node.get_id(), node);
            add_edge(node);
        } else {
            nodes.put(node.get_id(), node);
        }

    }

    /**
     * possibly add an edge to the graph
     * this is only called as nodes are added via the add_node() function
     * @param use_node the node being added
     */
    private void add_edge(Node use_node) {
        assert nodes.containsKey(use_node.get_opposite_id()) : "the def_node id  must be a key in nodes.";
        Node def_node = new Node(nodes.get(use_node.get_opposite_id()));
        if (use_node.get_index().equals(def_node.get_index()) || def_node.is_phi()) {
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
        String id = Node.make_id(old_name, old_av, DefOrUse.DEF, new_stmt.getJavaSourceStartLineNumber());
        assert nodes.containsKey(id) : "the id of the old node must be a key in nodes.";
        Node n = nodes.remove(id);
        String new_id = Node.make_id(new_name, new_av, DefOrUse.DEF, new_stmt.getJavaSourceStartLineNumber());
        Node new_node = new Node(new_stmt.toString(), new_name, new_av, n.get_index(), DefOrUse.DEF,
                new_stmt.getJavaSourceStartLineNumber(), base_def);
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
     * Make a pretty inter loop dependency graph
     */
    void make_graph() {
        if(edges.size() > 0) {
            for (Map.Entry<Integer, Edge> entry : edges.entrySet()) {
                Edge e = entry.getValue();
                Node def = e.get_def();
                Node use = e.get_use();
                guru.nidi.graphviz.model.Node def_node = node(def.get_stmt());
                guru.nidi.graphviz.model.Node use_node = node(use.get_stmt());
                inter_loop_dep_graph.add(def_node.link(to(use_node).with(Style.ROUNDED, LinkAttr.weight(Constants.GRAPHVIZ_EDGE_WEIGHT))));
            }
        } else {
            // TODO: this is added all the time??? why???
            inter_loop_dep_graph.add(Constants.NO_INTER_LOOP_DEPS_NODE);
        }
        Utils.print_graph(inter_loop_dep_graph);
    }

    void make_scc_graph(PhiVariableContainer pvc, ArrayVariables array_vars, Map<String, Integer> constants) {
        Set<PhiVariable> phi_variables = pvc.get_phi_vars();
        for(Map.Entry<String, Node> entry : nodes.entrySet()) {
            Node use = entry.getValue();
            if (use.get_type() == DefOrUse.USE) {
                String use_index = use.get_index().to_str();
                Logger.info("USE INDEX DEP CHAIN: ");
                ImmutablePair<Variable, List<AssignStmt>> use_dep_chain = pvc.get_var_dep_chain(constants, use_index);
                Solver use_solver = new Solver(use_index, use_dep_chain, pvc, constants);
                String use_eq = use_solver.get_resolved_eq();
                pvc.print_var_dep_chain(constants, use.get_index().to_str());
                if (use_eq.contains("=")) {
                    use_eq = use_eq.split(" = ")[1];
                }
                Map<String, Integer> d_vals = use_solver.solve();
                Logger.info(use_eq);
                for (PhiVariable pv : phi_variables) {
                    String def = pv.get_phi_def().toString();
                    if (use_eq.contains(def)) {
                        int d = 0;
                        if(!d_vals.isEmpty()) {
                            d = d_vals.get(def);
                        }
                        guru.nidi.graphviz.model.Node def_node = node(pv.toString());
                        guru.nidi.graphviz.model.Node use_node = node(use.get_stmt().replace(use.get_index().to_str(), use_eq));
                        SCC_graph.add(def_node.link(to(use_node).with(
                                Style.SOLID,
                                LinkAttr.weight(Constants.GRAPHVIZ_EDGE_WEIGHT))));
                        SCC_graph.add(use_node.link(to(def_node).with(
                                Style.DASHED,
                                LinkAttr.weight(Constants.GRAPHVIZ_EDGE_WEIGHT),
                                Label.of("d = " + d))));
                    }
                }
            }
        }
        Utils.print_graph(SCC_graph);
    }
}
