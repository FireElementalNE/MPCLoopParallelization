import guru.nidi.graphviz.attribute.Color;
import guru.nidi.graphviz.attribute.LinkAttr;
import guru.nidi.graphviz.attribute.Style;
import guru.nidi.graphviz.model.MutableGraph;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.tinylog.Logger;
import soot.Value;
import soot.ValueBox;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.shimple.PhiExpr;

import java.util.*;
import java.util.stream.Collectors;

import static guru.nidi.graphviz.model.Factory.*;

/**
 * A  convenience class to cut down on unreadable code. It contains all of the phi variables, as well
 * as their associated functions (does not contain created array phi variables!)
 */
@SuppressWarnings({"FieldMayBeFinal", "unused"})
public class PhiVariableContainer {
    /**
     * all true phi vars
     */
    private Set<PhiVariable> phi_vars;
    /**
     * a graph variable used to create multiple png graphs
     */
    private MutableGraph graph_creator;
    /**
     * variables used when making graphs (to make sure we do not add nodes multiple times)
     */
    private Set<String> parsed_phi_vars;

    /**
     * Constructor for PhiVariableContainer
     * @param class_name the name of the class being analyzed
     */
    PhiVariableContainer(String class_name) {
        this.phi_vars = new HashSet<>();
        this.parsed_phi_vars = new HashSet<>();
    }

    /**
     * Copy constructor for the PhiVariableContainer class
     * @param pvc the PhiVariable container being copied
     */
    PhiVariableContainer(PhiVariableContainer pvc) {
        this.phi_vars = pvc.phi_vars.stream().map(PhiVariable::new).collect(Collectors.toSet());
        this.graph_creator = pvc.graph_creator;
//        this.index_phi_var_links = pvc.index_phi_var_links;
        this.parsed_phi_vars = new HashSet<>(pvc.parsed_phi_vars);

    }

    /**
     * add a new phi variable.
     * @param pv the PhiVariable to add
     */
    void add(PhiVariable pv) {
        phi_vars.add(new PhiVariable(pv));
    }

    /**
     * process an assignment statement, finding:
     * 1. new looping statements
     * 2. new aliases
     * @param stmt the assignment being analyzed
     * @return true iff a link was found
     */
    boolean process_assignment(AssignStmt stmt) {
        boolean found_link = false;
        ValueBox left = stmt.getLeftOpBox();
        for(PhiVariable pv : phi_vars) {
            List<ImmutablePair<Value, Value>> values = pv.get_phi_var_uses(stmt);
            // if we are REDEFINING a phi variable it is a looping stmt.
            if(pv.defines_phi_var(stmt) && !values.isEmpty()) {
                found_link = true;
                Logger.debug("Found that stmt '" + stmt.toString()  + "' links to phi stmt '" + pv.toString() + "'.");
                Logger.debug("This is most likely a Looping stmt, also needs to be used as an index to be useful.");
                pv.add_linked_stmt(stmt);
                // TODO: not sure if this is correct...
                pv.add_alias(left, stmt, values);
            }
            else if(!values.isEmpty() && !Utils.is_def(stmt)) {
                // if we we are not DEFINING a phi var but we are using one
                found_link = true;
                Logger.debug("Found that stmt '" + stmt.toString() + "' uses phi vars:");
                Logger.debug("\toriginal phi: " + pv.toString());
                for(ImmutablePair<Value, Value> v_pair : values) {
                    Logger.debug("\t  " + v_pair.getLeft().toString() + " is affected by " + v_pair.getRight().toString());
                }
                pv.add_alias(left, stmt, values);
            }
            if(stmt.containsArrayRef()) {
                ArrayRef ar = stmt.getArrayRef();
                String index_name = ar.getIndexBox().getValue().toString();
                // Logger.info(pv.toString() + " has been " + index_name + ": " + );
                if(Objects.equals(pv.get_phi_def().toString(), index_name)
                        || pv.has_ever_been(index_name)) {
                    Logger.debug("PhiVar " + index_name + " used as an index, needs to also have a link");
//                    found_link = true;
                    pv.set_used_as_index(true);
                }
            }
        }
        return found_link;
    }

