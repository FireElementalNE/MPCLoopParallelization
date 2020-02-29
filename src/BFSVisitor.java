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
    private ArrayDefUseGraph graph;
    BFSVisitor(Map<Block, DownwardExposedArrayRef> c_arr_ver, Block b, ArrayDefUseGraph graph) {
        this.c_arr_ver = c_arr_ver;
        this.b = b;
        assert c_arr_ver.containsKey(b); // this should always be here!
        this.daf = new DownwardExposedArrayRef(c_arr_ver.get(b));
        this.graph = new ArrayDefUseGraph(graph);
    }

    Map<Block, DownwardExposedArrayRef> get_c_arr_ver() {
        return c_arr_ver;
    }

    ArrayDefUseGraph get_graph() {
        return graph;
    }

    private void check_array_read(Stmt stmt) {
        String basename = stmt.getArrayRef().getBaseBox().getValue().toString();
        Logger.debug("Change needed in stmt: " + stmt.toString());
        Logger.debug(" " + basename + " should be changed to " + daf.get_name(basename));
        Logger.debug(" " + "This is a use for " + daf.get_name(basename));
        ArrayVersion av = new ArrayVersion(daf.get(basename));
        Node new_node = new Node(stmt, basename, av, DefOrUse.USE);
        graph.add_node(new_node, false);
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
                Logger.debug("This is a new def for " + daf.get_name(basename));
                ArrayVersion av = new ArrayVersion(daf.get(basename));
                graph.add_node(new Node(stmt, basename, av, DefOrUse.DEF), true);
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
