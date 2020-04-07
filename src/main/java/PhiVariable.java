import org.apache.commons.lang3.tuple.ImmutablePair;
import soot.Value;
import soot.ValueBox;
import soot.jimple.AssignStmt;
import soot.shimple.PhiExpr;
import soot.toolkits.scalar.ValueUnitPair;

import java.util.*;

/**
 * Class representing a phi variable. Theses variables are the only that can "change" per loop iterations
 * Any variable based off of theses variables also can "change"
 */
public class PhiVariable {
    // TODO: add javadoc for these variables
    private PhiExpr phi_expr;
    private ValueBox phi_def;
    private Map<Integer, ImmutablePair<ValueBox, AssignStmt>> all_values;
    private List<AssignStmt> linked_stmts;
    private List<Variable> var_links;
    private int counter;
    private boolean used_as_index;

    /**
     * create a new PhiVariable
     * @param stmt the original phi definition
     */
    PhiVariable(AssignStmt stmt) {
        this.phi_expr = (PhiExpr)stmt.getRightOp();
        this.phi_def = stmt.getLeftOpBox();
        this.all_values = new HashMap<>();
        this.linked_stmts = new ArrayList<>();
        this.counter = 1;
        this.all_values.put(counter, new ImmutablePair<>(stmt.getLeftOpBox(), stmt));
        this.var_links = new ArrayList<>();
        var_links.add(new Variable(phi_def.getValue(), phi_expr));
        for(ValueUnitPair vup : phi_expr.getArgs()) {
            var_links.add(new Variable(vup.getValue(), phi_expr));
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
    }

    /**
     * PhiVariable toString()
     * @return a string representing the PhiVariable
     */
    @Override
    public String toString() {
        // TODO: show more info
        return String.format("%s = %s --> %s",
                phi_def.getValue().toString(), phi_expr.toString(),
                all_values.get(counter).getRight().toString());
    }

    /**
     * make a graphviz graph of the PhiVariable
     * TODO: need to fix, the arrows are incorrect. Each element has a specific path back to the
     *   original phi stmt
     */
    void make_graph() {
        for(Variable v : var_links) {
            v.make_graph(phi_def.getValue().toString() + "-" + phi_expr.toString());
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
     * @param v the variable name
     * @return a def/use string iff this PhiVariable contains a Variable that has been the passed variable at
     *         at some point, otherwise null
     */
    String get_var_dep_chain_str(String v) {
        for(Variable var : var_links) {
            if(var.has_ever_been(v)) {
                return var.get_defs_str(v);
            }
        }
        return null;
    }

    /**
     * get a pair that consists of:
     *   1. a variable
     *   2. the dep chain of that variable
     * @param v the variable name
     * @return a pair consisting of the variable and the list of assignment statements (if it exists)
     *   otherwise null.
     */
    ImmutablePair<Variable, Set<AssignStmt>> get_var_dep_chain(String v) {
        for(Variable var : var_links) {
            if(var.has_ever_been(v)) {
                return new ImmutablePair<>(var, var.get_def_lst(v));
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
        return has_link() && used_as_index;
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
    ValueBox get_phi_def() {
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
    boolean has_link() {
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
    void set_used_as_index(boolean used_as_index) {
        this.used_as_index = used_as_index;
    }


}
