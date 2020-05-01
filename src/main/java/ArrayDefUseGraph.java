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
import java.util.Objects;
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
     * @param links_to_prev_iter true iff we are adding an intra-loop dep
     */
    void add_node(Node node, boolean is_def, boolean links_to_prev_iter) {
        if (!is_def) {
            nodes.put(node.get_id(), node);
            add_edge(node, links_to_prev_iter);
        } else {
            nodes.put(node.get_id(), node);
        }

    }

    /**
     * possibly add an edge to the graph
     * this is only called as nodes are added via the add_node() function
     * @param use_node the node being added
     * @param links_to_prev_iter true iff we are adding an intra-loop dep
     */
    private void add_edge(Node use_node, boolean links_to_prev_iter) {
        if(links_to_prev_iter) {
            for(Map.Entry<String, Node> entry : nodes.entrySet()) {
                String name = entry.getKey();
                Node n = entry.getValue();
                if(n.get_type() == DefOrUse.DEF
                        && Objects.equals(use_node.get_basename(), n.get_basename())
                        && !n.is_base_def()) {
                    Edge edge = new Edge(new Node(n), use_node, true);
                    edges.put(edge.hashCode(), edge);
                }
            }
        } else {
            assert nodes.containsKey(use_node.get_opposite_id());
            Node def_node = new Node(nodes.get(use_node.get_opposite_id()));
            if (use_node.get_index().equals(def_node.get_index()) || def_node.is_phi()) {
                if (def_node.is_phi()) {
                    Logger.info("Adding edge def node is a phi node");
                } else {
                    Logger.info("Adding edge, indexes match.");
                }
                Edge edge = new Edge(nodes.get(use_node.get_opposite_id()), use_node, false);
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
        assert nodes.containsKey(id);
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
        Map<Integer, Edge> the_edges = edges.entrySet().stream().filter(k -> !k.getValue().is_scc_edge()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if(the_edges.size() > 0) {
            for (Map.Entry<Integer, Edge> entry : the_edges.entrySet()) {
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

    void make_scc_graph(PhiVariableContainer pvc, Map<String, Integer> constants) {
        // TODO: add weights
        Map<Integer, Edge> the_edges = edges.entrySet().stream().filter(k -> k.getValue().is_scc_edge()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        for(Map.Entry<Integer, Edge> entry: the_edges.entrySet()) {
            Edge e = entry.getValue();
            Node def = e.get_def();
            Node use = e.get_use();
            String def_index = def.get_index().to_str();
            String use_index = use.get_index().to_str();
            Logger.info("DEF INDEX DEP CHAIN: ");
            // pvc.print_var_dep_chain(constants, def.get_index().to_str());
            ImmutablePair<Variable, List<AssignStmt>> def_dep_chain = pvc.get_var_dep_chain(constants, def_index);
            String def_eq = null;
            if(Utils.not_null(def_dep_chain)) {
                Solver def_solver = new Solver(def_index, def_dep_chain, pvc, constants);
                def_eq = def_solver.get_resolved_eq();
                if (def_eq.contains("=")) {
                    def_eq = def_eq.split(" = ")[1];
                }
            } else {
                def_eq = "CREATED_ARRAY_SSA_PHI_NODE";
            }
            Logger.info(def_eq);
            Logger.info("USE INDEX DEP CHAIN: ");
            ImmutablePair<Variable, List<AssignStmt>> use_dep_chain = pvc.get_var_dep_chain(constants, use_index);
            Solver use_solver = new Solver(use_index, use_dep_chain, pvc, constants);
            String use_eq = use_solver.get_resolved_eq();
            if(use_eq.contains("=")) {
                use_eq = use_eq.split(" = ")[1];
            }
            Logger.info(use_eq);
            pvc.print_var_dep_chain(constants, use.get_index().to_str());
//            String def_stmt, use_stmt;
//            if(def.is_aug()) {
//                def_stmt = def.get
//            }


            guru.nidi.graphviz.model.Node def_node = node(def.get_stmt().replace(def.get_index().to_str(), def_eq));
            guru.nidi.graphviz.model.Node use_node = node(use.get_stmt().replace(use.get_index().to_str(), use_eq));
            int d = use_solver.solve();
            SCC_graph.add(def_node.link(to(use_node).with(
                    Style.SOLID,
                    LinkAttr.weight(Constants.GRAPHVIZ_EDGE_WEIGHT))));
            SCC_graph.add(use_node.link(to(def_node).with(
                    Style.DASHED,
                    LinkAttr.weight(Constants.GRAPHVIZ_EDGE_WEIGHT),
                    Label.of("d = " + d))));
        }
        Utils.print_graph(SCC_graph);
    }
}
