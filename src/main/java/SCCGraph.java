import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.attribute.LinkAttr;
import guru.nidi.graphviz.attribute.Style;
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
    /**
     * the SCC graph
     */
    private MutableGraph SCC_graph;
    /**
     * a list of SCC nodes
     */
    Set<SCCNode> nodes;

    /**
     * the base constructor for the SCC graph
     * @param class_name the name of the class being analyzed
     */
    SCCGraph(String class_name) {
        this.SCC_graph = mutGraph(class_name + "_scc_final").setDirected(true);
        this.nodes = new HashSet<>();
    }

    /**
     * Copy constructor for the SCC graph
     * @param sccg the other SCC graph
     */
    SCCGraph(SCCGraph sccg) {
        this.SCC_graph = sccg.SCC_graph;
        this.nodes = new HashSet<>(sccg.nodes);
    }

    /**
     * add an SCC node to the graph
     * @param node the node to add
     */
    void add_node(SCCNode node) {
        this.nodes.add(node);
    }

    /**
     * make an SCC graph png
     * @param pvc the container class that holds all non array phi variables
     * @param array_vars A wrapper class that contains a Map for all array variables to  array version
     * @param constants A list of the _original_ phi variables that is queried on the second iteration
     * @param def_use_graph The final DefUse Graph
     */
    void make_scc_graph(PhiVariableContainer pvc, ArrayVariables array_vars, Map<String, Integer> constants,
                        ArrayDefUseGraph def_use_graph) {
        Set<PhiVariable> phi_variables = pvc.get_phi_vars();
        for(SCCNode node : nodes) {
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
                    if(!d_vals.isEmpty()) {
                        d = d_vals.get(def);
                    }
                    guru.nidi.graphviz.model.Node def_node = node(pv.toString());
                    String node_stmt = node.get_stmt();
                    for(Map.Entry<String, Node> entry : def_use_graph.get_nodes().entrySet()) {
                        if(Objects.equals(entry.getValue().get_stmt(), node_stmt)) {
                            node_stmt = entry.getValue().get_aug_stmt_str();
                        }
                    }
                    guru.nidi.graphviz.model.Node use_node = node(node_stmt.replace(node.get_index().to_str(), use_eq));
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
        Utils.print_graph(SCC_graph, Constants.EMPTY_SCC);
    }
}
