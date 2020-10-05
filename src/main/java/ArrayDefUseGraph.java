import guru.nidi.graphviz.attribute.LinkAttr;
import guru.nidi.graphviz.attribute.Style;
import guru.nidi.graphviz.model.MutableGraph;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.tinylog.Logger;
import soot.ValueBox;
import soot.jimple.AssignStmt;
import soot.jimple.IfStmt;
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
        if(nodes.containsKey(node.get_id())) {
            Logger.error("We have an ID conflict: " + node.get_id());
            Logger.error(node.get_stmt().toString());
            Logger.error(nodes.get(node.get_id()).get_stmt().toString());
            System.exit(0);
        }
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
                        String id = Node.make_id(node.get_basename(), tmp_av, DefOrUse.DEF,
                                node.is_if(), node.get_line_num());
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
        Node n = nodes.get(use_node.get_opposite_id());
        if(Utils.not_null(n)) {
            Node def_node = new Node(n);
            if (use_node.get_index().equals(def_node.get_index()) || def_node.is_phi()) {
                // TODO: this index check is wrong.... indexes could be the same!
                if (def_node.is_phi()) {
                    Logger.info("Adding edge def node is a phi node");
                } else {
                    Logger.info("Adding edge, indexes match.");
                }
                def_node.set_is_used_in_edge(true);
                use_node.set_is_used_in_edge(true);
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
        } else {
            Logger.info("Use Node node found in nodes, is an if (probs): " + use_node.is_if());
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
     * @param was_if_stmt true iff old node was an if statment
     * @param blf construct for finding shimple line numbers
     */
    void array_def_rename(String old_name, ArrayVersion old_av,
                          String new_name, ArrayVersion new_av, AssignStmt new_stmt, boolean base_def, boolean was_if_stmt, BodyLineFinder blf) {
        String id = Node.make_id(old_name, old_av, DefOrUse.DEF, was_if_stmt, new_av.get_line_num());
        assert nodes.containsKey(id) : "the id of the old node must be a key in nodes.";
        Node n = nodes.remove(id);
        String new_id = Node.make_id(new_name, new_av, DefOrUse.DEF, new_stmt instanceof IfStmt, blf.get_line(new_stmt));
        Node new_node = new Node(new_stmt, new_name, new_av, n.get_index(), DefOrUse.DEF, base_def, blf.get_line(new_stmt));
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
     * @param phi_vars a container containing all phi_variables that have been seen up to this point
     *                 (along with the aliases of those PhiVariables
     * @param constants the constants
     *
     */
    void make_graph(PhiVariableContainer phi_vars, Map<String, Integer> constants) {
        for (Map.Entry<Integer, Edge> entry : edges.entrySet()) {
            Edge e = entry.getValue();
            Node def = e.get_def();
            Node use = e.get_use();
            guru.nidi.graphviz.model.Node def_node = node(def.get_aug_stmt_str());
            guru.nidi.graphviz.model.Node use_node = node(use.get_aug_stmt_str());
            array_def_use_graph.add(def_node.link(to(use_node).with(Style.ROUNDED, LinkAttr.weight(Constants.GRAPHVIZ_EDGE_WEIGHT))));
        }
        List<Node> non_edge_nodes = nodes.values().stream().filter(el -> !el.get_is_used_in_edge()).collect(Collectors.toList());
        for(Node n : non_edge_nodes) {
            guru.nidi.graphviz.model.Node node = node(n.get_aug_stmt_str());
            if(n.get_stmt_type() == NodeStmtType.IF_STMT) {
                IfStmt ifStmt = (IfStmt)n.get_stmt();
                Stmt target = ifStmt.getTarget();
                List<Node> possible_links = nodes.values().stream()
                        .filter(el -> !Objects.equals(el.get_stmt_type(),NodeStmtType.PHI_STMT))
                        .collect(Collectors.toList());
                for(Node n1 : possible_links) {
                    if(Objects.equals(n1.get_stmt().toString(), target.toString())) {
                        Logger.info("MAKE AN EDGE HERE!!!");
                    } else {
                        Logger.info(String.format("NO EDGE: '%s' != '%s'",
                                n1.get_stmt().toString(), target.toString()));
                    }
                }
                ValueBox cond_exp_box = ifStmt.getConditionBox();
                List<ValueBox> vals = cond_exp_box.getValue().getUseBoxes();
                for(ValueBox vb : vals) {
                    String name = vb.getValue().toString();
                    if(!NumberUtils.isCreatable(name)) {
                        ImmutablePair<Variable, List<AssignStmt>> dep_chain = phi_vars.get_var_dep_chain(constants, name);
                        Solver s = new Solver(target, name, dep_chain, phi_vars, constants);
                        for(AssignStmt as : dep_chain.getRight()) {
                            if(as.containsArrayRef()) {
                                Logger.info("IF STMT: here is the assignment: " + as.toString());
                                String basename = as.getArrayRef().getBaseBox().getValue().toString();
                                ValueBox index_box = as.getArrayRef().getIndexBox();
                            }
                        }
                    }
                }
                array_def_use_graph.add(node);
            }

        }
        Utils.print_graph(array_def_use_graph, Constants.EMPTY_DEF_USE);
    }

    /**
     * print the dep chiains for the rhs of node statements
     * @param pvc the phi variable containter to build the dep chians
     * @param constants the constants map
     */
    void print_def_node_dep_chains(PhiVariableContainer pvc, Map<String, Integer> constants) {
        for(Map.Entry<String, Node> entry : nodes.entrySet()) {
            Node n = entry.getValue();
            Stmt stmt = n.get_stmt();
            if(stmt instanceof AssignStmt && n.get_type() == DefOrUse.DEF) {
                Logger.debug("Printing deps for node: " + n.get_aug_stmt_str());
                List<String> uses = n.get_stmt().getUseBoxes().stream().map(el -> el.getValue().toString()).collect(Collectors.toList());
                for(String s : uses) {
                    pvc.print_var_dep_chain(constants, s);
                }
            } else {
                Logger.debug("Statment is not an assignment or is a use: " + n.get_aug_stmt_str());
            }
        }
    }
}
