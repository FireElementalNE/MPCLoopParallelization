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

    private void update_vars(Value v, AssignStmt s) {
        for (Variable var : vars) {
            if (Objects.equals(v.toString(), var.get_current_version().toString())) {
                Logger.debug("Found var " + v.toString() + " updating.");
                var.add_alias(v, s);
                return;
            }
        }
        Logger.debug("Did not find var " + v.toString() + " creating new entry.");
        vars.add(new Variable(v, s));
    }


    @Override
    public void caseAssignStmt(AssignStmt stmt) {
        ValueBox left = stmt.getLeftOpBox();
        ValueBox right = stmt.getRightOpBox();
        if(Objects.equals(left.getValue().getType().toString(), Constants.INT_TYPE)) {
            Logger.debug("Found stmt writing to an int: " + stmt.toString());
            update_vars(left.getValue(), stmt);
        }
    }

}