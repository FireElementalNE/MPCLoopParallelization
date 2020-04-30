import guru.nidi.graphviz.attribute.Color;
import guru.nidi.graphviz.attribute.LinkAttr;
import guru.nidi.graphviz.model.MutableGraph;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.tinylog.Logger;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.shimple.PhiExpr;

import java.util.*;
import java.util.stream.Collectors;

import static guru.nidi.graphviz.model.Factory.*;

/**
 * A variable object holds all variables that are linked to a phi expression
 * It has a map _aliases_ which contains information on how every variable linked to the phi expression maps
 * to every other variable that gets added TODO: need to explain this better
 */
@SuppressWarnings("FieldMayBeFinal")
public class Variable {
    private Map<String, Set<Alias>> aliases;
    private Value root_val;
    private MutableGraph var_graph; // TODO: this might be stale.
    private MutableGraph def_graph;
    private PhiExpr phi_expr;

    /**
     * Create a new variable object.
     * @param v the variable
     * @param phi_expr the phi expression
     */
    Variable(Value v, PhiExpr phi_expr) {
        this.root_val = v;
        this.phi_expr = phi_expr;
        this.aliases = new HashMap<>();
        aliases.put(v.toString(), new HashSet<>());
        this.var_graph = null;
        this.def_graph = null;
    }

    /**
     * copy constructor for the Variable class
     * @param var the variable being copied
     */
    Variable(Variable var) {
        this.phi_expr = var.phi_expr;
        this.aliases = new HashMap<>(var.aliases);
        this.root_val = var.root_val;
        this.var_graph = var.var_graph;
        this.def_graph = var.def_graph;
    }

    /**
     * add an alias to the set of known aliases
     * this not only created a new alias but add this alias new alias to the set
     * of every variable containing the old alias
     * @param link a pair consisting of the new alias and the old alias
     * @param stmt the assignment stmt
     */
    void add_alias(ImmutablePair<Value, Value> link, AssignStmt stmt) {
        String old_val = link.getLeft().toString();
        String new_val = link.getRight().toString();
        // check if it changing an existing value
        for(Map.Entry<String, Set<Alias>> entry : aliases.entrySet()) {
            if(Objects.equals(entry.getKey(), old_val)) {
                entry.getValue().add(new Alias(new_val, stmt));
                aliases.put(new_val, new HashSet<>());
                break;
            }
        }
    }