    /**
     * make graphs for all of the PhiVariables
     */
    void make_graphs() {
        for(PhiVariable pv : phi_vars) {
            pv.make_graph();
        }
    }

    /**
     * print a def/use string for a given variable
     * @param constants a map of variables that are constants (phi values do not effect them at all) and their values
     * @param v the variable name
     */
    void print_var_dep_chain(Map<String, Integer> constants, String v) {
        // TODO: this is a proof of concept function
        for(PhiVariable pv : phi_vars) {
            if(pv.has_ever_been(v)) {
                String s = pv.get_var_dep_chain_str(constants, v);
                if(Utils.not_null(s)) {
                    // make sure it is not null!
                    Logger.info(s);
                }
            }
        }
    }

    /**
     * make a variable dependency graph for the passed variable
     * @param constants a map of variables that are constants (phi values do not effect them at all) and their values
     * @param v the variable name
     */
    void make_var_dep_chain_graph(Map<String, Integer> constants, String v) {
        for(PhiVariable pv : phi_vars) {
            if(pv.has_ever_been(v)) {
               pv.make_var_dep_chain_graph(constants, v);
            }
        }
    }

    /**
     * get the actual list of assignments for the passed variable
     * @param constants a map of variables that are constants (phi values do not effect them at all) and their values
     * @param v the variable name
     * @return a pair consisting of the variable and the list of assignment statements (if it exists)
     *   otherwise null.
     */
    ImmutablePair<Variable, List<AssignStmt>> get_var_dep_chain(Map<String, Integer> constants, String v) {
        if(constants.containsKey(v)) {
            return new ImmutablePair<>(null, null);
        } else {
            for (PhiVariable pv : phi_vars) {
                if (pv.has_ever_been(v)) {
                    ImmutablePair<Variable, Set<AssignStmt>> ans = pv.get_var_dep_chain(constants, v);
                    // TODO: this now comes out unordered... fix this
                    return new ImmutablePair<>(ans.getLeft(), new ArrayList<>(ans.getRight()));
                }
            }
        }
        return null;
    }



    /**
     * get a list of looping index vars
     * @return a list of all looping index vars
     */
    List<PhiVariable> get_looping_index_vars() {
        List<PhiVariable> pv_lst = new ArrayList<>();
        for(PhiVariable pv : phi_vars) {
            if(pv.is_looping_index_var()) {
                pv_lst.add(new PhiVariable(pv));
            }
        }
        return pv_lst;
    }

    /**
     * get all phi variables that are NOT index variables (used for SCC)
     * @return the list of phi variables that are NOT index variables
     */
    List<PhiVariable> get_non_index_vars() {
        return phi_vars.stream().filter(pv -> !pv.is_used_as_index()).collect(Collectors.toList());
    }

    /**
     * get the list of phi variables
     * @return the list of phi variables
     */
    Set<PhiVariable> get_phi_vars() {
        return phi_vars;
    }

    /**
     * get a phi variable that has a link that defines the passed variable
     * @param link the passed variable
     * @return either null (if no phi variable contains the definition) or the phi variable containing the def
     */
    PhiVariable get_phi_var_with_link_def(String link) {
        List<PhiVariable> pv = this.phi_vars.stream()
                .filter(el -> el.has_linked_var_def(link)).collect(Collectors.toList());
        if(pv.size() == 1) {
            return pv.get(0);
        } else {
            Logger.debug("This value should be 1: " + pv.size());
            return null;
        }
    }

    /**
     * get a phi variable if it is in the container
     * @param def the definition name of the variable
     * @return the phi variable if it is in the container. If not returns null
     */
    PhiVariable get_phi_var(String def) {
        for(PhiVariable pv : phi_vars) {
            if(Objects.equals(pv.get_phi_def().toString(), def)) {
                return pv;
            }
        }
        return null;
    }


