import guru.nidi.graphviz.attribute.LinkAttr;
import guru.nidi.graphviz.model.MutableGraph;
import org.apache.commons.lang3.tuple.ImmutablePair;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.shimple.PhiExpr;

import java.util.*;

import static guru.nidi.graphviz.model.Factory.*;

public class Variable {
    private Map<String, Set<Alias>> aliases;
    private Value root_val;
    private MutableGraph var_graph;
    private PhiExpr phi_expr;

    /**
     * Create a new variable object. A variable object holds all variables that are linked to a phi expression
     * It has a map _aliases_ which contains information on how every variable linked to the phi expression maps
     * to every other variable that gets added TODO: need to explain this better
     * @param v the variable
     * @param phi_expr the phi expression
     */
    Variable(Value v, PhiExpr phi_expr) {
        this.root_val = v;
        this.phi_expr = phi_expr;
        this.aliases = new HashMap<>();
        aliases.put(v.toString(), new HashSet<>());
        this.var_graph = null;
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
     * make a graphviz graph for the variable
     * @param phi_name the name of the phi node
     */
    void make_graph(String phi_name) {
        String graph_name = String.format("%s_%S", phi_name, root_val.toString());
        this.var_graph = mutGraph(graph_name).setDirected(true);
        parse_node_graph(root_val.toString());
        Utils.print_graph(var_graph);
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
     * get the 'parent' of a given variable (what is this aliasing?)
     * @param child the name of the child variable
     * @return a pair containing the parent name and the Alias that set it to the child if it exits otherwise null
     */
    ImmutablePair<String, Alias> get_parent(String child) {
        for(Map.Entry<String, Set<Alias>> entry : aliases.entrySet()) {
            if(alias_set_contains(entry.getValue(), child)) {
                Alias a = get_alias(entry.getValue(), child);
                assert Utils.not_null(a) : "Should NEVER get here";
                return new ImmutablePair<>(entry.getKey(), a);
            }
        }
        return null;
    }

    /**
     * create a string tracing one variable definition back to the source definition. It also prints the
     * assigment corresponding to the alias. (recursive)
     * @param current_var the current variable
     * @return the def/use string
     */
    private String get_parse_node_str(String current_var) {
        ImmutablePair<String, Alias> p_pair = get_parent(current_var);
        if(Utils.not_null(p_pair)) {
            return current_var + " (" + p_pair.getRight().get_stmt().getRightOp().toString() +
                    ") -> " + get_parse_node_str(p_pair.getLeft());
        } else {
            // if it has no parent it must be a root node!
            return current_var + " (" + phi_expr.toString() + ").";
        }
    }

    /**
     * initiate the creation of a def/use string (see parse_node_print())
     * @param v the variable
     * @return the parse node string if there is some alias of it otherwise null
     */
    String get_defs_str(String v) {
        // TODO: check root var?
        if(aliases.containsKey(v)) {
            return get_parse_node_str(v);
        }
        return null;
    }

    /**
     * Recursive function to get the def/use chain of from the passed variable to the root variable
     * represented as a list.
     * @param current_var the variable name
     * @param def_lst the list of assignment statements
     * @return the list of assignment statements
     */
    List<AssignStmt> get_parse_node_lst(String current_var, List<AssignStmt> def_lst) {
        ImmutablePair<String, Alias> p_pair = get_parent(current_var);
        if(Utils.not_null(p_pair)) {
            def_lst.add(p_pair.getRight().get_stmt());
            return get_parse_node_lst(p_pair.getLeft(), def_lst);
        } else {
            // if it has no parent it must be a root node!
            return def_lst;
        }
    }

    /**
     * function to start the recursive function get_parse_node_lst() with an empty list
     * @param v a variable name
     * @return the def/use chain of from the passed variable to the root variable represented as a list.
     */
    List<AssignStmt> get_def_lst(String v) {
        List<AssignStmt> def_lst = new ArrayList<>();
        if(aliases.containsKey(v)) {
            return get_parse_node_lst(v, def_lst);
        }
        return def_lst;
    }

    /**
     * getter function for the PhiExpr
     * @return the PhiExpr
     */
    PhiExpr get_phi_expr() {
        return phi_expr;
    }

}
