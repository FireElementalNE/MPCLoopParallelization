import org.apache.commons.lang3.tuple.ImmutablePair;
import soot.Value;
import soot.ValueBox;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.shimple.PhiExpr;
import soot.toolkits.scalar.ValueUnitPair;

import java.util.*;

public class PhiVariable {
    // TODO: add javadoc for these variables
    private PhiExpr phi_expr;
    private ValueBox phi_def;
    private Map<Integer, ImmutablePair<ValueBox, AssignStmt>> all_values;
    private List<AssignStmt> linked_stmts;
    private List<Variable> var_links;
    private int counter;

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
        var_links.add(new Variable(phi_def.getValue()));
        for(ValueUnitPair vup : phi_expr.getArgs()) {
            var_links.add(new Variable(vup.getValue()));
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
                    if (!(left_box.getValue() instanceof ArrayRef)) {
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
    boolean has_ever_been(Value v) {
        // must be int!
        assert Objects.equals(v.getType().toString(), Constants.INT_TYPE);
        for(Map.Entry<Integer, ImmutablePair<ValueBox, AssignStmt>> entry : all_values.entrySet()) {
            Value v1 = entry.getValue().getLeft().getValue();
            if(Objects.equals(v1.toString(), v.toString())) {
                return true;
            }
        }
        return false;
    }

    /**
     * add an alias for this PhiVariable
     * @param vb the ValueBox for the variable
     * @param s the assignment statement for this variable
     * @param value_links observed links between aliased values
     */
    void add_alias(ValueBox vb, AssignStmt s, List<ImmutablePair<Value, Value>> value_links) {
        counter++;
        all_values.put(counter, new ImmutablePair<>(vb, s));
        for(ImmutablePair<Value, Value> v_pair : value_links) {
            for(Variable var : this.var_links) {
                if(var.has_ever_been(v_pair.getLeft())) {
//                    if(v_pair.getRight() instanceof ArrayRef)
                    var.add_alias(v_pair);
                }
            }
        }
    }

    /**
     * add a linked assignment statement for this PhiVariable (This is for looping variables)
     * @param stmt the assignment statement
     */
    void add_linked_stmt(AssignStmt stmt) {
        linked_stmts.add(stmt);
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
}