    /**
     * get the phi expression associated with a phi variable
     * @param var the variable
     * @return the phi expression iff there is a var defines a phi function otherwise null
     */
    PhiExpr get_phi_expr(String var) {
        for(PhiVariable pv : phi_vars) {
            if(Objects.equals(var, pv.get_phi_def().toString())) {
                return pv.get_phi_expr();
            }
        }
        return null;
    }

    /**
     * true iff there is a phi variable that uses this variable in it's definition
     * @param var the variable in question
     * @return true iff there is a phi variable that uses this variable in it's definition
     */
    boolean is_used_in_phi(String var) {
        for(PhiVariable pv : phi_vars) {
            if(pv.get_uses().contains(var)) {
                return true;
            }
        }
        return false;
    }

    /**
     * tests if variable defines a phi var
     * @param var the variable
     * @return true iff the variable defines a phi var
     */
    boolean defines_phi(String var) {
        for(PhiVariable pv : phi_vars) {
            if(Objects.equals(pv.get_phi_def().toString(), var)) {
                return true;
            }
        }
        return false;
    }


    /**
     * true iff the passed variable is a phi variable definition
     * @param var the variable in question
     * @return true iff the passed variable is a phi variable definition
     */
    boolean is_phi_def(String var) {
        for(PhiVariable pv : phi_vars) {
            if(Objects.equals(pv.get_phi_def().toString(), var)) {
                return true;
            }
        }
        return false;
    }

    /**
     * helper function to make the phi_link graph, this is for finding variables that change according to
     * phi variables (i.e. they change every loop iteration). This handles actual phi variables
     * @param pv the top level phi variable
     * @param cur_node the current node being parsed
     * @param phi_expr the phi expression of the node
     * @param array_vars the array variables that have been found (passed from Analysis)
     * @param constants the constants that have been found (passed from Analysis)
     */
    private void handle_phi_var(PhiVariable pv, guru.nidi.graphviz.model.Node cur_node, PhiExpr phi_expr,
                                ArrayVariables array_vars, Map<String, Integer> constants) {
        String var = cur_node.name().toString();
        cur_node = cur_node.with(Color.GREEN);
        Logger.debug("handle_phi_var: " + var + " (" + pv.toString() + ")");
        guru.nidi.graphviz.model.Node src_node;
        String src_name = var + " = " + phi_expr.toString();
        src_node = node(src_name).with(Color.BROWN);
        graph_creator.add(cur_node.link(to(src_node)
                .with(Style.ROUNDED, LinkAttr.weight(Constants.GRAPHVIZ_EDGE_WEIGHT))));
        List<String> phi_expr_uses = Utils.get_phi_var_uses_as_str(phi_expr);
        for (String use : phi_expr_uses) {
            guru.nidi.graphviz.model.Node use_node = node(use);
            boolean is_not_constant = false;
            if(constants.containsKey(use)) {
                use_node = use_node.with(Color.RED);
            } else if(pv.get_uses().contains(use)) {
                use_node = use_node.with(Color.CYAN);
            } else {
                use_node = use_node.with(Color.GREEN);
            }
            graph_creator.add(src_node.link(to(use_node)
                        .with(Style.ROUNDED, LinkAttr.weight(Constants.GRAPHVIZ_EDGE_WEIGHT))));
            if(!constants.containsKey(use)) { //  && !pv.get_uses().contains(use)) {
                add_phi_links_node(pv, use_node, array_vars, constants);
            }
        }
    }