    /**
     * checks to see if any variable has ever been a given value
     * @param v the value (as a string)
     * @return true iff this variable (or any alias of this variable) has been the given value
     */
    public boolean has_ever_been(String v) {
        for(Map.Entry<String, Set<Alias>> entry : aliases.entrySet()) {
            if(Objects.equals(entry.getKey(), v)) {
                return true;
            }
            for(Alias a : entry.getValue()) {
                if(Objects.equals(a.get_name(), v)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * recursive helper function to create a graph for the variable
     * @param current_var the current value being parsed
     */
    private void parse_node_graph(String current_var) {
        guru.nidi.graphviz.model.Node current_node = node(current_var);
        Set<Alias> children = aliases.get(current_var);
        if(children.isEmpty()) {
            var_graph.add(current_node);
        } else {
            for (Alias a : aliases.get(current_var)) {
                guru.nidi.graphviz.model.Node child = node(a.get_name());
                var_graph.add(current_node.link(to(child).with(LinkAttr.weight(Constants.GRAPHVIZ_EDGE_WEIGHT))));
                parse_node_graph(a.get_name());
            }
        }
    }

    /**
     * make a GraphViz graph for the variable (if it has more than one alias)
     * @param phi_name the name of the phi node
     */
    void make_graph(String phi_name) {
        String graph_name = String.format("%s_%S", phi_name, root_val.toString());
        if(aliases.size() > 1) {
            this.var_graph = mutGraph(graph_name).setDirected(true);
            parse_node_graph(root_val.toString());
            Utils.print_graph(var_graph);
        } else {
            Logger.info(root_val.toString() + " only has one alias, itself. Not making graph.");
        }
    }

    /**
     * check to see if a set of aliases contains a given variable name
     * @param alias_set the alias set
     * @param s the variable name
     * @return true iff the set contains the given variable name
     */
    private boolean alias_set_contains(Set<Alias> alias_set, String s) {
        for(Alias a : alias_set) {
            if(Objects.equals(a.get_name(), s)) {
                return true;
            }
        }
        return false;
    }

    /**
     * get an alias associated with a variable from a set of aliases
     * @param alias_set the set of aliases
     * @param s the variable name
     * @return an Alias iff the set contains an alias corresponding to the variable name otherwise null
     */
    private Alias get_alias(Set<Alias> alias_set, String s) {
        for(Alias a : alias_set) {
            if(Objects.equals(a.get_name(), s)) {
                return a;
            }
        }
        return null;
    }

    /**
     * get the 'parents' of a given variable
     * @param child the name of the child variable
     * @return a pair containing the parent name and the Alias that set it to the child if it exits otherwise null
     */
    List<ImmutablePair<String, Alias>> get_parents(String child) {
        List<ImmutablePair<String, Alias>> parents = new ArrayList<>();
        for(Map.Entry<String, Set<Alias>> entry : aliases.entrySet()) {
            if(alias_set_contains(entry.getValue(), child)) {
                Alias a = get_alias(entry.getValue(), child);
                assert Utils.not_null(a) : "Should NEVER get here";
                parents.add(new ImmutablePair<>(entry.getKey(), a));
            }
        }
        return parents;
    }

    /**
     * create a string tracing one variable definition back to the source definition. It also prints the
     * assigment corresponding to the alias. (recursive)
     * @param current_var the current variable
     * @return the def/use string
     */
    private String build_var_dep_chain_str(String current_var) {
        List<ImmutablePair<String, Alias>> p_pairs = get_parents(current_var);
        assert p_pairs.size() == 1 || p_pairs.size() == 2 || p_pairs.isEmpty();
        if(p_pairs.size() == 1) {
            ImmutablePair<String, Alias> p_pair = p_pairs.get(0);
            return current_var + " (" + p_pair.getRight().get_stmt().getRightOp().toString() +
                        ") -> " + build_var_dep_chain_str(p_pair.getLeft());
        }
        else if(p_pairs.size() == 2){
            ImmutablePair<String, Alias> p_pair1 = p_pairs.get(0);
            ImmutablePair<String, Alias> p_pair2 = p_pairs.get(1);
            return current_var + " [(" + p_pair1.getRight().get_stmt().getRightOp().toString() +
                    ") -> " + build_var_dep_chain_str(p_pair1.getLeft()) + "]" +
                    "[(" + p_pair2.getRight().get_stmt().getRightOp().toString() +
                    ") -> " + build_var_dep_chain_str(p_pair2.getLeft()) + "]";
        }
        return "(" + phi_expr.toString() + ")";
    }

    /**
     * initiate the creation of a def/use string (see parse_node_print())
     * @param constants a map of variables that are constants (phi values do not effect them at all) and their values
     * @param v the variable
     * @return the parse node string if there is some alias of it otherwise null
     */
    String get_var_dep_chain_str(Map<String, Integer> constants, String v) {
        // TODO: check root var?
        if(aliases.containsKey(v)) {
            return build_var_dep_chain_str(v);
        }
        return null;
    }

    /**
     * Recursive function to get the def/use chain of  the passed variable to the root variable
     * represented as a set. (so order is pretty wacky)
     * @param current_var the variable name
     * @param def_lst the list of assignment statements (built up recursively)
     * @return the set of assignment statements
     */
    private Set<AssignStmt> parse_var_dep_chain(String current_var, Set<AssignStmt> def_lst) {
        // parents can only be of size 0, 1, or 2
        List<ImmutablePair<String, Alias>> p_pairs = get_parents(current_var);
        assert p_pairs.size() == 1 || p_pairs.size() == 2 || p_pairs.isEmpty();
        if(p_pairs.size() == 1) {
            ImmutablePair<String, Alias> p_pair = p_pairs.get(0);
            def_lst.add(p_pair.getRight().get_stmt());
            return parse_var_dep_chain(p_pair.getLeft(), def_lst);
        }
        else if(p_pairs.size() == 2) {
            ImmutablePair<String, Alias> p_pair1 = p_pairs.get(0);
            ImmutablePair<String, Alias> p_pair2 = p_pairs.get(1);
            def_lst.add(p_pair1.getRight().get_stmt());
            def_lst = parse_var_dep_chain(p_pair1.getLeft(), def_lst);
            def_lst.add(p_pair2.getRight().get_stmt());
            return parse_var_dep_chain(p_pair2.getLeft(), def_lst);
        }
        return def_lst;
    }


    /**
     * function to start the recursive  parse_var_dep_chain() function with an empty list
     * @param constants a map of variables that are constants (phi values do not effect them at all) and their values
     * @param v a variable name
     * @return the def/use chain of from the passed variable to the root variable represented as a list.
     */
    Set<AssignStmt> get_var_dep_chain(Map<String, Integer> constants, String v) {
        Set<AssignStmt> def_lst = new HashSet<>();
        if(aliases.containsKey(v)) {
            return parse_var_dep_chain(v, def_lst);
        }
        return def_lst;
    }


    /**
     * If there is a constant in the passed assignment stmt add it, and a link from the current node
     * to the def_graph
     * @param stmt the statement
     * @param cur_node the current node
     * @param constants a map of variables that are constants (phi values do not effect them at all) and their valeus
     */
    private void add_constant_nodes(AssignStmt stmt, guru.nidi.graphviz.model.Node cur_node,
                                    Map<String, Integer> constants) {
        List<String> uses = stmt.getUseBoxes().stream()
                .map(i -> i.getValue().toString()).collect(Collectors.toList());
        for(String u : uses) {
            if(constants.containsKey(u)) {
                guru.nidi.graphviz.model.Node con_node = node(u).with(Color.RED);
                def_graph.add(cur_node.link(to(con_node)
                        .with(LinkAttr.weight(Constants.GRAPHVIZ_EDGE_WEIGHT))));
            }
        }
    }

    /**
     * recursive function to create a visible dependency graph for a given variable
     * @param current_var the current variable being parsed
     * @param prev the previous node that we came from
     * @param constants a map of variables that are constants (phi values do not effect them at all) and their values
     */
    @SuppressWarnings("DuplicatedCode")
    private void create_var_dep_graph(String current_var, guru.nidi.graphviz.model.Node prev,
                                      Map<String, Integer> constants) {
        List<ImmutablePair<String, Alias>> p_pairs = get_parents(current_var);
        assert p_pairs.size() == 1 || p_pairs.size() == 2 || p_pairs.isEmpty();
        if(p_pairs.size() == 1) {
            ImmutablePair<String, Alias> p_pair = p_pairs.get(0);
            AssignStmt stmt = p_pair.getRight().get_stmt();
            guru.nidi.graphviz.model.Node p = node(stmt.toString());
            def_graph.add(prev.link(to(p).with(LinkAttr.weight(Constants.GRAPHVIZ_EDGE_WEIGHT))));
            add_constant_nodes(stmt, p, constants);
            create_var_dep_graph(p_pair.getLeft(), p, constants);
        }
        else if(p_pairs.size() == 2){
            ImmutablePair<String, Alias> p_pair1 = p_pairs.get(0);
            ImmutablePair<String, Alias> p_pair2 = p_pairs.get(1);
            AssignStmt stmt1 = p_pair1.getRight().get_stmt();
            AssignStmt stmt2 = p_pair2.getRight().get_stmt();
            guru.nidi.graphviz.model.Node p1 = node(stmt1.toString());
            guru.nidi.graphviz.model.Node p2 = node(stmt2.toString());
            def_graph.add(prev.link(to(p1).with(LinkAttr.weight(Constants.GRAPHVIZ_EDGE_WEIGHT))));
            add_constant_nodes(stmt1, p1, constants);
            create_var_dep_graph(p_pair1.getLeft(), p1, constants);
            def_graph.add(prev.link(to(p2).with(LinkAttr.weight(Constants.GRAPHVIZ_EDGE_WEIGHT))));
            add_constant_nodes(stmt2, p2, constants);
            create_var_dep_graph(p_pair2.getLeft(), p2, constants);
        } else {
            guru.nidi.graphviz.model.Node root = node(phi_expr.toString()).with(Color.GREEN);
            def_graph.add(prev.link(to(root).with(LinkAttr.weight(Constants.GRAPHVIZ_EDGE_WEIGHT))));
        }
    }

    /**
     * function to start the recursive function create_var_dep_graph()
     * @param constants a map of variables that are constants (phi values do not effect them at all) and their values
     * @param v the top level variable we are looking at.
     */
    void make_var_dep_chain_graph(Map<String, Integer> constants, String v) {
        guru.nidi.graphviz.model.Node current_node = node(v);
        String graph_name = String.format("%s_%s_def", v, root_val.toString());
        this.def_graph = mutGraph(graph_name).setDirected(true);
        create_var_dep_graph(v, current_node, constants);
        Utils.print_graph(def_graph);
    }

    /**
     * getter function for the PhiExpr
     * @return the PhiExpr
     */
    PhiExpr get_phi_expr() {
        return phi_expr;
    }

}
