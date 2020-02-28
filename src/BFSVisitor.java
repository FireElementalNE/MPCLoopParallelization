import org.pmw.tinylog.Logger;
import soot.ValueBox;
import soot.jimple.*;
import soot.toolkits.graph.Block;

import java.util.Map;
import java.util.Objects;

class BFSVisitor extends AbstractStmtSwitch {
    private Map<Block, DownwardExposedArrayRef> c_arr_ver;
    private Block b;
    private DownwardExposedArrayRef daf;
    BFSVisitor(Map<Block, DownwardExposedArrayRef> c_arr_ver, Block b) {
        this.c_arr_ver = c_arr_ver;
        this.b = b;
        assert c_arr_ver.containsKey(b); // this should always be here!
        this.daf = new DownwardExposedArrayRef(c_arr_ver.get(b));
    }

    Map<Block, DownwardExposedArrayRef> get_c_arr_ver() {
        return c_arr_ver;
    }

    private void check_array_read(Stmt stmt) {
        String basename = stmt.getArrayRef().getBaseBox().getValue().toString();
        daf.new_ver(basename);
        Logger.debug("Change needed in stmt: " + stmt.toString());
        Logger.debug(" " + basename + " should be changed to " + daf.get_name(basename));
        c_arr_ver.put(b, daf);
    }

    @Override
    public void caseBreakpointStmt(BreakpointStmt stmt) {

    }

    @Override
    public void caseInvokeStmt(InvokeStmt stmt) {
        if(stmt.containsArrayRef()) {
            Logger.debug("Checking " + stmt.toString());
            check_array_read(stmt);
        }
    }

    @Override
    public void caseAssignStmt(AssignStmt stmt) {
        if(stmt.containsArrayRef()) {
            ValueBox left = stmt.getLeftOpBox();
            if(Objects.equals(stmt.getArrayRefBox(), left)) {
                String basename = stmt.getArrayRef().getBaseBox().getValue().toString();
                Logger.debug("Array write found (change needed): " + stmt.toString());
                daf.new_ver(basename);
                Logger.debug(" " + basename + " needs to be changed to " + daf.get_name(basename));
                c_arr_ver.put(b, daf);
            } else {
                check_array_read(stmt);
            }
        }
    }

    @Override
    public void caseIdentityStmt(IdentityStmt stmt) {
        if(stmt.containsArrayRef()) {
            Logger.debug("Checking " + stmt.toString());
            check_array_read(stmt);
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
            check_array_read(stmt);
        }
    }

    @Override
    public void caseIfStmt(IfStmt stmt) {

        if(stmt.containsArrayRef()) {
            Logger.debug("Checking " + stmt.toString());
            check_array_read(stmt);
        }
    }

    @Override
    public void caseLookupSwitchStmt(LookupSwitchStmt stmt) {
        if(stmt.containsArrayRef()) {
            Logger.debug("Checking " + stmt.toString());
            check_array_read(stmt);
        }
    }

    @Override
    public void caseNopStmt(NopStmt stmt) {

    }

    @Override
    public void caseRetStmt(RetStmt stmt) {

    }

    @Override
    public void caseReturnStmt(ReturnStmt stmt) {
        if(stmt.containsArrayRef()) {
            Logger.debug("Checking " + stmt.toString());
            check_array_read(stmt);
        }
    }

    @Override
    public void caseReturnVoidStmt(ReturnVoidStmt stmt) {

    }

    @Override
    public void caseTableSwitchStmt(TableSwitchStmt stmt) {
        if(stmt.containsArrayRef()) {
            Logger.debug("Checking " + stmt.toString());
            check_array_read(stmt);
        }
    }

    @Override
    public void caseThrowStmt(ThrowStmt stmt) {
        if(stmt.containsArrayRef()) {
            Logger.debug("Checking " + stmt.toString());
            check_array_read(stmt);
        }
    }

    @Override
    public void defaultCase(Object obj) {

    }
}
