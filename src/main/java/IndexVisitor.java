import org.tinylog.Logger;
import soot.ValueBox;
import soot.jimple.*;

public class IndexVisitor extends AbstractStmtSwitch {

    private PhiVariableContainer pvc;

    IndexVisitor(PhiVariableContainer pvc) {
        this.pvc = new PhiVariableContainer(pvc);
    }

    public PhiVariableContainer get_pvc() {
        return new PhiVariableContainer(pvc);
    }

    private void check_index(Stmt stmt) {
        ArrayRef ar = stmt.getArrayRef();
        ValueBox index_box = ar.getIndexBox();
        Logger.info("Printing def-chain: '" + index_box.getValue().toString() + "' in stmt '" + stmt.toString() + "'");
        pvc.print_var_dep_chain(index_box.getValue().toString());
    }


    @Override
    public void caseBreakpointStmt(BreakpointStmt stmt) {
        if(stmt.containsArrayRef()) {
            Logger.debug("Checking " + stmt.toString());
            check_index(stmt);
        }
    }

    @Override
    public void caseInvokeStmt(InvokeStmt stmt) {
        if(stmt.containsArrayRef()) {
            Logger.debug("Checking " + stmt.toString());
            check_index(stmt);
        }
    }

    @Override
    public void caseAssignStmt(AssignStmt stmt) {
        if(stmt.containsArrayRef()) {
            Logger.debug("Checking " + stmt.toString());
            check_index(stmt);
        }
    }

    @Override
    public void caseIdentityStmt(IdentityStmt stmt) {
        if(stmt.containsArrayRef()) {
            Logger.debug("Checking " + stmt.toString());
            check_index(stmt);
        }
    }

    @Override
    public void caseEnterMonitorStmt(EnterMonitorStmt stmt) {
        if(stmt.containsArrayRef()) {
            Logger.debug("Checking " + stmt.toString());
            check_index(stmt);
        }
    }

    @Override
    public void caseExitMonitorStmt(ExitMonitorStmt stmt) {
        if(stmt.containsArrayRef()) {
            Logger.debug("Checking " + stmt.toString());
            check_index(stmt);
        }
    }

    @Override
    public void caseGotoStmt(GotoStmt stmt) {
        if(stmt.containsArrayRef()) {
            Logger.debug("Checking " + stmt.toString());
            check_index(stmt);
        }
    }

    @Override
    public void caseIfStmt(IfStmt stmt) {

        if(stmt.containsArrayRef()) {
            Logger.debug("Checking " + stmt.toString());
            check_index(stmt);
        }
    }

    @Override
    public void caseLookupSwitchStmt(LookupSwitchStmt stmt) {
        if(stmt.containsArrayRef()) {
            Logger.debug("Checking " + stmt.toString());
            check_index(stmt);
        }
    }

    @Override
    public void caseNopStmt(NopStmt stmt) {
        if(stmt.containsArrayRef()) {
            Logger.debug("Checking " + stmt.toString());
            check_index(stmt);
        }
    }

    @Override
    public void caseRetStmt(RetStmt stmt) {
        if(stmt.containsArrayRef()) {
            Logger.debug("Checking " + stmt.toString());
            check_index(stmt);
        }
    }

    @Override
    public void caseReturnStmt(ReturnStmt stmt) {
        if(stmt.containsArrayRef()) {
            Logger.debug("Checking " + stmt.toString());
            check_index(stmt);
        }
    }

    @Override
    public void caseReturnVoidStmt(ReturnVoidStmt stmt) {

    }

    @Override
    public void caseTableSwitchStmt(TableSwitchStmt stmt) {
        if(stmt.containsArrayRef()) {
            Logger.debug("Checking " + stmt.toString());
            check_index(stmt);
        }
    }

    @Override
    public void caseThrowStmt(ThrowStmt stmt) {
        if(stmt.containsArrayRef()) {
            Logger.debug("Checking " + stmt.toString());
            check_index(stmt);
        }
    }

    @Override
    public void defaultCase(Object obj) {

    }

}