    /**
     * helper function to make the phi_link graph, this is for finding variables that change according to
     * phi variables (i.e. they change every loop iteration). This handles non-phi variables
     * @param pv the top level phi variable
     * @param cur_node the current node being parsed
     * @param array_vars the array variables that have been found (passed from Analysis)
     * @param constants the constants that have been found (passed from Analysis)
     */
    private void handle_non_phi_var(PhiVariable pv, guru.nidi.graphviz.model.Node cur_node, ArrayVariables array_vars,
                                    Map<String, Integer> constants) {
        String var = cur_node.name().toString();
        Logger.debug("handle_non_phi_var: " + var + " (" + pv.toString() + ")");
        ImmutablePair<Variable, List<AssignStmt>> dep_chain = get_var_dep_chain(constants, var);
        if (Utils.not_null(dep_chain)) {
            for (AssignStmt stmt : dep_chain.getRight()) {
                List<String> uses = Utils.get_assignment_uses_as_str(stmt);
                boolean complex_var = false;
                for (String use : uses) {
                    guru.nidi.graphviz.model.Node src_node = node(use);
                    if (constants.containsKey(use)) {
                        src_node = src_node.with(Color.RED);
                    } else if (array_vars.contains_key(use)) {
                        src_node = src_node.with(Color.BLUE);
                    } else if (NumberUtils.isCreatable(use)) {
                        src_node = src_node.with(Color.ORANGE);
                    } else {
                        if(is_used_in_phi(use)) {
                            src_node = src_node.with(Color.DARKGREEN);
                        } else if(defines_phi(use)) {
                            src_node = src_node.with(Color.YELLOW);
                        } else {
                            complex_var = true;
                        }
                    }
                    // TODO: adding too many nodes
                    graph_creator.add(cur_node.link(to(src_node)
                            .with(Style.ROUNDED, LinkAttr.weight(Constants.GRAPHVIZ_EDGE_WEIGHT))));
                    if(complex_var) {
                        add_phi_links_node(pv, src_node, array_vars, constants);
                    }
                }
            }
        }
    }

    /**
     * helper function to make the phi_link graph, this is for finding variables that change according to
     * phi variables (i.e. they change every loop iteration). This handles all variables and splits
     * them between phi variables and non-phi variables
     * @param pv the top level phi variable
     * @param cur_node the current node being parsed
     * @param array_vars the array variables that have been found (passed from Analysis)
     * @param constants the constants that have been found (passed from Analysis)
     */
    private void add_phi_links_node(PhiVariable pv, guru.nidi.graphviz.model.Node cur_node,
                                    ArrayVariables array_vars, Map<String, Integer> constants) {
        String var = cur_node.name().toString();
        PhiExpr phi_expr = get_phi_expr(cur_node.name().toString());
        if(!parsed_phi_vars.contains(var)) {
            parsed_phi_vars.add(var);
            if (Utils.not_null(phi_expr) && pv.contains_var(var)) {
                handle_phi_var(pv, cur_node, phi_expr, array_vars, constants);
            } else {
                handle_non_phi_var(pv, cur_node, array_vars, constants);
            }
        }
    }

    /**
     * the start of the recursive function to make the phi links graph
     * @param array_vars the array variables that have been found (passed from Analysis)
     * @param constants the constants that have been found (passed from Analysis)
     */
    void make_phi_links_graph(ArrayVariables array_vars, Map<String, Integer> constants) {
        for (PhiVariable pv : phi_vars) {
            String var = pv.get_phi_def().toString();
            Logger.debug("make_phi_links_graph: " + var + " (" + pv.toString() + ")");
            if(pv.is_used_as_index()) {
                graph_creator = mutGraph(var + "_index_phi_var_links").setDirected(true);
            } else {
                graph_creator = mutGraph(var + "_non_index_phi_links").setDirected(true);
            }
            parsed_phi_vars = new HashSet<>();
            guru.nidi.graphviz.model.Node cur_node = node(var);
            add_phi_links_node(pv, cur_node, array_vars, constants);
            Utils.print_graph(graph_creator, String.format(Constants.EMPTY_PHI_LINKS, var));
        }
    }

    /**
     * write all the phi variable object graphs
     */
    void make_non_index_graphs() {
        for(PhiVariable pv : phi_vars) {
            pv.write_non_index_graph();
            Logger.info(pv.get_phi_def().toString() + ": " + pv.get_counter());
        }
    }
}
