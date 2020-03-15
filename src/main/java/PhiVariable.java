import soot.Value;
import soot.ValueBox;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.shimple.PhiExpr;
import soot.toolkits.scalar.ValueUnitPair;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PhiVariable {
    private PhiExpr phi_expr;
    private ValueBox phi_def;
    private List<AssignStmt> linked_stmts;
//    private List<PhiVariable> linked_phi_vars;

    PhiVariable(AssignStmt stmt) {
        this.phi_expr = (PhiExpr)stmt.getRightOp();
        this.phi_def = stmt.getLeftOpBox();
        this.linked_stmts = new ArrayList<>();
//        this.linked_phi_vars = new ArrayList<>();
    }

    PhiVariable(PhiVariable pv) {
        // CHECKTHIS: might need deep copy
        this.phi_expr = pv.phi_expr;
        this.phi_def = pv.phi_def;
        this.linked_stmts = new ArrayList<>(pv.linked_stmts);
//        this.linked_phi_vars = new ArrayList<>(pv.linked_phi_vars);
    }

    List<Value> get_phi_var_uses(AssignStmt stmt) {
        List<Value> values = new ArrayList<>();
        List<ValueBox> uses = stmt.getUseBoxes();
        ValueBox left_box = stmt.getLeftOpBox();
        // skip assigns with arrayref on the left hand side
        for(ValueBox vb : uses) {
            if(Objects.equals(vb.getValue().toString(), phi_def.getValue().toString())) {
                // array writes can never be indexes...
                if(!(left_box.getValue() instanceof ArrayRef)) {
                    values.add(vb.getValue());
                }
            }
        }
        return values;
    }

    boolean defines_phi_var(AssignStmt stmt) {
        List<ValueBox> defs = stmt.getDefBoxes();
//        for(PhiVariable pv : linked_phi_vars) {
//            // TODO: need to think this out more. more than one is not working correctly
//            return pv.defines_phi_var(stmt);
//        }
        for(ValueUnitPair vup : phi_expr.getArgs()) {
            for(ValueBox vb : defs) {
                if (Objects.equals(vup.getValue().toString(), vb.getValue().toString())) {
                    return true;
                }
            }
        }
        return false;
    }

    void add_linked_stmt(AssignStmt stmt) {
        linked_stmts.add(stmt);
    }

//    void add_linked_phi_var(PhiVariable pv) {
//        this.linked_phi_vars.add(new PhiVariable(pv));
//    }

    PhiExpr get_phi_expr() {
        return phi_expr;
    }

    List<AssignStmt> get_linked_stmts() {
        return new ArrayList<>(linked_stmts);
    }

//    List<PhiVariable> get_linked_phi_vars() {
//        return new ArrayList<>(linked_phi_vars);
//    }

    ValueBox get_phi_def() {
        return phi_def;
    }

    @Override
    public String toString() {
        return phi_def.toString() + " = " + phi_expr.toString();
    }
}
