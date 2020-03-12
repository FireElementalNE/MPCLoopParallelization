import org.tinylog.Logger;
import soot.Value;
import soot.ValueBox;
import soot.jimple.AbstractStmtSwitch;
import soot.jimple.AssignStmt;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class VariableVisitor extends AbstractStmtSwitch {

    List<Variable> vars;
    VariableVisitor(List<Variable> vars) {
        this.vars =  vars.stream().map(Variable::new).collect(Collectors.toList());
    }

    List<Variable> get_vars() {
        return vars.stream().map(Variable::new).collect(Collectors.toList());
    }

    private void update_vars(Value v2, Value v1, AssignStmt s) {
        // TODO: this still needs some thinking about, not perfectly correct.
        for (Variable var : vars) {
            if (Objects.equals(v1.toString(), var.get_current_version())) {
                Logger.debug("Found var " + v2.toString() + " updating.");
                var.add_alias(v2, s);
                return;
            }
        }
        Logger.debug("Did not find var " + v1.toString() + " creating new entry.");
        vars.add(new Variable(v1, s));
    }


    @Override
    public void caseAssignStmt(AssignStmt stmt) {
        ValueBox left = stmt.getLeftOpBox();
        ValueBox right = stmt.getRightOpBox();
        if(Objects.equals(left.getValue().getType().toString(), Constants.INT_TYPE) &&
            !stmt.containsArrayRef()) {
            Logger.debug("Found stmt writing to an int: " + stmt.toString());
            update_vars(left.getValue(), right.getValue(), stmt);
        }
    }

}