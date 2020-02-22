import soot.ValueBox;
import soot.jimple.*;

import java.util.Map;
import java.util.Objects;

import org.pmw.tinylog.Logger;

class ArraySSAVisitor extends AbstractStmtSwitch {
    private Map<String, ArraySSAPhi> phis;
    ArraySSAVisitor(Map <String, ArraySSAPhi> phis) {
        this.phis = phis;
    }

    Map <String, ArraySSAPhi> get_phis() {
        return phis;
    }

    public void array_ssa_assignment(AssignStmt stmt) {
        ValueBox right = stmt.getRightOpBox();
        Logger.info("We are writing to an array, the following transform should be made!");
        ArrayRef array_ref = stmt.getArrayRef();
        ValueBox base = array_ref.getBaseBox();
        ValueBox index = array_ref.getIndexBox();
        String base_name = base.getValue().toString();
        if(!phis.containsKey(base_name)) {
            phis.put(base_name, new ArraySSAPhi((base)));
        } else {
            phis.get(base_name).add_copy();
        }
        Logger.info(" Original: " + stmt.toString());
        Logger.info(" " + String.format(Constants.SSA_ARRAY_CPY,
                phis.get(base_name).get_prev_copy(),
                phis.get(base_name).get_latest_copy()));
        Logger.info(" " + String.format(Constants.SSA_ASSIGNMENT,
                phis.get(base_name).get_latest_copy(),
                index.getValue().toString(), right.getValue().toString()));
    }

    public void check_array_ref(ArrayRef array_ref, Stmt stmt) {
        ValueBox base = array_ref.getBaseBox();
        ValueBox index = array_ref.getIndexBox();
        String base_name = base.getValue().toString();
        String index_name = index.getValue().toString();
        if(phis.containsKey(base_name)) {
            Logger.info("Statement contains stale array ref.");
            Logger.debug(" Original: " + stmt.toString());
            Logger.info(" " + array_ref.toString()
                    + " should be changed to " + String.format(Constants.ARRAY_REF,
                    phis.get(base_name).get_latest_copy(), index_name));
        }
    }

    @Override
    public void caseBreakpointStmt(BreakpointStmt stmt) {

    }

    @Override
    public void caseInvokeStmt(InvokeStmt stmt) {
        if(stmt.containsArrayRef()) {
            Logger.debug("Checking " + stmt.toString());
            check_array_ref(stmt.getArrayRef(), stmt);
        }
    }


    @Override
    public void caseAssignStmt(AssignStmt stmt) {
        if(stmt.containsArrayRef()) {
            ValueBox left = stmt.getLeftOpBox();
            if(Objects.equals(stmt.getArrayRefBox(), left)) {
                array_ssa_assignment(stmt);
            } else {
                check_array_ref(stmt.getArrayRef(), stmt);
            }
        }
    }

    @Override
    public void caseIdentityStmt(IdentityStmt stmt) {
        if(stmt.containsArrayRef()) {
            Logger.debug("Checking " + stmt.toString());
            check_array_ref(stmt.getArrayRef(), stmt);
        }
    }

    @Override
    public void caseEnterMonitorStmt(EnterMonitorStmt stmt) {

    }

    @Override
    public void caseExitMonitorStmt(ExitMonitorStmt stmt) {

    }

    @Override
    public void caseGotoStmt(GotoStmt stmt) {
        if(stmt.containsArrayRef()) {
            Logger.debug("Checking " + stmt.toString());
            check_array_ref(stmt.getArrayRef(), stmt);
        }
    }

    @Override
    public void caseIfStmt(IfStmt stmt) {
        if(stmt.containsArrayRef()) {
            Logger.debug("Checking " + stmt.toString());
            check_array_ref(stmt.getArrayRef(), stmt);
        }
    }

    @Override
    public void caseLookupSwitchStmt(LookupSwitchStmt stmt) {
        if(stmt.containsArrayRef()) {
            Logger.debug("Checking " + stmt.toString());
            check_array_ref(stmt.getArrayRef(), stmt);
        }
    }

    @Override
    public void caseNopStmt(NopStmt stmt) {

    }

    @Override
    public void caseRetStmt(RetStmt stmt) {

    }

    public void caseReturnStmt(ReturnStmt stmt) {
        if(stmt.containsArrayRef()) {
            Logger.debug("Checking " + stmt.toString());
            check_array_ref(stmt.getArrayRef(), stmt);
        }
    }

    public void caseReturnVoidStmt(ReturnVoidStmt stmt) {

    }

    public void caseTableSwitchStmt(TableSwitchStmt stmt) {
        if(stmt.containsArrayRef()) {
            Logger.debug("Checking " + stmt.toString());
            check_array_ref(stmt.getArrayRef(), stmt);
        }
    }

    public void caseThrowStmt(ThrowStmt stmt) {
        if(stmt.containsArrayRef()) {
            Logger.debug("Checking " + stmt.toString());
            check_array_ref(stmt.getArrayRef(), stmt);
        }
    }

    public void defaultCase(Object obj) {

    }

}