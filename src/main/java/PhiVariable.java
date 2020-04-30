import guru.nidi.graphviz.attribute.LinkAttr;
import guru.nidi.graphviz.attribute.Style;
import guru.nidi.graphviz.model.MutableGraph;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.tinylog.Logger;
import soot.Value;
import soot.ValueBox;
import soot.jimple.AssignStmt;
import soot.shimple.PhiExpr;
import soot.toolkits.scalar.ValueUnitPair;

import java.util.*;

import static guru.nidi.graphviz.model.Factory.*;

/**
 * Class representing a phi variable. Theses variables are the only that can "change" per loop iterations
 * Any variable based off of theses variables also can "change"
 */
@SuppressWarnings("FieldMayBeFinal")
public class PhiVariable {
    // TODO: add javadoc for these variables
    private PhiExpr phi_expr;
    private Value phi_def;
    private Map<Integer, ImmutablePair<ValueBox, AssignStmt>> all_values;
    private List<AssignStmt> linked_stmts;
    private List<Variable> var_links;
    private int counter;
    private boolean used_as_index;
    private MutableGraph non_index_graph;


    /**
     * create a new PhiVariable
     * @param stmt the original phi definition
     */
    PhiVariable(AssignStmt stmt) {
        this.phi_expr = (PhiExpr)stmt.getRightOp();
        this.phi_def = stmt.getLeftOpBox().getValue();
        this.all_values = new HashMap<>();
        this.linked_stmts = new ArrayList<>();
        this.counter = 1;
        this.all_values.put(counter, new ImmutablePair<>(stmt.getLeftOpBox(), stmt));
        this.var_links = new ArrayList<>();
        var_links.add(new Variable(phi_def, phi_expr));
        for(Value v : phi_expr.getValues()) {
            var_links.add(new Variable(v, phi_expr));
        }
    }

    /**
     * copy a PhiVariable
     * @param pv the PhiVariable being copied
     */
    PhiVariable(PhiVariable pv) {
        this.phi_expr = pv.phi_expr;
        this.phi_def = pv.phi_def;
        this.counter = pv.counter;
        this.all_values = new HashMap<>(pv.all_values);
        this.linked_stmts = new ArrayList<>(pv.linked_stmts);
        this.var_links = new ArrayList<>(pv.var_links);
        this.used_as_index = pv.used_as_index;
        this.non_index_graph = pv.non_index_graph;
    }

    /**
     * test if a variable is either the def of a phi var or one of the uses
     * @param var the variable
     * @return true iff the variable is a def of a phi var or one of the uses.
     */
    boolean contains_var(String var) {
        String def = phi_def.toString();
        List<String> use_vars = Utils.get_phi_var_uses_as_str(phi_expr);
        return Objects.equals(def, var) || use_vars.contains(var);
    }

    /**
     * PhiVariable toString()
     * @return a string representing the PhiVariable
     */
    @Override
    public String toString() {
        // TODO: show more info
        return String.format("%s = %s --> %s",
                phi_def.toString(), phi_expr.toString(),
                all_values.get(counter).getRight().toString());
    }

    /**
     * try and make a GraphViz graph of the PhiVariable
     * TODO: need to fix, the arrows are incorrect. Each element has a specific path back to the
     *   original phi stmt
     */
    void make_graph() {
        for(Variable v : var_links) {
            v.make_graph(phi_def.toString() + "-" + phi_expr.toString());
        }
    }

    /**
     * get the uses of PhiVariable alias in an assignment stmt
     * @param stmt the assignment statement
     * @return a list of Values that are in the all_values map that are used in the passed
     *   assignment stmt.
     */
    List<ImmutablePair<Value, Value>> get_phi_var_uses(AssignStmt stmt) {
        List<ImmutablePair<Value, Value>> values = new ArrayList<>();
        List<ValueBox> uses = stmt.getUseBoxes();
        ValueBox left_box = stmt.getLeftOpBox();
        // skip assigns with arrayref on the left hand side
        for(ValueBox vb : uses) {
            for(Map.Entry<Integer, ImmutablePair<ValueBox, AssignStmt>> entry : all_values.entrySet()) {
                Value v = entry.getValue().getLeft().getValue();
                if (Objects.equals(vb.getValue().toString(), v.toString())) {
                    // array writes can never be indexes...
                    if (!Utils.is_def(stmt)) {
                        values.add(new ImmutablePair<>(vb.getValue(), left_box.getValue()));
                    }
                }
            }
        }
        return values;
    }




