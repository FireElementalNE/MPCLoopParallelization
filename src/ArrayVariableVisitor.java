import org.pmw.tinylog.Logger;
import soot.jimple.*;
import soot.jimple.internal.JNewArrayExpr;

import java.util.HashSet;
import java.util.Set;

public class ArrayVariableVisitor extends AbstractStmtSwitch {
    private Set<String> vars;
    private Set<String> array_aliasing;

    ArrayVariableVisitor(Set<String> array_aliasing) {
        vars = new HashSet<>();
        this.array_aliasing = array_aliasing;
    }

    Set<String> get_vars() {
        return vars;
    }

    private String get_basename(ArrayRef aref) {
        return aref.getBaseBox().getValue().toString();
    }

    private void check_array_ref(Stmt stmt) {
        if(stmt.containsArrayRef()) {
            String basename = get_basename(stmt.getArrayRef());
            boolean rc = vars.add(basename);
            if(!rc) {
                Logger.info("Element " + basename + " is already in set.");
            }
        }
    }

    @Override
    public void caseBreakpointStmt(BreakpointStmt stmt) {
        check_array_ref(stmt);
    }

    @Override
    public void caseInvokeStmt(InvokeStmt stmt) {
        check_array_ref(stmt);
    }

    @Override
    public void caseAssignStmt(AssignStmt stmt) {
        String left_op = stmt.getLeftOp().toString();
        String right_op = stmt.getRightOp().toString();
        if(stmt.containsArrayRef()) {
            check_array_ref(stmt);
        } else if(stmt.getRightOp() instanceof JNewArrayExpr) {
            Logger.info("Found a new array: " + left_op);
            vars.add(left_op);
        } else if(vars.contains(right_op)){
            Logger.debug("An array got renamed...");
        }
    }

    @Override
    public void caseIdentityStmt(IdentityStmt stmt) {
        check_array_ref(stmt);
    }

    @Override
    public void caseEnterMonitorStmt(EnterMonitorStmt stmt) {
        check_array_ref(stmt);
    }

    @Override
    public void caseExitMonitorStmt(ExitMonitorStmt stmt) {
        check_array_ref(stmt);
    }

    @Override
    public void caseGotoStmt(GotoStmt stmt) {
        check_array_ref(stmt);
    }

    @Override
    public void caseIfStmt(IfStmt stmt) {
        check_array_ref(stmt);
    }

    @Override
    public void caseLookupSwitchStmt(LookupSwitchStmt stmt) {
        check_array_ref(stmt);
    }

    @Override
    public void caseNopStmt(NopStmt stmt) {
        check_array_ref(stmt);
    }

    @Override
    public void caseRetStmt(RetStmt stmt) {
        check_array_ref(stmt);
    }

    @Override
    public void caseReturnStmt(ReturnStmt stmt) {
        check_array_ref(stmt);
    }

    @Override
    public void caseReturnVoidStmt(ReturnVoidStmt stmt) {
        check_array_ref(stmt);
    }

    @Override
    public void caseTableSwitchStmt(TableSwitchStmt stmt) {
        check_array_ref(stmt);
    }

    @Override
    public void caseThrowStmt(ThrowStmt stmt) {
        check_array_ref(stmt);
    }
}
