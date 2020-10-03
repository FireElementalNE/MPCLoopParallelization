import guru.nidi.graphviz.attribute.*;
import guru.nidi.graphviz.model.MutableGraph;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.tinylog.Logger;
import soot.jimple.AssignStmt;

import java.util.*;

import static guru.nidi.graphviz.model.Factory.*;

/**
 * class to create the SCC graph
 */
public class SCCGraph {
    /* TODO: BIG !
        So the SCC stuff is not WRONG but it is not correct. An SCC is when we have something like Test10. A loop carried dependency
        as well as a regular dependency, this creates a _cycle_. That is the def of SCC.
        Most of the examples (like test 12 and test 19) are just directly vectorize-able.
        Think of this: A[i] = A[i - 1] + 3. That is a true SCC because it has a cycle (and since the read is before the write,
        d > 0; If the write is before the read d >=0)
        LOOK at test10! and look at the graph! The -1 parts should be back edges. Also for TRUE SCC the negative d values are OK
        but for the vectorize-able ones they are NOT. In those cases (like test19 and test12) the indexes never coincide and do not
        form an SCC (again test19). So the d value is being represented two different ways. Fix this by labeling the edges?
     */


    /**
     * the SCC graph
     */
    private final MutableGraph SCC_graph;
    /**
     * a list of SCC nodes
     */
    Set<SCCNode> nodes;
    /**
     * a list of SCC edges
     */
    Set<SCCEdge> edges;

    /**
     * the base constructor for the SCC graph
     * @param class_name the name of the class being analyzed
     */
    SCCGraph(String class_name) {
        this.SCC_graph = mutGraph(class_name + "_scc_final").setDirected(true);
        this.nodes = new HashSet<>();
        this.edges = new HashSet<>();
    }

    /**
     * Copy constructor for the SCC graph
     * @param sccg the other SCC graph
     */
    SCCGraph(SCCGraph sccg) {
        this.SCC_graph = sccg.SCC_graph;
        this.nodes = new HashSet<>(sccg.nodes);
        this.edges = new HashSet<>(sccg.edges);
    }

    /**
     * get SCC edges (should be run after make graph)
     * @return SCC edges
     */
    Set<SCCEdge> get_edges() {
        // TODO: check if we actually make graph
        return edges;
    }

    /**
     * add an SCC node to the graph
     * @param node the node to add
     */
    void add_node(SCCNode node) {
        this.nodes.add(node);
    }

    /**
     * get a chain (list) of scc components that are linked to the current node
     * @param node the current SCCNode
     * @return the chain sorted by line number
     */
    private List<SCCNode> get_scc_chain(SCCNode node) {
        List<SCCNode> scc_chain = new ArrayList<>();
        scc_chain.add(node);
        for(SCCNode n : nodes) {
            if(Objects.equals(n.get_basename(), node.get_basename())) {
                // TODO: this might be incorrect???
                if(node.get_rw() != n.get_rw()) {
                    scc_chain.add(n);
                }
            }
        }
        scc_chain.sort(new SortByLineNumber());
        return scc_chain;
    }

    /**
     * get a solver associated with an SCCNode
     * @param node the SCCNode
     * @param pvc the Phi Variable containter
     * @param constants the map of all constants
     * @return a solver for the SCCNode variable
     */
    private Solver get_solver(SCCNode node, PhiVariableContainer pvc, Map<String, Integer> constants) {
        String index = node.get_index().to_str();
        ImmutablePair<Variable, List<AssignStmt>> cur_dep_chain = pvc.get_var_dep_chain(constants, index);
        return new Solver(node.get_stmt(), index, cur_dep_chain, pvc, constants);
    }