    /**
     * Check if assigment statement defines a phi variable
     * @param stmt the assignment statement
     * @return true iff the assignment stmt defines this variables in the original phi expression of this
     *   PhiVariable
     */
    boolean defines_phi_var(AssignStmt stmt) {
        List<ValueBox> defs = stmt.getDefBoxes();
        for(ValueUnitPair vup : phi_expr.getArgs()) {
            for(ValueBox vb : defs) {
                if (Objects.equals(vup.getValue().toString(), vb.getValue().toString())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * check to see if the passed values has ever been an alias of this PhiVariable
     * @param v the value to check
     * @return true iff this PhiVariable has ever been this value
     */
    boolean has_ever_been(String v) {
        // must be int!
        assert Objects.equals(v, Constants.INT_TYPE);
        for(Map.Entry<Integer, ImmutablePair<ValueBox, AssignStmt>> entry : all_values.entrySet()) {
            Value v1 = entry.getValue().getLeft().getValue();
            if(Objects.equals(v1.toString(), v)) {
                return true;
            }
        }
        return false;
    }

    /**
     * add an alias for this PhiVariable
     * @param vb the ValueBox for the variable
     * @param stmt the assignment statement for this variable
     * @param value_links observed links between aliased values
     */
    void add_alias(ValueBox vb, AssignStmt stmt, List<ImmutablePair<Value, Value>> value_links) {
        counter++;
        all_values.put(counter, new ImmutablePair<>(vb, stmt));
        for(ImmutablePair<Value, Value> v_pair : value_links) {
            for(Variable var : this.var_links) {
                if(var.has_ever_been(v_pair.getLeft().toString())) {
//                    if(v_pair.getRight() instanceof ArrayRef)
                    var.add_alias(v_pair, stmt);
                }
            }
        }
    }

    /**
     * create a def/use string for a given variable
     * @param constants a list of variables that are constants (phi values do not effect them at all) and their values
     * @param v the variable name
     * @return a def/use string iff this PhiVariable contains a Variable that has been the passed variable at
     *         at some point, otherwise null
     */
    String get_var_dep_chain_str(Map<String, Integer> constants, String v) {
        for(Variable var : var_links) {
            if(var.has_ever_been(v)) {
                return var.get_var_dep_chain_str(constants, v);
            }
        }
        return null;
    }

    /**
     * Make a def graph for a given variable (based on this phi variable)
     * @param constants a map of variables that are constants (phi values do not effect them at all) and their values
     * @param v the variable name
     */
    void make_var_dep_chain_graph(Map<String, Integer> constants, String v) {
        for(Variable var : var_links) {
            if(var.has_ever_been(v)) {
                var.make_var_dep_chain_graph(constants, v);
            }
        }
    }

    /**
     * get a pair that consists of:
     *   1. a variable
     *   2. the dep chain of that variable
     * @param constants a map of variables that are constants (phi values do not effect them at all) and their values
     * @param v the variable name
     * @return a pair consisting of the variable and the list of assignment statements (if it exists)
     *   otherwise null.
     */
    ImmutablePair<Variable, Set<AssignStmt>> get_var_dep_chain(Map<String, Integer> constants, String v) {
        for(Variable var : var_links) {
            if(var.has_ever_been(v)) {
                return new ImmutablePair<>(var, var.get_var_dep_chain(constants, v));
            }
        }
        return null;
    }

    /**
     * add a linked assignment statement for this PhiVariable (This is for looping variables)
     * @param stmt the assignment statement
     */
    void add_linked_stmt(AssignStmt stmt) {
        linked_stmts.add(stmt);
    }

    /**
     * check if the phi variable is a looping phi variable (that we care about)
     * @return true iff the variable is a looping variable that is also used as an index.
     */
    boolean is_looping_index_var() {
        return has_links() && used_as_index;
    }

    /**
     * getter for phi_expr
     * @return the phi_expr for this PhiVariable
     */
    PhiExpr get_phi_expr() {
        return phi_expr;
    }

    /**
     * getter for phi_def
     * @return the phi_def for this PhiVariable
     */
    Value get_phi_def() {
        return phi_def;
    }

    /**
     * getter for linked_stmts
     * @return a list of linked statements for this PhiVariable
     */
    List<AssignStmt> get_linked_stmts() {
        return new ArrayList<>(linked_stmts);
    }

    /**
     * method for determining if this phi variable has links
     * @return true iff this phi variable has links (is most likely a looping var)
     */
    boolean has_links() {
        return !linked_stmts.isEmpty();
    }

    /**
     * check if this phi variable is ever used as an index
     * @return true iff this phi variables is used as an index
     */
    boolean is_used_as_index() {
        return used_as_index;
    }

    /**
     * setter for used as index, set the variable to passed value
     * @param used_as_index passed used as index value
     */
    @SuppressWarnings("SameParameterValue")
    void set_used_as_index(boolean used_as_index) {
        this.used_as_index = used_as_index;
    }

    /**
     * get all uses from the phi expr
     * @return all the phi expr uses
     */
    List<String> get_uses() {
        return Utils.get_phi_var_uses_as_str(phi_expr);
    }

    /**
     * test to determine if this phi variable has a linked statement that
     * defines the passed var
     * @param var the variable
     * @return true iff linked statements field contains a definition for
     *         the passed variable
     */
    boolean has_linked_var_def(String var) {
        for(AssignStmt stmt : this.linked_stmts) {
            String left_name = stmt.getLeftOp().toString();
            if(Objects.equals(var, left_name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * getter for linked statement that defines the passed var
     * @param var the variable
     * @return the assigment statement that defines the passed var (from linked_statements)
     *         if it does not contain it null.
     */
    AssignStmt get_linked_var_def(String var) {
        assert has_linked_var_def(var);
        for(AssignStmt stmt : this.linked_stmts) {
            String left_name = stmt.getLeftOp().toString();
            if(Objects.equals(var, left_name)) {
                return stmt;
            }
        }
        return null;
    }

    /**
     * write a graph for non-index phi variables showing all the versions in order.
     */
    void write_non_index_graph() {
        if(!used_as_index) {
            this.non_index_graph = mutGraph("NonIndex-" + phi_def.toString()).setDirected(true);
            guru.nidi.graphviz.model.Node cur_node = node(phi_def.toString());
            for(int i = 1; i <= counter; i++) {
                ImmutablePair<ValueBox, AssignStmt> value = all_values.get(i);
                guru.nidi.graphviz.model.Node tmp =
                        node(value.getLeft().getValue().toString() + " -> " + value.getRight().toString());
                non_index_graph.add(cur_node.link(to(tmp).with(Style.ROUNDED, LinkAttr.weight(Constants.GRAPHVIZ_EDGE_WEIGHT))));
                cur_node = tmp;
            }
            Utils.print_graph(non_index_graph);
        } else {
            Logger.warn("Cannot make graph for '" + this.toString() + "' as it is an index.");
        }
    }

    /**
     * getter for the counter variable
     * @return the counter variable
     */
    int get_counter() {
        return counter;
    }
}
