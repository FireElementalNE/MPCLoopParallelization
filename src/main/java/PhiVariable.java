import soot.ValueBox;
import soot.jimple.AssignStmt;
import soot.shimple.PhiExpr;
import soot.toolkits.scalar.ValueUnitPair;

import java.util.List;
import java.util.Objects;

public class PhiVariable {
    private PhiExpr phi_expr;
    private ValueBox phi_def;

    PhiVariable(AssignStmt stmt) {
        this.phi_expr = (PhiExpr)stmt.getRightOp();
        this.phi_def = stmt.getLeftOpBox();
    }

    PhiVariable(PhiVariable pv) {
        // CHECKTHIS: might need deep copy
        this.phi_expr = pv.phi_expr;
        this.phi_def = pv.phi_def;
    }

    boolean linked_to(AssignStmt stmt) {
        // TODO: this is sloppy...
        List<ValueBox> defs = stmt.getDefBoxes();
        List<ValueBox> uses = stmt.getUseBoxes();
        boolean step1 = false;
        boolean step2 = false;
        for(ValueBox vb : uses) {
            if(Objects.equals(vb.getValue().toString(), phi_def.getValue().toString())) {
                step1 = true;
                break;
            }
        }
        for(ValueUnitPair vup : phi_expr.getArgs()) {
            for(ValueBox vb : defs) {
                if (Objects.equals(vup.getValue().toString(), vb.getValue().toString())) {
                    step2 = true;
                    break;
                }
            }
        }
        return step1 && step2;
    }

}