    /**
     * make an SCC graph png
     * @param pvc the container class that holds all non array phi variables
     * @param constants A list of the _original_ phi variables that is queried on the second iteration
     * @param def_use_graph The final DefUse Graph
     * @param if_stmts container for if statements (used in def/use graph)
     * @param array_vars the map of array variables and versions
     */
    void make_scc_graph(PhiVariableContainer pvc, Map<String, Integer> constants,
                        ArrayDefUseGraph def_use_graph, IfStatementContainer if_stmts,
                        ArrayVariables array_vars) {

        List<ImmutablePair<String, String>> seen_deps = new ArrayList<>();

        for(SCCNode node : nodes) {
            Logger.info("we are at node '" + node.get_stmt().toString() + "' which is a " + node.get_rw().toString());
            List<SCCNode> ns = get_scc_chain(node);
            for(SCCNode n : ns) {
                Logger.info("     LINK: " + n.get_stmt().toString());
            }
        }

        List<List<SCCNode>> completed_chains = new ArrayList<>();
        for(SCCNode node : nodes) {
            List<SCCNode> scc_chain = get_scc_chain(node);
            if(!completed_chains.contains(scc_chain) && scc_chain.size() > 1) {
                SCCNode cur_node = scc_chain.get(0);
                for(int i = 1; i < scc_chain.size(); i++) {
                    List<ReadWrite> rw_check = new ArrayList<>(Arrays.asList(cur_node.get_rw(), scc_chain.get(i).get_rw()));
                    if (rw_check.contains(ReadWrite.READ) && rw_check.contains(ReadWrite.WRITE)) {
                        ReadWrite c_rw = cur_node.get_rw();
                        ReadWrite n_rw = scc_chain.get(i).get_rw();
                        int c_line = cur_node.get_line_num();
                        int n_line = scc_chain.get(i).get_line_num();
                        int can_be_cycle = 0;
                        if(c_line > n_line) {
                            Logger.debug("Current line is after n_line");
                            if(c_rw == ReadWrite.READ && n_rw == ReadWrite.WRITE) {
                                Logger.debug("This Cannot be a cycle. 1");
                                Logger.debug("\tc_stmt: " + cur_node.get_stmt().toString());
                                Logger.debug("\tn_stmt: " + scc_chain.get(i).get_stmt().toString());
                            } else if(c_rw == ReadWrite.WRITE && n_rw == ReadWrite.READ) {
                                Logger.debug("This might be a cycle. 1");
                                Logger.debug("\tc_stmt: " + cur_node.get_stmt().toString());
                                Logger.debug("\tn_stmt: " + scc_chain.get(i).get_stmt().toString());
                                can_be_cycle = 1;
                            } else {
                                Logger.warn("We should not get here! 1");
                            }
                        } else if( c_line < n_line) {
                            Logger.debug("Current line is before n_line");
                            if(c_rw == ReadWrite.READ && n_rw == ReadWrite.WRITE) {
                                Logger.debug("This might be a cycle. 2");
                                Logger.debug("\tc_stmt: " + cur_node.get_stmt().toString());
                                Logger.debug("\tn_stmt: " + scc_chain.get(i).get_stmt().toString());
                                can_be_cycle = 2;
                            } else if(c_rw == ReadWrite.WRITE && n_rw == ReadWrite.READ) {
                                Logger.debug("This Cannot be a cycle. 2");
                                Logger.debug("\tc_stmt: " + cur_node.get_stmt().toString());
                                Logger.debug("\tn_stmt: " + scc_chain.get(i).get_stmt().toString());
                            } else {
                                Logger.warn("We should not get here! 2");
                            }
                        } else {
                            Logger.debug("Current line is the same as n_line");
                            Logger.debug("This might be a cycle. 2");
                            Logger.debug("\tc_stmt: " + cur_node.get_stmt().toString());
                            Logger.debug("\tn_stmt: " + scc_chain.get(i).get_stmt().toString());
                            can_be_cycle = 3;
                            // TODO: need to look at the left hand side and the right hand side to tell
                            //       if there is a cycle. STILL confused about this????
                        }
                        Solver cur_solver = get_solver(cur_node, pvc, constants);
                        Solver n_solver = get_solver(scc_chain.get(i), pvc, constants);

                        String cur_eq = cur_solver.get_resolved_eq();
                        if (cur_eq.contains("=")) {
                            cur_eq = cur_eq.split(" = ")[1];
                        }
                        String n_eq = n_solver.get_resolved_eq();
                        if (n_eq.contains("=")) {
                            n_eq = n_eq.split(" = ")[1];
                        }

                        Map<String, Integer> cur_d_vals = cur_solver.solve();
                        Map<String, Integer> n_d_vals = n_solver.solve();
                        Map<String, Integer> d_vals = new HashMap<>();
                        Map<String, List<Integer>> indexes = new HashMap<>();
                        for (Map.Entry<String, Integer> c_entry : cur_d_vals.entrySet()) {
                            for (Map.Entry<String, Integer> n_entry : n_d_vals.entrySet()) {
                                if (Objects.equals(c_entry.getKey(), n_entry.getKey())) {
                                    indexes.put(c_entry.getKey(), new ArrayList<>());
                                    indexes.get(c_entry.getKey()).add(c_entry.getValue());
                                    indexes.get(c_entry.getKey()).add(n_entry.getValue());
                                    // TODO: write - read, it is not arbitrary!
                                    // TODO: this is still weird....
                                    d_vals.put(c_entry.getKey(), c_entry.getValue() + n_entry.getValue());
                                }
                            }
                        }
                        StringBuilder c_sb = new StringBuilder();
                        StringBuilder n_sb = new StringBuilder();
                        for(Map.Entry<String, List<Integer>> el : indexes.entrySet()) {
                            c_sb.append(el.getKey()).append(": ").append(el.getValue().get(0)).append("\n");
                            n_sb.append(el.getKey()).append(": ").append(el.getValue().get(1)).append("\n");
                        }

                        // TODO: ask ana about this: testing the constant rate assumption and the eq assumption.
                        //       if we have a bunch of statements do we need to FLATTEN the whole thing and THEN
                        //       pass it into a solver? if so this could be hard to do automatically!!!!
//
//                        Map<String, String> rep_strings = new HashMap<>();
//                        if(cur_node.get_stmt() instanceof AssignStmt) {
//                            AssignStmt as = (AssignStmt)cur_node.get_stmt();
//                            Logger.debug("LOOKING AT: " + as.toString());
//                            List<ValueBox> defs_uses = as.getUseAndDefBoxes();
//                            for(ValueBox vb : defs_uses) {
//                                Value v = vb.getValue();
//                                if(!(vb.getValue() instanceof ArrayRef) && !array_vars.contains_key(v.toString())) {
//                                    String v_name = v.toString();
//                                    ImmutablePair<Variable, List<AssignStmt>> dep_chain = pvc.get_var_dep_chain(constants, v_name);
//                                    String resolved_chain = Utils.resolve_dep_chain(v_name, dep_chain, constants);
//                                    Logger.debug("\tRESOLVED DEP CHAIN: " + resolved_chain);
//                                    String[] tmp_rep = resolved_chain.split(" = ");
//                                    if(tmp_rep.length == 1) {
//                                        rep_strings.put(v_name, tmp_rep[0]);
//                                    } else if(tmp_rep.length == 2) {
//                                        rep_strings.put(tmp_rep[0], tmp_rep[1]);
//                                    } else {
//                                        Logger.error("There is more than one equal sign in the equation, something is VERY wrong.");
//                                        System.exit(0);
//                                    }
//                                }
//                            }
//                        }
                        String s1 = Utils.get_aug_node_stmt(cur_node, def_use_graph, cur_eq);
                        String s2 = Utils.get_aug_node_stmt(scc_chain.get(i), def_use_graph, n_eq);


                        guru.nidi.graphviz.model.Node c_indexes = node(c_sb.toString()).with(Shape.RECTANGLE);
                        guru.nidi.graphviz.model.Node n_indexes = node(n_sb.toString()).with(Shape.RECTANGLE);
                        guru.nidi.graphviz.model.Node c_node = node(cur_node.get_line_num()
                                + ": " + Utils.get_aug_node_stmt(cur_node, def_use_graph, cur_eq));
                        guru.nidi.graphviz.model.Node n_node = node(scc_chain.get(i).get_line_num()
                                + ": " + Utils.get_aug_node_stmt(scc_chain.get(i), def_use_graph, n_eq));

                        // TODO: need to get ALL phi nodes that were declared in that block they ALL reference themselves
                        //       and all outer phi nodes
                        for (Map.Entry<String, Node> entry : def_use_graph.get_nodes().entrySet()) {
                            if(entry.getValue().is_phi()) {
                                String base_name = entry.getValue().get_basename();
                                ArrayVersionPhi avp = (ArrayVersionPhi)entry.getValue().get_av();
                                MUXNode mux_node = avp.get_mux_node();
                                guru.nidi.graphviz.model.Node cond_node = node(mux_node.get_conditional().toString()).with(Shape.RECTANGLE);
                                String phi_node_str = Utils.create_phi_stmt(base_name, avp);
                                guru.nidi.graphviz.model.Node phi_node = node(phi_node_str).with(Shape.RECTANGLE);
                                SCC_graph.add(phi_node.link(to(cond_node).with(
                                        Style.BOLD,
                                        LinkAttr.weight(Constants.GRAPHVIZ_EDGE_WEIGHT),
                                        Color.BLACK)));
                                List<ArrayVersion> a_versions = avp.get_array_versions();
                                for(ArrayVersion av : a_versions) {
                                    String av_str =  base_name + "_" + av.get_version();
                                    String aug_node_str = Utils.get_aug_node_stmt(cur_node, def_use_graph, cur_eq);
                                    if(aug_node_str.contains(av_str)) {
                                        SCC_graph.add(c_node.link(to(phi_node).with(
                                            Style.BOLD,
                                            LinkAttr.weight(Constants.GRAPHVIZ_EDGE_WEIGHT),
                                            Color.BLACK)));
                                    }
                                }
                            }
                        }
//                        Logger.info("Started new stuff");
//                        String cur_eq_cp =  Utils.get_aug_node_stmt(cur_node, def_use_graph, cur_eq);
//
//                        for(Map.Entry<String, String> entry : rep_strings.entrySet()) {
//                            cur_eq_cp = cur_eq_cp.replace(entry.getKey(), entry.getValue());
//                        }
//                        Logger.debug("CUR_EQ_FIXED: " + cur_eq_cp + "(" + cur_solver.get_resolved_eq() + ")");

                        SCC_graph.add(c_node.link(to(c_indexes).with(
                                Style.DOTTED,
                                LinkAttr.weight(Constants.GRAPHVIZ_EDGE_WEIGHT),
                                Color.GRAY)));

                        SCC_graph.add(n_node.link(to(n_indexes).with(
                                Style.DOTTED,
                                LinkAttr.weight(Constants.GRAPHVIZ_EDGE_WEIGHT),
                                Color.GRAY)));


                        if(can_be_cycle == 0) {
                            for (Map.Entry<String, Integer> entry : d_vals.entrySet()) {
                                edges.add(new SCCEdge(cur_node, scc_chain.get(i), entry.getValue()));

                                SCC_graph.add(c_node.link(to(n_node).with(
                                        Style.DASHED,
                                        LinkAttr.weight(Constants.GRAPHVIZ_EDGE_WEIGHT),
                                        Label.of("d = " + entry.getValue()),
                                        Color.ORANGE)));
                            }
                            // TODO: these can be dependency edges
                        } else {
//                            for(SCCEdge e : edges) {
//                                ImmutablePair<String, String> im = e.get_id_pair();
//                                if(Objects.equals(im.getRight(), t.getRight()) &&
//                                        Objects.equals(im.getLeft(), t.getLeft())) {
//                                    Logger.error("we are adding again????");
////                                        System.exit(0);
//                                }
//
//                            }
                            ImmutablePair<String, String> t = new ImmutablePair(cur_node.get_stmt().toString(),
                                    scc_chain.get(i).get_stmt().toString());
                            if(!seen_deps.contains(t)) {
                                for (Map.Entry<String, Integer> entry : d_vals.entrySet()) {
                                    SCCEdge new_edge = new SCCEdge(scc_chain.get(i), cur_node, entry.getValue());
                                    edges.add(new_edge);
                                    seen_deps.add(new_edge.get_id_pair());

                                    if(can_be_cycle == 1) {
                                        SCC_graph.add(n_node.link(to(c_node).with(
                                                Style.DASHED,
                                                LinkAttr.weight(Constants.GRAPHVIZ_EDGE_WEIGHT),
                                                Label.of("d = " + entry.getValue()),
                                                Color.BLUE)));
                                        SCC_graph.add(c_node.link(to(n_node).with(
                                                Style.SOLID,
                                                LinkAttr.weight(Constants.GRAPHVIZ_EDGE_WEIGHT),
                                                Color.GREEN)));

                                    } else if(can_be_cycle == 2) {
                                        SCC_graph.add(c_node.link(to(n_node).with(
                                                Style.DASHED,
                                                LinkAttr.weight(Constants.GRAPHVIZ_EDGE_WEIGHT),
                                                Label.of("d = " + entry.getValue()),
                                                Color.BLUE)));
                                        SCC_graph.add(n_node.link(to(c_node).with(
                                                Style.SOLID,
                                                LinkAttr.weight(Constants.GRAPHVIZ_EDGE_WEIGHT),
                                                Color.GREEN)));
                                    } else {
                                        // TODO: ASSUMING READ TO WRITE PROBS WRONG!
                                        SCC_graph.add(n_node.link(to(c_node).with(
                                                Style.DASHED,
                                                LinkAttr.weight(Constants.GRAPHVIZ_EDGE_WEIGHT),
                                                Label.of("d = " + entry.getValue()),
                                                Color.BLUE)));
                                        SCC_graph.add(c_node.link(to(n_node).with(
                                                Style.SOLID,
                                                LinkAttr.weight(Constants.GRAPHVIZ_EDGE_WEIGHT),
                                                Color.GREEN)));
                                    }
//                                    SCC_graph.add(n_node.link(to(c_node).with(
//                                            Style.DASHED,
//                                            LinkAttr.weight(Constants.GRAPHVIZ_EDGE_WEIGHT),
//                                            Label.of("d = " + entry.getValue()),
//                                            Color.BLUE)));
//                                    SCC_graph.add(c_node.link(to(n_node).with(
//                                            Style.SOLID,
//                                            LinkAttr.weight(Constants.GRAPHVIZ_EDGE_WEIGHT),
//                                            Color.GREEN)));
                                }
                            } else {
                                Logger.info("We have done this already");
                            }
                        }
                        cur_node = new SCCNode(scc_chain.get(i));
                    }
                }
                completed_chains.add(scc_chain);
            } else {
                Logger.debug("Chain already completed.");
            }
        }
        for(Map.Entry<String, IfStatement> el : if_stmts.get_statements().entrySet()) {
            guru.nidi.graphviz.model.Node if_node = node(el.getKey());
            SCCNode true_b = find_matching_node(el.getValue().get_true_branch().toString());
            SCCNode false_b = find_matching_node(el.getValue().get_false_branch().toString());
            guru.nidi.graphviz.model.Node false_n;
            guru.nidi.graphviz.model.Node true_n;
            if(Utils.not_null(true_b)) {
                true_n = node(true_b.get_stmt().toString());
            } else {
                true_n = node(el.getValue().get_true_branch().toString());
            }
            SCC_graph.add(if_node.link(to(true_n).with(Style.ROUNDED, LinkAttr.weight(Constants.GRAPHVIZ_EDGE_WEIGHT))));
            if(Utils.not_null(false_b)) {
                false_n = node(false_b.get_stmt().toString());
            } else {
                false_n = node(el.getValue().get_false_branch().toString());
            }
            SCC_graph.add(if_node.link(to(false_n).with(Style.ROUNDED, LinkAttr.weight(Constants.GRAPHVIZ_EDGE_WEIGHT))));

        }
        Utils.print_graph(SCC_graph, Constants.EMPTY_SCC);
    }

