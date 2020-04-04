import org.apache.commons.lang3.tuple.ImmutablePair;
import org.tinylog.Logger;
import soot.ValueBox;
import soot.jimple.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class IndexVisitor extends AbstractStmtSwitch {

    private PhiVariableContainer pvc;
    private Set<String> second_iter_def_vars;
    private Set<String> top_phi_var_names;
    private ArrayDefUseGraph graph;

    IndexVisitor(PhiVariableContainer pvc, Set<String> second_iter_def_vars,
                 Set<String> top_phi_var_names, ArrayDefUseGraph graph) {
        this.pvc = new PhiVariableContainer(pvc);
        this.second_iter_def_vars = new HashSet<>(second_iter_def_vars);
        this.top_phi_var_names = new HashSet<>(top_phi_var_names);
        this.graph = new ArrayDefUseGraph(graph);
    }

    Set<String> get_second_iter_def_vars() {
        // TODO: this may be incorrect. To be an intra-loop dependency a variable must be based off
        //         of a phi variable, and have some sort of augmentation done to it (e.g. pv1 - 1).
        return new HashSet<>(second_iter_def_vars);
    }

    Set<String> get_top_phi_var_names() {
        return new HashSet<>(top_phi_var_names);
    }

    public ArrayDefUseGraph get_graph() {
        return new ArrayDefUseGraph(graph);
    }

    PhiVariableContainer get_pvc() {
        return new PhiVariableContainer(pvc);
    }


    private void print_dep_box_info(ValueBox vb) {

    }

    private void check_index(Stmt stmt) {
        // TODO:
        //   Just check the stmt on the _right_ if it is an operation if any kind
        //   then it is some sort of intra-loop dependency
        ArrayRef ar = stmt.getArrayRef();
        ValueBox index_box = ar.getIndexBox();
        String index_name = index_box.getValue().toString();
        if(!second_iter_def_vars.contains(index_name)
                && !top_phi_var_names.contains(index_name)) {
            Logger.debug("Printing def-chain: '" + index_name + "' in stmt '" + stmt.toString() + "'");
            pvc.print_var_dep_chain(index_box.getValue().toString());
            ImmutablePair<Variable, List<AssignStmt>> dep_chain = pvc.get_var_dep_chain(index_name);
            // Node(String stmt, String basename, ArrayVersion av, Index index, DefOrUse type, int line_num) {
            StringBuilder stmt_str = new StringBuilder();
            if (Utils.not_null(dep_chain)) {
                if(!dep_chain.getRight().isEmpty()) {
                    Logger.debug("Dep chain for " + index_name + ":");
                    for (int i = 0; i < dep_chain.getRight().size(); i++) {
                        AssignStmt a = dep_chain.getRight().get(i);
                        stmt_str.append(a.toString());
                        if (i != dep_chain.getRight().size() - 1) {
                            stmt_str.append(" >>> ");
                        }
                        Logger.debug("\t" + a.toString());
                    }
                } else {
                    Logger.debug("Dep chain for " + index_name + " is empty (is it a phi var?).");
                }
                stmt_str.append(" >>> ").append(stmt.toString());
                graph.add_node(new Node(stmt_str.toString(), ar.getBaseBox().getValue().toString(),
                        new ArrayVersionSingle(-1, -1), new Index(index_box), DefOrUse.USE,
                        stmt.getJavaSourceStartLineNumber()), false, true);
            } else {
                Logger.error("dep chain for " + index_box.getValue().toString() + " is null.");
            }
            second_iter_def_vars.add(index_name);
        } else {
            Logger.debug("Variable '" + index_name + "' is has been defined already.");
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
