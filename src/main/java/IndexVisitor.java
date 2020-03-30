import org.apache.commons.lang3.tuple.ImmutablePair;
import org.tinylog.Logger;
import soot.Value;
import soot.ValueBox;
import soot.jimple.*;
import soot.jimple.internal.*;

import java.util.List;

public class IndexVisitor extends AbstractStmtSwitch {

    private PhiVariableContainer pvc;

    IndexVisitor(PhiVariableContainer pvc) {
        this.pvc = new PhiVariableContainer(pvc);
    }

    public PhiVariableContainer get_pvc() {
        return new PhiVariableContainer(pvc);
    }

    private void print_dep_box_info(ValueBox vb) {
        Value v = vb.getValue();
        if(v instanceof AbstractFloatBinopExpr) {
            if(v instanceof JAddExpr) {
                Logger.info("JAddExpr");
            }
            else if(v instanceof JDivExpr) {
                Logger.info("JDivExpr");
            }
            else if(v instanceof JMulExpr) {
                Logger.info("JMulExpr");
            }
            else if(v instanceof JRemExpr) {
                Logger.info("JRemExpr");
            }
            else if(v instanceof JSubExpr) {
                Logger.info("JSubExpr");
                JSubExpr vsub = (JSubExpr)v;
                Logger.info("Subtracting " + vsub.getOp2().toString() + " from " + vsub.getOp1().toString());
            } else {
                Logger.error("Error: we should not get here. All classes should be caught. (inner)");
            }
            Logger.info("AbstractFloatBinopExpr");
        }
        else if(v instanceof AbstractIntBinopExpr) {
            Logger.info("AbstractFloatBinopExpr");
        }
        else if(v instanceof AbstractIntLongBinopExpr) {
            Logger.info("AbstractFloatBinopExpr");
        }
        else if(v instanceof AbstractJimpleBinopExpr){
            Logger.info("AbstractJimpleBinopExpr");
        } else {
            Logger.error("Error: we should not get here. All classes should be caught. (outer)");
        }

    }

    private void check_index(Stmt stmt) {
        // TODO:
        //   Just check the stmt on the _right_ if it is an operation if any kind
        //   then it is some sort of intra-loop dependency
        ArrayRef ar = stmt.getArrayRef();
        ValueBox index_box = ar.getIndexBox();
        String index_name = index_box.getValue().toString();
        Logger.info("Printing def-chain: '" + index_name + "' in stmt '" + stmt.toString() + "'");
        pvc.print_var_dep_chain(index_box.getValue().toString());
        ImmutablePair<Variable, List<AssignStmt>> dep_chain = pvc.get_var_dep_chain(index_name);
        if(Utils.not_null(dep_chain)) {
            Logger.debug("Dep chain for " + index_name + ":");
            for(AssignStmt a : dep_chain.getRight()) {
                Logger.debug("\t" + a.toString());
                print_dep_box_info(a.getRightOpBox());
            }
        } else {
            Logger.debug("dep chain for " + index_box.getValue().toString() + " is null.");
        }
    }


    @Override
    public void caseBreakpointStmt(BreakpointStmt stmt) {
        if(stmt.containsArrayRef()) {
            Logger.debug("caseBreakpointStmt Checking " + stmt.toString());
            check_index(stmt);
        }
    }

    @Override
    public void caseInvokeStmt(InvokeStmt stmt) {
        if(stmt.containsArrayRef()) {
            Logger.debug("caseInvokeStmt Checking " + stmt.toString());
            check_index(stmt);
        }
    }

    @Override
    public void caseAssignStmt(AssignStmt stmt) {
        if(stmt.containsArrayRef()) {
            Logger.debug("caseAssignStmt Checking " + stmt.toString());
            check_index(stmt);
        }
    }

    @Override
    public void caseIdentityStmt(IdentityStmt stmt) {
        if(stmt.containsArrayRef()) {
            Logger.debug("caseIdentityStmt Checking " + stmt.toString());
            check_index(stmt);
        }
    }

    @Override
    public void caseEnterMonitorStmt(EnterMonitorStmt stmt) {
        if(stmt.containsArrayRef()) {
            Logger.debug("caseEnterMonitorStmt Checking " + stmt.toString());
            check_index(stmt);
        }
    }

    @Override
    public void caseExitMonitorStmt(ExitMonitorStmt stmt) {
        if(stmt.containsArrayRef()) {
            Logger.debug("caseExitMonitorStmt caseExitMonitorStmt Checking " + stmt.toString());
            check_index(stmt);
        }
    }

    @Override
    public void caseGotoStmt(GotoStmt stmt) {
        if(stmt.containsArrayRef()) {
            Logger.debug("caseGotoStmt Checking " + stmt.toString());
            check_index(stmt);
        }
    }

    @Override
    public void caseIfStmt(IfStmt stmt) {

        if(stmt.containsArrayRef()) {
            Logger.debug("caseIfStmt Checking " + stmt.toString());
            check_index(stmt);
        }
    }

    @Override
    public void caseLookupSwitchStmt(LookupSwitchStmt stmt) {
        if(stmt.containsArrayRef()) {
            Logger.debug("caseLookupSwitchStmt Checking " + stmt.toString());
            check_index(stmt);
        }
    }

    @Override
    public void caseNopStmt(NopStmt stmt) {
        if(stmt.containsArrayRef()) {
            Logger.debug("caseNopStmt Checking " + stmt.toString());
            check_index(stmt);
        }
    }

    @Override
    public void caseRetStmt(RetStmt stmt) {
        if(stmt.containsArrayRef()) {
            Logger.debug("caseRetStmt Checking " + stmt.toString());
            check_index(stmt);
        }
    }

    @Override
    public void caseReturnStmt(ReturnStmt stmt) {
        if(stmt.containsArrayRef()) {
            Logger.debug("caseReturnStmt Checking " + stmt.toString());
            check_index(stmt);
        }
    }

    @Override
    public void caseReturnVoidStmt(ReturnVoidStmt stmt) {

    }

    @Override
    public void caseTableSwitchStmt(TableSwitchStmt stmt) {
        if(stmt.containsArrayRef()) {
            Logger.debug("caseTableSwitchStmt Checking " + stmt.toString());
            check_index(stmt);
        }
    }

    @Override
    public void caseThrowStmt(ThrowStmt stmt) {
        if(stmt.containsArrayRef()) {
            Logger.debug("caseThrowStmt Checking " + stmt.toString());
            check_index(stmt);
        }
    }

    @Override
    public void defaultCase(Object obj) {

    }

}