    /**
     * find a node that matches the stmt (non augmented!)
     * @param stmt the stmt
     * @return the Node or null
     */
    private SCCNode find_matching_node(String stmt) {
        for(SCCNode el : nodes) {
            if(Objects.equals(stmt, el.get_stmt().toString())) {
                return el;
            }
        }
        return null;
    }

    // TODO: do not delete this until I check with Ana!
    /*void make_scc_graph(PhiVariableContainer pvc, ArrayVariables array_vars, Map<String, Integer> constants,
                        ArrayDefUseGraph def_use_graph) {
        // TODO: filter on line number and read write (only some links matter!)
        Set<PhiVariable> phi_variables = pvc.get_phi_vars();
        for(SCCNode node : nodes) {
            if (has_read_write(node)) {
                String use_index = node.get_index().to_str();
                Logger.info("USE INDEX DEP CHAIN: ");
                ImmutablePair<Variable, List<AssignStmt>> use_dep_chain = pvc.get_var_dep_chain(constants, use_index);
                Solver use_solver = new Solver(use_index, use_dep_chain, pvc, constants);
                String use_eq = use_solver.get_resolved_eq();
                pvc.print_var_dep_chain(constants, node.get_index().to_str());
                if (use_eq.contains("=")) {
                    use_eq = use_eq.split(" = ")[1];
                }
                Map<String, Integer> d_vals = use_solver.solve();
                Logger.info(use_eq);
                for (PhiVariable pv : phi_variables) {
                    String def = pv.get_phi_def().toString();
                    if (use_eq.contains(def)) {
                        int d = 0;
                        if (!d_vals.isEmpty()) {
                            d = d_vals.get(def);
                        }
                        guru.nidi.graphviz.model.Node def_node = node(pv.toString());
                        String node_stmt = node.get_stmt();
                        for (Map.Entry<String, Node> entry : def_use_graph.get_nodes().entrySet()) {
                            if (Objects.equals(entry.getValue().get_stmt().toString(), node_stmt)) {
                                node_stmt = entry.getValue().get_aug_stmt_str();
                            }
                        }
                        guru.nidi.graphviz.model.Node use_node = node(node_stmt.replace(node.get_index().to_str(), use_eq));
                        if(d != 0) {
                            SCC_graph.add(def_node.link(to(use_node).with(
                                    Style.SOLID,
                                    LinkAttr.weight(Constants.GRAPHVIZ_EDGE_WEIGHT))));
                            SCC_graph.add(use_node.link(to(def_node).with(
                                    Style.DASHED,
                                    LinkAttr.weight(Constants.GRAPHVIZ_EDGE_WEIGHT),
                                    Label.of("d = " + d))));
                        } else {
                            Logger.debug(String.format("d value is zero for link between '%s' and '%s'.",
                                    def_node.name(), use_node.name()));
                        }
                    }
                }
            } else {
                Logger.info(String.format("Node '%s' is not read and written to.", node.get_stmt()));
            }
        }
        Utils.print_graph(SCC_graph, Constants.EMPTY_SCC);
    }*/
}
