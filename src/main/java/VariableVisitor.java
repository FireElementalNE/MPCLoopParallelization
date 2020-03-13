import org.tinylog.Logger;
import soot.ValueBox;
import soot.jimple.AbstractStmtSwitch;
import soot.jimple.AssignStmt;
import soot.shimple.PhiExpr;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class VariableVisitor extends AbstractStmtSwitch {

    private List<Variable> vars;
    private Set<PhiVariable> phi_vars;
    VariableVisitor(List<Variable> vars, Set<PhiVariable> phi_vars) {
        this.vars =  vars.stream().map(Variable::new).collect(Collectors.toList());
        this.phi_vars = phi_vars.stream().map(PhiVariable::new).collect(Collectors.toSet());
    }

    List<Variable> get_vars() {
        return vars.stream().map(Variable::new).collect(Collectors.toList());
    }
    Set<PhiVariable> get_phi_vars() {
        return phi_vars.stream().map(PhiVariable::new).collect(Collectors.toSet());
    }

    @Override
    public void caseAssignStmt(AssignStmt stmt) {
        ValueBox left = stmt.getLeftOpBox();
        ValueBox right = stmt.getRightOpBox();
        if(right.getValue() instanceof PhiExpr) {
            Logger.info("We found a phi node: " + stmt.toString());
            phi_vars.add(new PhiVariable(stmt));
        } else {
            Logger.debug("Not a phi node, checking usages: " + stmt.toString());
            List<ValueBox> uses =stmt.getUseBoxes();
            for(ValueBox vb : uses) {
                for(PhiVariable pv : phi_vars) {
                    if(pv.linked_to(stmt)) {
                        Logger.info("We found a match for var " + vb.getValue().toString() + " in a phi node.");
                    }
                }
            }
        }
        /* TODO:
        From here we need to do something like the following.

        When we get an assignment stmt:
            1) check if ANYTHING on the right side is linked to a variable that is in a phi node; if so
                then will effect our _d_. (these are the equations that we will pass to z3 eventually...)
            2) somehow link ALL of those statements to the phi node...
            3) at the end when all of the statements are linked to the phi node, pass to z3 and get a concrete
                answer (if it exists....)
            4) ...?
         */
    }
}