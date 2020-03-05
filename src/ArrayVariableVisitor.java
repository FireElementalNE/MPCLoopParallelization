import org.tinylog.Logger;
import soot.ValueBox;
import soot.jimple.*;
import soot.jimple.internal.JNewArrayExpr;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ArrayVariableVisitor extends AbstractStmtSwitch {
    private Map<String, ArrayVersion> vars;
    private ArrayDefUseGraph graph;
    ArrayVariableVisitor(Map<String, ArrayVersion> vars, ArrayDefUseGraph graph) {
        this.vars = new HashMap<>();
        this.vars =  vars.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        this.graph = new ArrayDefUseGraph(graph);
    }

    Map<String, ArrayVersion> get_vars() {
        return vars.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    ArrayDefUseGraph get_graph() {
        return graph;
    }

    private String get_basename(ArrayRef aref) {
        return aref.getBaseBox().getValue().toString();
    }

    private void check_array_ref(Stmt stmt) {
        if(stmt.containsArrayRef()) {
            String basename = get_basename(stmt.getArrayRef());
            ValueBox index_box = stmt.getArrayRef().getIndexBox();
            if(!vars.containsKey(basename)) {
//                Logger.debug("Key " + basename + " already exists.");
//            } else {
                // CHECKTHIS: not sure this is right
//                Logger.error("How can we ref something that does not exist??");
                // If we have not referenced it how can it be a phi node??? I THINK this is right...
                vars.put(basename, new ArrayVersionSingle(1, new Index(index_box)));
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
        }
        if(stmt.getRightOp() instanceof JNewArrayExpr) {
            // NOTE: ALWAYS a new array!
            Logger.info("Found a new array: " + left_op);
            ArrayVersion av = new ArrayVersionSingle(1, null);
            graph.add_node(new Node(stmt.toString(), left_op, av, DefOrUse.DEF), true);
            vars.put(left_op, av);
        }
        if(vars.containsKey(right_op)) {
            // NOTE: PURE array renaming!
            assert !stmt.containsArrayRef();
            ArrayVersion av = vars.get(right_op);
            ArrayVersion new_av = Utils.rename_av(av);
            vars.put(left_op, new_av);
            graph.array_def_rename(right_op, vars.get(right_op), left_op, new_av, stmt);
            vars.remove(right_op);
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
